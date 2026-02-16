package com.example.englishword.ui.study

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.englishword.ads.AdManager
import com.example.englishword.data.local.entity.StudySession
import com.example.englishword.data.local.entity.Word
import com.example.englishword.data.repository.SettingsRepository
import com.example.englishword.data.repository.StudyRepository
import com.example.englishword.data.repository.UnlockRepository
import com.example.englishword.data.repository.WordRepository
import com.example.englishword.ui.quiz.QuizGenerator
import com.example.englishword.ui.quiz.QuizOptions
import com.example.englishword.util.TtsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class StudyWordMode {
    AUTO,
    REVIEW_ONLY,
    NEW_ONLY
}

data class StudyModeCounts(
    val reviewCount: Int = 0,
    val newCount: Int = 0
) {
    val autoCount: Int get() = reviewCount + newCount
}

/**
 * ViewModel for the Study screen.
 * Manages the study session, card flipping, word evaluation, and quiz mode.
 */
@HiltViewModel
class StudyViewModel @Inject constructor(
    private val wordRepository: WordRepository,
    private val studyRepository: StudyRepository,
    private val settingsRepository: SettingsRepository, // Added: for premium check
    private val unlockRepository: UnlockRepository, // Added: for review limits
    val adManager: AdManager, // Added: for interstitial ads
    val ttsManager: TtsManager // Added: for TTS pronunciation
) : ViewModel() {

    private val _uiState = MutableStateFlow<StudyUiState>(StudyUiState.Loading)
    val uiState: StateFlow<StudyUiState> = _uiState.asStateFlow()

    // Track evaluation counts for the session
    private var knownCount = 0
    private var againCount = 0
    private var laterCount = 0
    private var currentLevelId: Long = 0

    // Limit laterQueue recycling to prevent infinite loops
    private var laterQueueCycleCount = 0
    private val maxLaterQueueCycles = 3

    // Track when card was shown to user (for response time measurement)
    private var cardShownTimestamp: Long = 0L

    // Quiz mode: cached words in current level for generating distractors
    private var allWordsInLevel: List<Word> = emptyList()
    private var isQuizMode: Boolean = false
    private var studyWordMode: StudyWordMode = StudyWordMode.AUTO

    // Job tracking for quiz answer DB writes (prevents race condition with nextQuizWord)
    private var lastRecordJob: Job? = null

    override fun onCleared() {
        ttsManager.stop()
        super.onCleared()
    }

    suspend fun getModeCounts(levelId: Long): StudyModeCounts {
        val review = wordRepository.getReviewWordCount(levelId)
        val new = wordRepository.getNewWordCount(levelId)
        return StudyModeCounts(reviewCount = review, newCount = new)
    }

    /**
     * Load words for study from the specified level.
     * First checks for an incomplete session to resume.
     */
    fun loadWords(levelId: Long) {
        currentLevelId = levelId
        viewModelScope.launch {
            _uiState.value = StudyUiState.Loading

            try {
                // Check study mode setting
                isQuizMode = settingsRepository.isQuizModeSync()

                // Check if free user has exceeded daily review limit
                val isPremium = settingsRepository.isPremiumSync()
                if (!isPremium) {
                    val canReview = unlockRepository.canReviewMore(isPremium)
                    if (!canReview) {
                        _uiState.value = StudyUiState.Error("本日の無料復習上限に達しました。プレミアムで無制限に学習できます。")
                        return@launch
                    }

                    val isUnlocked = unlockRepository.isUnitUnlocked(levelId, isPremium = false, isParentLevel = false)
                    if (!isUnlocked) {
                        _uiState.value = StudyUiState.Error("このユニットはロックされています。広告視聴またはプレミアムで解除してください。")
                        return@launch
                    }
                }

                // For quiz mode, pre-load all words in the level for distractor generation
                if (isQuizMode) {
                    allWordsInLevel = wordRepository.getWordsByLevelSync(levelId)
                    // Fall back to flashcard mode if not enough unique words for quiz
                    if (!QuizGenerator.canGenerateQuiz(allWordsInLevel)) {
                        isQuizMode = false
                    }
                }

                // Check for incomplete session to resume (only if AUTO mode, since
                // the old session may contain words from a different mode)
                if (studyWordMode == StudyWordMode.AUTO) {
                    val incompleteSession = studyRepository.getIncompleteSessionForLevel(levelId)
                    if (incompleteSession != null && incompleteSession.isInProgress) {
                        resumeSession(incompleteSession)
                        return@launch
                    }
                }

                // Start fresh session
                startNewSession(levelId)
            } catch (e: Exception) {
                _uiState.value = StudyUiState.Error("単語の読み込みに失敗しました: ${e.message}")
            }
        }
    }

    fun setStudyWordMode(mode: StudyWordMode) {
        studyWordMode = mode
    }

    /**
     * Resume an incomplete session.
     */
    private suspend fun resumeSession(session: StudySession) {
        val wordIds = session.getWordIdList()
        val laterQueueIds = session.getLaterQueueIdList()

        // Load words from database
        val words = wordRepository.getWordsByIds(wordIds)
        val laterQueue = if (laterQueueIds.isNotEmpty()) {
            wordRepository.getWordsByIds(laterQueueIds)
        } else {
            emptyList()
        }

        if (words.isEmpty()) {
            // Session data corrupted, start fresh
            studyRepository.deleteSession(session.id)
            startNewSession(session.levelId)
            return
        }

        // Restore counters
        knownCount = session.knownCount
        againCount = session.againCount
        laterCount = session.laterCount

        // Start response time tracking for resumed session
        cardShownTimestamp = System.currentTimeMillis()

        val currentIndex = session.currentIndex.coerceAtMost(words.size - 1)
        val currentWord = words.getOrNull(currentIndex)

        // Generate quiz options if in quiz mode
        val quizOptions = if (isQuizMode && currentWord != null) {
            generateQuizOptions(currentWord)
        } else null

        _uiState.value = StudyUiState.Studying(
            words = words,
            currentIndex = currentIndex,
            isFlipped = false,
            sessionId = session.id,
            laterQueue = laterQueue,
            isReversed = session.isReversed,
            isQuizMode = isQuizMode,
            quizOptions = quizOptions,
            selectedAnswerIndex = null,
            isQuizAnswered = false
        )
    }

    /**
     * Start a new study session.
     */
    private suspend fun startNewSession(levelId: Long) {
        val words = when (studyWordMode) {
            StudyWordMode.AUTO -> wordRepository.getWordsForReview(levelId, limit = 20)
            StudyWordMode.REVIEW_ONLY -> wordRepository.getDueWordsForReview(levelId, limit = 20)
            StudyWordMode.NEW_ONLY -> wordRepository.getNewWordsForReview(levelId, limit = 20)
        }

        if (words.isEmpty()) {
            val message = when (studyWordMode) {
                StudyWordMode.AUTO -> "学習対象の単語がありません"
                StudyWordMode.REVIEW_ONLY -> "復習対象の単語がありません"
                StudyWordMode.NEW_ONLY -> "新規単語がありません"
            }
            _uiState.value = StudyUiState.Error(message)
            return
        }

        // 設定から出題方向を取得
        val isReversed = settingsRepository.isStudyDirectionReversedSync()

        // Start a new session with progress data
        val wordIds = words.map { it.id }
        val sessionId = studyRepository.startSessionWithProgress(levelId, wordIds, isReversed)

        if (sessionId == -1L) {
            _uiState.value = StudyUiState.Error("学習セッションの開始に失敗しました")
            return
        }

        // Reset counters
        knownCount = 0
        againCount = 0
        laterCount = 0
        laterQueueCycleCount = 0

        // Start response time tracking
        cardShownTimestamp = System.currentTimeMillis()

        // Generate quiz options if in quiz mode
        val firstWord = words.firstOrNull()
        val quizOptions = if (isQuizMode && firstWord != null) {
            generateQuizOptions(firstWord)
        } else null

        _uiState.value = StudyUiState.Studying(
            words = words,
            currentIndex = 0,
            isFlipped = false,
            sessionId = sessionId,
            laterQueue = emptyList(),
            isReversed = isReversed,
            isQuizMode = isQuizMode,
            quizOptions = quizOptions,
            selectedAnswerIndex = null,
            isQuizAnswered = false
        )
    }

    /**
     * Toggle the card flip state.
     */
    fun flipCard() {
        val currentState = _uiState.value
        if (currentState is StudyUiState.Studying) {
            _uiState.value = currentState.copy(isFlipped = !currentState.isFlipped)
            // Reset response time when card is flipped to show answer
            if (!currentState.isFlipped) {
                cardShownTimestamp = System.currentTimeMillis()
            }
        }
    }

    /**
     * Toggle between English→Japanese and Japanese→English.
     */
    fun toggleDirection() {
        val currentState = _uiState.value
        if (currentState is StudyUiState.Studying) {
            _uiState.value = currentState.copy(
                isReversed = !currentState.isReversed,
                isFlipped = false
            )
        }
    }

    /**
     * Speak the current word's English text via TTS.
     */
    fun speakWord() {
        val currentState = _uiState.value
        if (currentState is StudyUiState.Studying) {
            currentState.currentWord?.let { word ->
                ttsManager.speak(word.english)
            }
        }
    }

    /**
     * Evaluate the current word and move to the next.
     */
    fun evaluateWord(result: EvaluationResult) {
        val currentState = _uiState.value
        if (currentState !is StudyUiState.Studying) return

        val currentWord = currentState.currentWord ?: return
        val sessionId = currentState.sessionId
        val responseTimeMs = System.currentTimeMillis() - cardShownTimestamp

        when (result) {
            EvaluationResult.AGAIN -> {
                // まだ: カードを裏返すだけ（同じ単語をもう一度）
                againCount++
                _uiState.value = currentState.copy(isFlipped = false)

                // Record to database and save progress
                viewModelScope.launch {
                    studyRepository.recordResult(sessionId, currentWord.id, 0, responseTimeMs)
                    saveProgress(currentState)
                    // Reset timer for next attempt
                    cardShownTimestamp = System.currentTimeMillis()
                }
            }
            EvaluationResult.LATER -> {
                // あとで: laterQueueに追加して次の単語へ進む
                laterCount++
                val newLaterQueue = currentState.laterQueue + currentWord
                val nextIndex = currentState.currentIndex + 1

                if (nextIndex < currentState.words.size) {
                    val newState = currentState.copy(
                        currentIndex = nextIndex,
                        isFlipped = false,
                        laterQueue = newLaterQueue
                    )
                    _uiState.value = newState

                    viewModelScope.launch {
                        studyRepository.recordResult(sessionId, currentWord.id, 1, responseTimeMs)
                        saveProgress(newState)
                        cardShownTimestamp = System.currentTimeMillis()
                    }
                } else if (newLaterQueue.isNotEmpty()) {
                    // メインリスト終了、laterQueueを新しいリストとして開始
                    laterQueueCycleCount++
                    if (laterQueueCycleCount > maxLaterQueueCycles) {
                        forceCompleteSession(sessionId, currentWord.id, 1, responseTimeMs)
                        return
                    }
                    val newState = currentState.copy(
                        words = newLaterQueue,
                        currentIndex = 0,
                        isFlipped = false,
                        laterQueue = emptyList()
                    )
                    _uiState.value = newState

                    viewModelScope.launch {
                        studyRepository.recordResult(sessionId, currentWord.id, 1, responseTimeMs)
                        saveProgress(newState)
                        cardShownTimestamp = System.currentTimeMillis()
                    }
                } else {
                    // ありえないケース（laterQueueに追加直後なので空にはならない）
                    _uiState.value = currentState.copy(isFlipped = false)

                    viewModelScope.launch {
                        studyRepository.recordResult(sessionId, currentWord.id, 1, responseTimeMs)
                        cardShownTimestamp = System.currentTimeMillis()
                    }
                }
            }
            EvaluationResult.KNOWN -> {
                // 覚えた: 次の単語へ
                knownCount++

                // Increment review count for free users
                viewModelScope.launch {
                    unlockRepository.incrementReviewCount()
                }

                val nextIndex = currentState.currentIndex + 1

                if (nextIndex < currentState.words.size) {
                    val newState = currentState.copy(
                        currentIndex = nextIndex,
                        isFlipped = false
                    )
                    _uiState.value = newState

                    viewModelScope.launch {
                        studyRepository.recordResult(sessionId, currentWord.id, 2, responseTimeMs)
                        saveProgress(newState)
                        cardShownTimestamp = System.currentTimeMillis()
                    }
                } else if (currentState.laterQueue.isNotEmpty()) {
                    // メインリスト完了だがlaterQueueに単語が残っている
                    laterQueueCycleCount++
                    if (laterQueueCycleCount > maxLaterQueueCycles) {
                        forceCompleteSession(sessionId, currentWord.id, 2, responseTimeMs)
                        return
                    }
                    val newState = currentState.copy(
                        words = currentState.laterQueue,
                        currentIndex = 0,
                        isFlipped = false,
                        laterQueue = emptyList()
                    )
                    _uiState.value = newState

                    viewModelScope.launch {
                        studyRepository.recordResult(sessionId, currentWord.id, 2, responseTimeMs)
                        saveProgress(newState)
                        cardShownTimestamp = System.currentTimeMillis()
                    }
                } else {
                    // 全単語完了
                    forceCompleteSession(sessionId, currentWord.id, 2, responseTimeMs)
                    return
                }
            }
        }
    }

    // ==================== Quiz Mode Methods ====================

    /**
     * Generate quiz options for the given word.
     * Returns null if insufficient distractors (caller should fall back to flashcard).
     */
    private fun generateQuizOptions(word: Word, isReversed: Boolean = false): QuizOptions? {
        return QuizGenerator.generateOptions(
            correctWord = word,
            allWordsInLevel = allWordsInLevel,
            isReversed = isReversed
        )
    }

    /**
     * Handle quiz answer selection.
     * Correct answer = KNOWN (result=2), Wrong answer = AGAIN (result=0).
     */
    fun selectQuizAnswer(index: Int) {
        val currentState = _uiState.value
        if (currentState !is StudyUiState.Studying) return
        if (currentState.isQuizAnswered) return // Already answered
        if (currentState.quizOptions == null) return

        val currentWord = currentState.currentWord ?: return
        val isCorrect = index == currentState.quizOptions.correctIndex
        val responseTimeMs = System.currentTimeMillis() - cardShownTimestamp

        // Update UI state to show result
        _uiState.value = currentState.copy(
            selectedAnswerIndex = index,
            isQuizAnswered = true
        )

        // Record the result
        val resultValue = if (isCorrect) 2 else 0 // KNOWN=2, AGAIN=0
        if (isCorrect) {
            knownCount++
            // Increment review count for free users
            viewModelScope.launch {
                unlockRepository.incrementReviewCount()
            }
        } else {
            againCount++
        }

        lastRecordJob = viewModelScope.launch {
            studyRepository.recordResult(
                currentState.sessionId,
                currentWord.id,
                resultValue,
                responseTimeMs
            )
            saveProgress(currentState)
        }
    }

    /**
     * Skip the current word in quiz mode (LATER equivalent).
     */
    fun quizSkipWord() {
        val currentState = _uiState.value
        if (currentState !is StudyUiState.Studying) return
        if (currentState.isQuizAnswered) return

        val currentWord = currentState.currentWord ?: return
        laterCount++

        val newLaterQueue = currentState.laterQueue + currentWord
        val nextIndex = currentState.currentIndex + 1

        val newState = if (nextIndex < currentState.words.size) {
            val nextWord = currentState.words[nextIndex]
            val quizOptions = generateQuizOptions(nextWord, currentState.isReversed)
            currentState.copy(
                currentIndex = nextIndex,
                laterQueue = newLaterQueue,
                quizOptions = quizOptions,
                selectedAnswerIndex = null,
                isQuizAnswered = false
            )
        } else if (newLaterQueue.isNotEmpty()) {
            laterQueueCycleCount++
            if (laterQueueCycleCount > maxLaterQueueCycles) {
                forceCompleteSession(currentState.sessionId, currentWord.id, 1, 0L)
                return
            }
            val nextWord = newLaterQueue.first()
            val quizOptions = generateQuizOptions(nextWord, currentState.isReversed)
            currentState.copy(
                words = newLaterQueue,
                currentIndex = 0,
                laterQueue = emptyList(),
                quizOptions = quizOptions,
                selectedAnswerIndex = null,
                isQuizAnswered = false
            )
        } else {
            return
        }
        _uiState.value = newState

        viewModelScope.launch {
            studyRepository.recordResult(currentState.sessionId, currentWord.id, 1, 0L)
            saveProgress(newState)
            cardShownTimestamp = System.currentTimeMillis()
        }
    }

    /**
     * Move to the next word in quiz mode.
     * Called when user presses "Next" after answering.
     */
    fun nextQuizWord() {
        val currentState = _uiState.value
        if (currentState !is StudyUiState.Studying) return
        if (!currentState.isQuizAnswered) return

        val currentWord = currentState.currentWord ?: return
        val wasCorrect = currentState.selectedAnswerIndex == currentState.quizOptions?.correctIndex

        // Ensure previous DB write completes before proceeding
        viewModelScope.launch {
            lastRecordJob?.join()
            nextQuizWordInternal(currentState, currentWord, wasCorrect)
        }
    }

    private suspend fun nextQuizWordInternal(
        currentState: StudyUiState.Studying,
        currentWord: Word,
        wasCorrect: Boolean
    ) {
        if (wasCorrect) {
            // Correct: advance to next word
            val nextIndex = currentState.currentIndex + 1

            if (nextIndex < currentState.words.size) {
                val nextWord = currentState.words[nextIndex]
                val quizOptions = generateQuizOptions(nextWord, currentState.isReversed)

                val newState = currentState.copy(
                    currentIndex = nextIndex,
                    isFlipped = false,
                    quizOptions = quizOptions,
                    selectedAnswerIndex = null,
                    isQuizAnswered = false
                )
                _uiState.value = newState

                viewModelScope.launch {
                    saveProgress(newState)
                    cardShownTimestamp = System.currentTimeMillis()
                }
            } else if (currentState.laterQueue.isNotEmpty()) {
                // Main list done, start later queue
                laterQueueCycleCount++
                if (laterQueueCycleCount > maxLaterQueueCycles) {
                    forceCompleteSession(currentState.sessionId)
                    return
                }
                val newWords = currentState.laterQueue
                val nextWord = newWords.first()
                val quizOptions = generateQuizOptions(nextWord, currentState.isReversed)

                val newState = currentState.copy(
                    words = newWords,
                    currentIndex = 0,
                    isFlipped = false,
                    laterQueue = emptyList(),
                    quizOptions = quizOptions,
                    selectedAnswerIndex = null,
                    isQuizAnswered = false
                )
                _uiState.value = newState

                viewModelScope.launch {
                    saveProgress(newState)
                    cardShownTimestamp = System.currentTimeMillis()
                }
            } else {
                // All words completed
                forceCompleteSession(currentState.sessionId)
                return
            }
        } else {
            // Wrong: add to later queue and advance
            val newLaterQueue = currentState.laterQueue + currentWord
            val nextIndex = currentState.currentIndex + 1

            if (nextIndex < currentState.words.size) {
                val nextWord = currentState.words[nextIndex]
                val quizOptions = generateQuizOptions(nextWord, currentState.isReversed)

                val newState = currentState.copy(
                    currentIndex = nextIndex,
                    isFlipped = false,
                    laterQueue = newLaterQueue,
                    quizOptions = quizOptions,
                    selectedAnswerIndex = null,
                    isQuizAnswered = false
                )
                _uiState.value = newState

                viewModelScope.launch {
                    saveProgress(newState)
                    cardShownTimestamp = System.currentTimeMillis()
                }
            } else if (newLaterQueue.isNotEmpty()) {
                // Main list done, start later queue
                laterQueueCycleCount++
                if (laterQueueCycleCount > maxLaterQueueCycles) {
                    forceCompleteSession(currentState.sessionId)
                    return
                }
                val nextWord = newLaterQueue.first()
                val quizOptions = generateQuizOptions(nextWord, currentState.isReversed)

                val newState = currentState.copy(
                    words = newLaterQueue,
                    currentIndex = 0,
                    isFlipped = false,
                    laterQueue = emptyList(),
                    quizOptions = quizOptions,
                    selectedAnswerIndex = null,
                    isQuizAnswered = false
                )
                _uiState.value = newState

                viewModelScope.launch {
                    saveProgress(newState)
                    cardShownTimestamp = System.currentTimeMillis()
                }
            } else {
                // Should not happen (we just added to laterQueue)
                _uiState.value = currentState.copy(
                    isFlipped = false,
                    selectedAnswerIndex = null,
                    isQuizAnswered = false
                )
            }
        }
    }

    /**
     * Persist final session result and then emit completed state.
     * This avoids losing completion updates when screen navigation disposes the ViewModel.
     */
    private fun forceCompleteSession(
        sessionId: Long,
        finalWordId: Long? = null,
        finalResult: Int? = null,
        responseTimeMs: Long = 0L
    ) {
        viewModelScope.launch {
            if (finalWordId != null && finalResult != null) {
                studyRepository.recordResult(sessionId, finalWordId, finalResult, responseTimeMs)
            }

            studyRepository.completeSession(sessionId, knownCount + againCount, knownCount)
            val streak = studyRepository.getCurrentStreak()

            _uiState.value = StudyUiState.Completed(
                sessionId = sessionId,
                levelId = currentLevelId,
                totalCount = knownCount + againCount + laterCount,
                knownCount = knownCount,
                againCount = againCount,
                laterCount = laterCount,
                streak = streak
            )
        }
    }

    /**
     * Save current session progress to database.
     */
    private suspend fun saveProgress(state: StudyUiState.Studying) {
        studyRepository.saveSessionProgress(
            sessionId = state.sessionId,
            currentIndex = state.currentIndex,
            knownCount = knownCount,
            againCount = againCount,
            laterCount = laterCount,
            wordIds = state.words.map { it.id },
            laterQueueIds = state.laterQueue.map { it.id },
            isReversed = state.isReversed
        )
    }

    /**
     * Skip the current word (move to end of queue).
     */
    fun skipWord() {
        val currentState = _uiState.value
        if (currentState !is StudyUiState.Studying) return

        val currentWord = currentState.currentWord ?: return

        val newLaterQueue = currentState.laterQueue + currentWord
        val nextIndex = currentState.currentIndex + 1

        val newState = if (nextIndex < currentState.words.size) {
            currentState.copy(
                currentIndex = nextIndex,
                isFlipped = false,
                laterQueue = newLaterQueue
            )
        } else if (newLaterQueue.isNotEmpty()) {
            laterQueueCycleCount++
            if (laterQueueCycleCount > maxLaterQueueCycles) {
                forceCompleteSession(currentState.sessionId)
                return
            }
            currentState.copy(
                words = newLaterQueue,
                currentIndex = 0,
                isFlipped = false,
                laterQueue = emptyList()
            )
        } else {
            return
        }

        _uiState.value = newState

        // Save progress after skipping
        viewModelScope.launch {
            saveProgress(newState)
        }
    }

    /**
     * Reset the study session for retry.
     */
    fun resetSession() {
        loadWords(currentLevelId)
    }

    // ==================== Ad Methods (Added) ====================

    /**
     * Check if ads should be shown (not premium).
     */
    fun shouldShowAds(): Boolean {
        return adManager.shouldShowAds()
    }

    /**
     * Check if interstitial should be shown based on frequency cap.
     */
    fun shouldShowInterstitial(): Boolean {
        return adManager.shouldShowInterstitialByFrequency()
    }

    /**
     * Pre-load interstitial ad for showing at study completion.
     */
    fun preloadInterstitialAd() {
        adManager.loadInterstitialAd()
    }

    // ==================== Review Limit Methods ====================

    /**
     * Check if the user can review more today.
     */
    suspend fun canReviewMore(): Boolean {
        val isPremium = settingsRepository.isPremiumSync()
        return unlockRepository.canReviewMore(isPremium)
    }

    /**
     * Get remaining reviews for today.
     */
    suspend fun getRemainingReviews(): Int {
        val isPremium = settingsRepository.isPremiumSync()
        return unlockRepository.getRemainingReviews(isPremium)
    }
}
