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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Study screen.
 * Manages the study session, card flipping, and word evaluation.
 */
@HiltViewModel
class StudyViewModel @Inject constructor(
    private val wordRepository: WordRepository,
    private val studyRepository: StudyRepository,
    private val settingsRepository: SettingsRepository, // Added: for premium check
    private val unlockRepository: UnlockRepository, // Added: for review limits
    val adManager: AdManager // Added: for interstitial ads
) : ViewModel() {

    private val _uiState = MutableStateFlow<StudyUiState>(StudyUiState.Loading)
    val uiState: StateFlow<StudyUiState> = _uiState.asStateFlow()

    // Track evaluation counts for the session
    private var knownCount = 0
    private var againCount = 0
    private var laterCount = 0
    private var currentLevelId: Long = 0

    // Track when card was shown to user (for response time measurement)
    private var cardShownTimestamp: Long = 0L

    /**
     * Load words for study from the specified level.
     * First checks for an incomplete session to resume.
     */
    fun loadWords(levelId: Long) {
        currentLevelId = levelId
        viewModelScope.launch {
            _uiState.value = StudyUiState.Loading

            try {
                // Check for incomplete session to resume
                val incompleteSession = studyRepository.getIncompleteSessionForLevel(levelId)

                if (incompleteSession != null && incompleteSession.isInProgress) {
                    // Resume incomplete session
                    resumeSession(incompleteSession)
                    return@launch
                }

                // Start fresh session
                startNewSession(levelId)
            } catch (e: Exception) {
                _uiState.value = StudyUiState.Error("Failed to load words: ${e.message}")
            }
        }
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

        _uiState.value = StudyUiState.Studying(
            words = words,
            currentIndex = session.currentIndex.coerceAtMost(words.size - 1),
            isFlipped = false,
            sessionId = session.id,
            laterQueue = laterQueue,
            isReversed = session.isReversed
        )
    }

    /**
     * Start a new study session.
     */
    private suspend fun startNewSession(levelId: Long) {
        val words = wordRepository.getWordsForReview(levelId, limit = 20)

        if (words.isEmpty()) {
            _uiState.value = StudyUiState.Error("No words available for study")
            return
        }

        // 設定から出題方向を取得
        val isReversed = settingsRepository.isStudyDirectionReversedSync()

        // Start a new session with progress data
        val wordIds = words.map { it.id }
        val sessionId = studyRepository.startSessionWithProgress(levelId, wordIds, isReversed)

        if (sessionId == -1L) {
            _uiState.value = StudyUiState.Error("Failed to start study session")
            return
        }

        // Reset counters
        knownCount = 0
        againCount = 0
        laterCount = 0

        // Start response time tracking
        cardShownTimestamp = System.currentTimeMillis()

        _uiState.value = StudyUiState.Studying(
            words = words,
            currentIndex = 0,
            isFlipped = false,
            sessionId = sessionId,
            laterQueue = emptyList(),
            isReversed = isReversed
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
                // 使わない（互換性のために残す）
                laterCount++
                _uiState.value = currentState.copy(isFlipped = false)

                // Save progress
                viewModelScope.launch {
                    saveProgress(currentState)
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

                    // Save progress after moving to next word
                    viewModelScope.launch {
                        studyRepository.recordResult(sessionId, currentWord.id, 2, responseTimeMs)
                        saveProgress(newState)
                        // Reset timer for next word
                        cardShownTimestamp = System.currentTimeMillis()
                    }
                } else {
                    // 全単語完了
                    _uiState.value = StudyUiState.Completed(
                        sessionId = sessionId,
                        levelId = currentLevelId,
                        totalCount = knownCount + againCount,
                        knownCount = knownCount,
                        againCount = againCount,
                        laterCount = 0,
                        streak = 0
                    )

                    // Complete session (clears progress data)
                    viewModelScope.launch {
                        studyRepository.recordResult(sessionId, currentWord.id, 2, responseTimeMs)
                        studyRepository.completeSession(sessionId, knownCount + againCount, knownCount)
                        val streak = studyRepository.getCurrentStreak()
                        val completedState = _uiState.value
                        if (completedState is StudyUiState.Completed) {
                            _uiState.value = completedState.copy(streak = streak)
                        }
                    }
                }
            }
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
