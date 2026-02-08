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
     * Generate 4-choice quiz options for a given word.
     *
     * @param correctWord The word being tested (its japanese field is the correct answer)
     * @param allWordsInLevel All words in the same level (for same-level distractors)
     * @param allWords All words across all levels (fallback for distractors)
     * @return QuizOptions with 4 shuffled options and the correct answer index
     */
    fun generateOptions(
        correctWord: Word,
        allWordsInLevel: List<Word>,
        allWords: List<Word> = emptyList()
    ): QuizOptions {
        // Build pool of wrong answers (Japanese translations)
        val wrongPool = mutableListOf<String>()

        // First, try same-level words (excluding the correct word and duplicates)
        allWordsInLevel
            .filter { it.id != correctWord.id && it.japanese != correctWord.japanese }
            .shuffled()
            .take(10) // Take more than needed for variety
            .mapTo(wrongPool) { it.japanese }

        // If not enough from the same level, add from all words
        if (wrongPool.size < 3) {
            allWords
                .filter { it.id != correctWord.id && it.japanese != correctWord.japanese && it.japanese !in wrongPool }
                .shuffled()
                .take(3 - wrongPool.size)
                .mapTo(wrongPool) { it.japanese }
        }

        // Select 3 unique wrong answers
        val wrongAnswers = wrongPool.distinct().take(3)

        // If still not enough (very rare edge case), pad with placeholders
        val paddedWrong = wrongAnswers.toMutableList()
        while (paddedWrong.size < 3) {
            paddedWrong.add("---")
        }

        // Combine correct + wrong answers and shuffle
        val allOptions = (paddedWrong + correctWord.japanese).shuffled()
        val correctIndex = allOptions.indexOf(correctWord.japanese)

        return QuizOptions(
            options = allOptions,
            correctIndex = correctIndex
        )
    }
}
