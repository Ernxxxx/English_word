package com.example.englishword.domain.model

/**
 * Represents the state of the study screen.
 * Uses sealed class for type-safe state management.
 */
sealed class StudyState {

    /**
     * Loading state while fetching words or preparing the study session.
     */
    data object Loading : StudyState()

    /**
     * Ready state when words are loaded and ready for study.
     *
     * @property words List of words to study
     * @property currentIndex Current word index in the study session
     */
    data class Ready(
        val words: List<Word>,
        val currentIndex: Int = 0
    ) : StudyState() {

        /**
         * Gets the current word being studied.
         */
        val currentWord: Word?
            get() = words.getOrNull(currentIndex)

        /**
         * Checks if there are more words to study.
         */
        val hasNextWord: Boolean
            get() = currentIndex < words.size - 1

        /**
         * Checks if this is the last word.
         */
        val isLastWord: Boolean
            get() = currentIndex == words.size - 1

        /**
         * Gets the progress as a fraction (0.0 to 1.0).
         */
        val progress: Float
            get() = if (words.isEmpty()) 0f else (currentIndex + 1).toFloat() / words.size

        /**
         * Gets the progress as a percentage string.
         */
        val progressText: String
            get() = "${currentIndex + 1}/${words.size}"

        /**
         * Gets the total number of words in this session.
         */
        val totalWords: Int
            get() = words.size
    }

    /**
     * Completed state when the study session is finished.
     *
     * @property session Summary of the completed study session
     */
    data class Completed(
        val session: StudySession
    ) : StudyState()

    /**
     * Error state when something goes wrong.
     *
     * @property message Error message to display
     */
    data class Error(
        val message: String
    ) : StudyState()

    /**
     * Empty state when there are no words to study.
     *
     * @property reason The reason why there are no words (e.g., "All words mastered")
     */
    data class Empty(
        val reason: String = "No words to study"
    ) : StudyState()
}

/**
 * Represents a completed study session with statistics.
 */
data class StudySession(
    val totalWords: Int,
    val knownCount: Int,
    val laterCount: Int,
    val againCount: Int,
    val durationMillis: Long = 0L,
    val levelId: Long? = null
) {
    /**
     * Gets the accuracy as a percentage (0-100).
     */
    val accuracyPercent: Int
        get() = if (totalWords == 0) 0 else (knownCount * 100) / totalWords

    /**
     * Gets the duration in seconds.
     */
    val durationSeconds: Long
        get() = durationMillis / 1000

    /**
     * Gets a formatted duration string (e.g., "2:30").
     */
    val durationFormatted: String
        get() {
            val minutes = durationSeconds / 60
            val seconds = durationSeconds % 60
            return "$minutes:${seconds.toString().padStart(2, '0')}"
        }

    /**
     * Checks if this was a perfect session (all words known).
     */
    val isPerfect: Boolean
        get() = knownCount == totalWords && totalWords > 0
}
