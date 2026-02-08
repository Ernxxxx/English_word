package com.example.englishword.ui.study

import com.example.englishword.data.local.entity.Word
import com.example.englishword.ui.quiz.QuizOptions

/**
 * Sealed class representing the UI state of the Study screen.
 */
sealed class StudyUiState {

    /**
     * Initial loading state while fetching words.
     */
    data object Loading : StudyUiState()

    /**
     * Active study state with current word and progress.
     */
    data class Studying(
        val words: List<Word>,
        val currentIndex: Int,
        val isFlipped: Boolean,
        val sessionId: Long,
        val laterQueue: List<Word> = emptyList(),
        val isReversed: Boolean = false, // true = 日本語→英語
        // Quiz mode fields
        val isQuizMode: Boolean = false,
        val quizOptions: QuizOptions? = null,
        val selectedAnswerIndex: Int? = null,
        val isQuizAnswered: Boolean = false
    ) : StudyUiState() {
        val currentWord: Word?
            get() = words.getOrNull(currentIndex)

        val totalCount: Int
            get() = words.size

        val progress: Int
            get() = currentIndex + 1

        val isLastWord: Boolean
            get() = currentIndex >= words.size - 1 && laterQueue.isEmpty()
    }

    /**
     * Completed state after finishing all words.
     */
    data class Completed(
        val sessionId: Long,
        val levelId: Long,
        val totalCount: Int,
        val knownCount: Int,
        val againCount: Int,
        val laterCount: Int,
        val streak: Int
    ) : StudyUiState() {
        val accuracy: Float
            get() = if (totalCount > 0) knownCount.toFloat() / totalCount * 100 else 0f
    }

    /**
     * Error state when something goes wrong.
     */
    data class Error(
        val message: String
    ) : StudyUiState()
}

/**
 * Evaluation result for a word.
 */
enum class EvaluationResult {
    /** User doesn't know the word - needs more practice */
    AGAIN,
    /** User wants to review later in this session */
    LATER,
    /** User knows the word */
    KNOWN
}
