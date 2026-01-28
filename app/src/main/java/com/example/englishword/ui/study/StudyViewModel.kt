package com.example.englishword.ui.study

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.englishword.ads.AdManager
import com.example.englishword.data.local.entity.Word
import com.example.englishword.data.repository.SettingsRepository
import com.example.englishword.data.repository.StudyRepository
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
    val adManager: AdManager // Added: for interstitial ads
) : ViewModel() {

    private val _uiState = MutableStateFlow<StudyUiState>(StudyUiState.Loading)
    val uiState: StateFlow<StudyUiState> = _uiState.asStateFlow()

    // Track evaluation counts for the session
    private var knownCount = 0
    private var againCount = 0
    private var laterCount = 0
    private var currentLevelId: Long = 0

    /**
     * Load words for study from the specified level.
     */
    fun loadWords(levelId: Long) {
        currentLevelId = levelId
        viewModelScope.launch {
            _uiState.value = StudyUiState.Loading

            try {
                val words = wordRepository.getWordsForReview(levelId, limit = 20)

                if (words.isEmpty()) {
                    _uiState.value = StudyUiState.Error("No words available for study")
                    return@launch
                }

                // Start a new study session
                val sessionId = studyRepository.startSession(levelId)

                if (sessionId == -1L) {
                    _uiState.value = StudyUiState.Error("Failed to start study session")
                    return@launch
                }

                // Reset counters
                knownCount = 0
                againCount = 0
                laterCount = 0

                // 設定から出題方向を取得
                val isReversed = settingsRepository.isStudyDirectionReversedSync()

                _uiState.value = StudyUiState.Studying(
                    words = words,
                    currentIndex = 0,
                    isFlipped = false,
                    sessionId = sessionId,
                    laterQueue = emptyList(),
                    isReversed = isReversed
                )
            } catch (e: Exception) {
                _uiState.value = StudyUiState.Error("Failed to load words: ${e.message}")
            }
        }
    }

    /**
     * Toggle the card flip state.
     */
    fun flipCard() {
        val currentState = _uiState.value
        if (currentState is StudyUiState.Studying) {
            _uiState.value = currentState.copy(isFlipped = !currentState.isFlipped)
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

        when (result) {
            EvaluationResult.AGAIN -> {
                // まだ: カードを裏返すだけ（同じ単語をもう一度）
                againCount++
                _uiState.value = currentState.copy(isFlipped = false)

                // Record to database
                viewModelScope.launch {
                    studyRepository.recordResult(sessionId, currentWord.id, 0)
                }
            }
            EvaluationResult.LATER -> {
                // 使わない（互換性のために残す）
                laterCount++
                _uiState.value = currentState.copy(isFlipped = false)
            }
            EvaluationResult.KNOWN -> {
                // 覚えた: 次の単語へ
                knownCount++

                val nextIndex = currentState.currentIndex + 1

                if (nextIndex < currentState.words.size) {
                    _uiState.value = currentState.copy(
                        currentIndex = nextIndex,
                        isFlipped = false
                    )
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

                    // Complete session
                    viewModelScope.launch {
                        studyRepository.completeSession(sessionId, knownCount + againCount, knownCount)
                        val streak = studyRepository.getCurrentStreak()
                        val completedState = _uiState.value
                        if (completedState is StudyUiState.Completed) {
                            _uiState.value = completedState.copy(streak = streak)
                        }
                    }
                }

                // Record to database
                viewModelScope.launch {
                    studyRepository.recordResult(sessionId, currentWord.id, 2)
                }
            }
        }
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

        if (nextIndex < currentState.words.size) {
            _uiState.value = currentState.copy(
                currentIndex = nextIndex,
                isFlipped = false,
                laterQueue = newLaterQueue
            )
        } else if (newLaterQueue.isNotEmpty()) {
            _uiState.value = currentState.copy(
                words = newLaterQueue,
                currentIndex = 0,
                isFlipped = false,
                laterQueue = emptyList()
            )
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
}
