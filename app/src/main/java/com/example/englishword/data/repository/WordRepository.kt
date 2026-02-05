package com.example.englishword.data.repository

import com.example.englishword.data.local.dao.WordDao
import com.example.englishword.data.local.entity.Word
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for Word CRUD operations and SRS review functionality.
 * Provides reactive data access through Flow and suspend functions for write operations.
 */
@Singleton
class WordRepository @Inject constructor(
    private val wordDao: WordDao
) {

    // ==================== Read Operations ====================

    /**
     * Get all words ordered by creation date (newest first).
     */
    fun getAllWords(): Flow<List<Word>> {
        return wordDao.getAllWords()
            .catch { e ->
                emit(emptyList())
            }
    }

    /**
     * Get all words for a specific level.
     */
    fun getWordsByLevel(levelId: Long): Flow<List<Word>> {
        return wordDao.getWordsByLevel(levelId)
            .catch { e ->
                emit(emptyList())
            }
    }

    /**
     * Get a word by its ID.
     */
    fun getWordById(wordId: Long): Flow<Word?> {
        return wordDao.getWordById(wordId)
            .catch { e ->
                emit(null)
            }
    }

    /**
     * Get a word by ID synchronously.
     */
    suspend fun getWordByIdSync(wordId: Long): Word? {
        return try {
            wordDao.getWordByIdSync(wordId)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get multiple words by their IDs.
     * Preserves the order of input IDs in the result.
     */
    suspend fun getWordsByIds(wordIds: List<Long>): List<Word> {
        if (wordIds.isEmpty()) return emptyList()
        return try {
            val words = wordDao.getWordsByIds(wordIds)
            // Preserve original order
            val wordMap = words.associateBy { it.id }
            wordIds.mapNotNull { wordMap[it] }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Search words by English or Japanese text.
     */
    fun searchWords(query: String): Flow<List<Word>> {
        if (query.isBlank()) {
            return getAllWords()
        }
        return wordDao.searchWords(query)
            .catch { e ->
                emit(emptyList())
            }
    }

    // ==================== Count Operations ====================

    /**
     * Get total word count.
     */
    fun getTotalWordCount(): Flow<Int> {
        return wordDao.getTotalWordCount()
            .catch { e ->
                emit(0)
            }
    }

    /**
     * Get word count for a specific level.
     */
    fun getWordCountByLevel(levelId: Long): Flow<Int> {
        return wordDao.getWordCountByLevel(levelId)
            .catch { e ->
                emit(0)
            }
    }

    /**
     * Get total mastered word count.
     */
    fun getTotalMasteredCount(): Flow<Int> {
        return wordDao.getTotalMasteredCount()
            .catch { e ->
                emit(0)
            }
    }

    /**
     * Get mastered word count for a specific level.
     */
    fun getMasteredCountByLevel(levelId: Long): Flow<Int> {
        return wordDao.getMasteredCountByLevel(levelId)
            .catch { e ->
                emit(0)
            }
    }

    // ==================== SRS Review Operations ====================

    /**
     * Get words that are due for review for a specific level.
     * Returns new words (nextReviewAt is null) first, then words due for review.
     */
    suspend fun getWordsForReview(levelId: Long, limit: Int = 20): List<Word> {
        return try {
            val currentTime = System.currentTimeMillis()
            wordDao.getWordsForReview(levelId, currentTime, limit)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Get words that are due for review across all levels.
     */
    suspend fun getAllWordsForReview(limit: Int = 20): List<Word> {
        return try {
            val currentTime = System.currentTimeMillis()
            wordDao.getAllWordsForReview(currentTime, limit)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Get words by mastery level for a specific level.
     */
    fun getWordsByMasteryLevel(levelId: Long, masteryLevel: Int): Flow<List<Word>> {
        return wordDao.getWordsByMasteryLevel(levelId, masteryLevel)
            .catch { e ->
                emit(emptyList())
            }
    }

    /**
     * Get mastered words (masteryLevel = 5) for a specific level.
     */
    fun getMasteredWords(levelId: Long): Flow<List<Word>> {
        return wordDao.getMasteredWords(levelId)
            .catch { e ->
                emit(emptyList())
            }
    }

    /**
     * Get unmastered words (masteryLevel < 5) for a specific level.
     */
    fun getUnmasteredWords(levelId: Long): Flow<List<Word>> {
        return wordDao.getUnmasteredWords(levelId)
            .catch { e ->
                emit(emptyList())
            }
    }

    // ==================== Write Operations ====================

    /**
     * Insert a new word.
     * Returns the ID of the inserted word, or -1 on error.
     */
    suspend fun insertWord(word: Word): Long {
        return try {
            wordDao.insert(word)
        } catch (e: Exception) {
            -1L
        }
    }

    /**
     * Insert a new word with the given parameters.
     */
    suspend fun insertWord(
        levelId: Long,
        english: String,
        japanese: String,
        exampleEn: String? = null,
        exampleJa: String? = null
    ): Long {
        return try {
            val word = Word(
                levelId = levelId,
                english = english,
                japanese = japanese,
                exampleEn = exampleEn,
                exampleJa = exampleJa
            )
            wordDao.insert(word)
        } catch (e: Exception) {
            -1L
        }
    }

    /**
     * Insert multiple words.
     * Returns the list of inserted IDs.
     */
    suspend fun insertWords(words: List<Word>): List<Long> {
        return try {
            wordDao.insertAll(words)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Update an existing word.
     * Returns true on success, false on error.
     */
    suspend fun updateWord(word: Word): Boolean {
        return try {
            wordDao.update(word.copy(updatedAt = System.currentTimeMillis()))
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Update word mastery level and calculate next review time using SRS algorithm.
     *
     * @param wordId The ID of the word to update
     * @param isCorrect Whether the user answered correctly
     * @param quality Quality of recall (0-5): 0=complete blackout, 5=perfect response
     */
    suspend fun updateMastery(wordId: Long, isCorrect: Boolean, quality: Int = if (isCorrect) 4 else 1): Boolean {
        return try {
            val word = wordDao.getWordByIdSync(wordId) ?: return false

            val newMasteryLevel = when {
                !isCorrect -> maxOf(0, word.masteryLevel - 1)
                word.masteryLevel < 5 -> word.masteryLevel + 1
                else -> 5
            }

            val nextReviewAt = calculateNextReviewTime(newMasteryLevel, quality)

            wordDao.updateMastery(
                wordId = wordId,
                masteryLevel = newMasteryLevel,
                nextReviewAt = nextReviewAt
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Calculate the next review time based on mastery level and quality.
     * Uses a simplified SM-2 algorithm.
     */
    private fun calculateNextReviewTime(masteryLevel: Int, quality: Int): Long {
        val now = System.currentTimeMillis()
        val intervalMinutes = when (masteryLevel) {
            0 -> 1L           // 1 minute
            1 -> 10L          // 10 minutes
            2 -> 60L          // 1 hour
            3 -> 24 * 60L     // 1 day
            4 -> 3 * 24 * 60L // 3 days
            5 -> 7 * 24 * 60L // 7 days
            else -> 7 * 24 * 60L
        }

        // Adjust interval based on quality
        val qualityMultiplier = when {
            quality <= 1 -> 0.5
            quality == 2 -> 0.75
            quality == 3 -> 1.0
            quality == 4 -> 1.25
            else -> 1.5
        }

        val adjustedInterval = (intervalMinutes * qualityMultiplier).toLong()
        return now + (adjustedInterval * 60 * 1000) // Convert to milliseconds
    }

    /**
     * Reset progress for a specific word.
     */
    suspend fun resetWordProgress(wordId: Long): Boolean {
        return try {
            val word = wordDao.getWordByIdSync(wordId) ?: return false
            wordDao.update(
                word.copy(
                    masteryLevel = 0,
                    nextReviewAt = null,
                    reviewCount = 0,
                    updatedAt = System.currentTimeMillis()
                )
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Reset progress for all words in a level.
     */
    suspend fun resetLevelProgress(levelId: Long): Boolean {
        return try {
            val words = wordDao.getWordsByLevelSync(levelId)
            words.forEach { word ->
                wordDao.update(
                    word.copy(
                        masteryLevel = 0,
                        nextReviewAt = null,
                        reviewCount = 0,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    // ==================== Delete Operations ====================

    /**
     * Delete a word by its ID.
     * Returns true on success, false on error.
     */
    suspend fun deleteWord(wordId: Long): Boolean {
        return try {
            wordDao.deleteById(wordId)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Delete a word.
     */
    suspend fun deleteWord(word: Word): Boolean {
        return try {
            wordDao.delete(word)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Delete all words in a level.
     */
    suspend fun deleteWordsByLevel(levelId: Long): Boolean {
        return try {
            wordDao.deleteByLevel(levelId)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Delete all words.
     */
    suspend fun deleteAllWords(): Boolean {
        return try {
            wordDao.deleteAll()
            true
        } catch (e: Exception) {
            false
        }
    }

    // ==================== Utility Operations ====================

    /**
     * Check if a word with the given English text exists.
     */
    suspend fun wordExists(english: String): Boolean {
        return try {
            wordDao.getWordByEnglish(english) != null
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Import words from a list of pairs (english, japanese).
     */
    suspend fun importWords(levelId: Long, wordPairs: List<Pair<String, String>>): Int {
        return try {
            val words = wordPairs.map { (english, japanese) ->
                Word(
                    levelId = levelId,
                    english = english,
                    japanese = japanese
                )
            }
            val ids = wordDao.insertAll(words)
            ids.size
        } catch (e: Exception) {
            0
        }
    }
}
