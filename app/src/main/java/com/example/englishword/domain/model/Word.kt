package com.example.englishword.domain.model

/**
 * Represents a word entity for studying.
 * This is a domain model used throughout the app.
 */
data class Word(
    /**
     * Unique identifier for the word.
     */
    val id: Long,

    /**
     * The English word.
     */
    val word: String,

    /**
     * The meaning/definition of the word.
     */
    val meaning: String,

    /**
     * Optional pronunciation guide (IPA or phonetic).
     */
    val pronunciation: String? = null,

    /**
     * Optional example sentence using the word.
     */
    val example: String? = null,

    /**
     * The level/category this word belongs to.
     */
    val levelId: Long,

    /**
     * Current mastery level (0-5) for spaced repetition.
     */
    val masteryLevel: Int = 0,

    /**
     * Timestamp of the next scheduled review.
     */
    val nextReviewAt: Long = 0L,

    /**
     * Timestamp when this word was last reviewed.
     */
    val lastReviewedAt: Long? = null,

    /**
     * Total number of times this word has been reviewed.
     */
    val reviewCount: Int = 0
) {
    /**
     * Checks if this word is due for review.
     */
    val isDueForReview: Boolean
        get() = System.currentTimeMillis() >= nextReviewAt

    /**
     * Checks if this word is mastered (reached max level).
     */
    val isMastered: Boolean
        get() = masteryLevel >= 5

    /**
     * Checks if this word is new (never reviewed).
     */
    val isNew: Boolean
        get() = reviewCount == 0

    /**
     * Gets a display-friendly mastery status.
     */
    val masteryStatus: String
        get() = when {
            isMastered -> "Mastered"
            isNew -> "New"
            else -> "Learning"
        }
}
