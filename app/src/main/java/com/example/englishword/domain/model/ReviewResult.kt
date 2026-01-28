package com.example.englishword.domain.model

/**
 * Represents the result of a word review session.
 * Used to determine how the spaced repetition algorithm adjusts the word's mastery level.
 */
enum class ReviewResult(val value: Int) {
    /**
     * User doesn't know the word.
     * Decreases the mastery level.
     */
    AGAIN(0),

    /**
     * User wants to review again later (partial knowledge).
     * Keeps the mastery level unchanged.
     */
    LATER(1),

    /**
     * User knows the word well.
     * Increases the mastery level.
     */
    KNOWN(2);

    companion object {
        /**
         * Gets ReviewResult from its integer value.
         *
         * @param value The integer value (0, 1, or 2)
         * @return The corresponding ReviewResult, defaults to LATER if invalid
         */
        fun fromValue(value: Int): ReviewResult {
            return entries.find { it.value == value } ?: LATER
        }

        /**
         * Integer constant for AGAIN result.
         */
        const val AGAIN_VALUE = 0

        /**
         * Integer constant for LATER result.
         */
        const val LATER_VALUE = 1

        /**
         * Integer constant for KNOWN result.
         */
        const val KNOWN_VALUE = 2
    }
}
