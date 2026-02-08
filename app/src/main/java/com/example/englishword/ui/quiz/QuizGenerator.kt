package com.example.englishword.ui.quiz

import com.example.englishword.data.local.entity.Word

/**
 * Data class representing the 4-choice quiz options.
 * @param options List of 4 Japanese answer strings
 * @param correctIndex Index of the correct answer (0-3)
 */
data class QuizOptions(
    val options: List<String>,
    val correctIndex: Int
)

/**
 * Generator for 4-choice quiz options.
 * Creates distractor options from same-level words, falling back to all words if needed.
 */
object QuizGenerator {

    /**
     * Check if quiz mode can be used for the given word pool.
     * Requires at least 4 unique Japanese translations to generate meaningful distractors.
     */
    fun canGenerateQuiz(allWordsInLevel: List<Word>): Boolean {
        return allWordsInLevel.map { it.japanese }.distinct().size >= 4
    }

    /**
     * Generate 4-choice quiz options for a given word.
     *
     * @param correctWord The word being tested
     * @param allWordsInLevel All words in the same level (for same-level distractors)
     * @param allWords All words across all levels (fallback for distractors)
     * @param isReversed If true, use english as distractors instead of japanese
     * @return QuizOptions with 4 shuffled options and the correct answer index, or null if insufficient distractors
     */
    fun generateOptions(
        correctWord: Word,
        allWordsInLevel: List<Word>,
        allWords: List<Word> = emptyList(),
        isReversed: Boolean = false
    ): QuizOptions? {
        val correctAnswer = if (isReversed) correctWord.english else correctWord.japanese

        // Build pool of wrong answers
        val wrongPool = mutableListOf<String>()

        // First, try same-level words (excluding the correct word and duplicates)
        allWordsInLevel
            .filter {
                it.id != correctWord.id &&
                (if (isReversed) it.english else it.japanese) != correctAnswer
            }
            .shuffled()
            .take(10)
            .mapTo(wrongPool) { if (isReversed) it.english else it.japanese }

        // If not enough from the same level, add from all words
        if (wrongPool.size < 3) {
            allWords
                .filter {
                    it.id != correctWord.id &&
                    (if (isReversed) it.english else it.japanese) != correctAnswer &&
                    (if (isReversed) it.english else it.japanese) !in wrongPool
                }
                .shuffled()
                .take(3 - wrongPool.size)
                .mapTo(wrongPool) { if (isReversed) it.english else it.japanese }
        }

        // Select 3 unique wrong answers
        val wrongAnswers = wrongPool.distinct().take(3)

        // Not enough distractors - return null to fall back to flashcard mode
        if (wrongAnswers.size < 3) return null

        // Combine correct + wrong answers and shuffle
        val allOptions = (wrongAnswers + correctAnswer).shuffled()
        val correctIndex = allOptions.indexOf(correctAnswer)

        return QuizOptions(
            options = allOptions,
            correctIndex = correctIndex
        )
    }
}
