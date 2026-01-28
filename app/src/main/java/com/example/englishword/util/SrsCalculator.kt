package com.example.englishword.util

import com.example.englishword.domain.model.ReviewResult

/**
 * Spaced Repetition System (SRS) Calculator.
 * Calculates the next review time based on the current mastery level and review result.
 */
object SrsCalculator {

    // Intervals in milliseconds
    private const val IMMEDIATE = 0L
    private const val ONE_HOUR = 1L * 60 * 60 * 1000
    private const val EIGHT_HOURS = 8L * 60 * 60 * 1000
    private const val ONE_DAY = 24L * 60 * 60 * 1000
    private const val THREE_DAYS = 3L * 24 * 60 * 60 * 1000
    private const val SEVEN_DAYS = 7L * 24 * 60 * 60 * 1000

    // Level to interval mapping
    private val intervalByLevel = mapOf(
        0 to IMMEDIATE,
        1 to ONE_HOUR,
        2 to EIGHT_HOURS,
        3 to ONE_DAY,
        4 to THREE_DAYS,
        5 to SEVEN_DAYS
    )

    const val MAX_LEVEL = 5
    const val MIN_LEVEL = 0

    /**
     * Calculates the next review parameters based on current level and review result.
     *
     * @param currentLevel The current mastery level (0-5)
     * @param result The review result (AGAIN=0, LATER=1, KNOWN=2)
     * @return Pair of (newLevel, nextReviewTimestamp)
     */
    fun calculateNextReview(currentLevel: Int, result: Int): Pair<Int, Long> {
        val newLevel = when (result) {
            ReviewResult.AGAIN.value -> maxOf(MIN_LEVEL, currentLevel - 1)
            ReviewResult.LATER.value -> currentLevel
            ReviewResult.KNOWN.value -> minOf(MAX_LEVEL, currentLevel + 1)
            else -> currentLevel
        }

        val interval = intervalByLevel[newLevel] ?: IMMEDIATE
        val nextReviewTime = System.currentTimeMillis() + interval

        return Pair(newLevel, nextReviewTime)
    }

    /**
     * Calculates the next review parameters using ReviewResult enum.
     *
     * @param currentLevel The current mastery level (0-5)
     * @param result The ReviewResult enum value
     * @return Pair of (newLevel, nextReviewTimestamp)
     */
    fun calculateNextReview(currentLevel: Int, result: ReviewResult): Pair<Int, Long> {
        return calculateNextReview(currentLevel, result.value)
    }

    /**
     * Gets the interval description for a given level.
     *
     * @param level The mastery level
     * @return Human-readable interval description
     */
    fun getIntervalDescription(level: Int): String {
        return when (level) {
            0 -> "Immediate"
            1 -> "1 hour"
            2 -> "8 hours"
            3 -> "1 day"
            4 -> "3 days"
            5 -> "7 days"
            else -> "Unknown"
        }
    }

    /**
     * Checks if a word is considered mastered.
     *
     * @param level The mastery level
     * @return True if the word is mastered (level >= 5)
     */
    fun isMastered(level: Int): Boolean {
        return level >= MAX_LEVEL
    }
}
