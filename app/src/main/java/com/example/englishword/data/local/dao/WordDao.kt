package com.example.englishword.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.englishword.data.local.entity.Word
import kotlinx.coroutines.flow.Flow

/**
 * Result of batch query for word count and mastered count per level.
 */
data class LevelWordStats(
    val levelId: Long,
    val wordCount: Int,
    val masteredCount: Int
)

/**
 * Result of mastery distribution query for statistics screen.
 */
data class MasteryCount(
    val masteryLevel: Int,
    val count: Int
)

@Dao
interface WordDao {

    @Query("""
        SELECT levelId,
               COUNT(*) as wordCount,
               SUM(CASE WHEN masteryLevel >= 5 THEN 1 ELSE 0 END) as masteredCount
        FROM words
        GROUP BY levelId
    """)
    suspend fun getLevelWordStats(): List<LevelWordStats>

    @Query("SELECT * FROM words ORDER BY createdAt DESC")
    fun getAllWords(): Flow<List<Word>>

    @Query("SELECT * FROM words WHERE levelId = :levelId ORDER BY createdAt ASC")
    fun getWordsByLevel(levelId: Long): Flow<List<Word>>

    @Query("SELECT * FROM words WHERE levelId = :levelId ORDER BY createdAt ASC")
    suspend fun getWordsByLevelSync(levelId: Long): List<Word>

    @Query("SELECT * FROM words WHERE id = :id")
    fun getWordById(id: Long): Flow<Word?>

    @Query("SELECT * FROM words WHERE id = :id")
    suspend fun getWordByIdSync(id: Long): Word?

    // Get multiple words by IDs (for session recovery)
    @Query("SELECT * FROM words WHERE id IN (:ids)")
    suspend fun getWordsByIds(ids: List<Long>): List<Word>

    @Query("SELECT * FROM words WHERE english = :english LIMIT 1")
    suspend fun getWordByEnglish(english: String): Word?

    // Get words that are due for review (nextReviewAt <= current time or new words)
    @Query("""
        SELECT * FROM words
        WHERE levelId = :levelId
        AND (nextReviewAt IS NULL OR nextReviewAt <= :currentTime)
        ORDER BY
            CASE WHEN nextReviewAt IS NULL THEN 0 ELSE 1 END,
            nextReviewAt ASC
        LIMIT :limit
    """)
    suspend fun getWordsForReview(levelId: Long, currentTime: Long, limit: Int = 20): List<Word>

    // Get all words due for review across all levels
    @Query("""
        SELECT * FROM words
        WHERE nextReviewAt IS NULL OR nextReviewAt <= :currentTime
        ORDER BY
            CASE WHEN nextReviewAt IS NULL THEN 0 ELSE 1 END,
            nextReviewAt ASC
        LIMIT :limit
    """)
    suspend fun getAllWordsForReview(currentTime: Long, limit: Int = 20): List<Word>

    // Get words by mastery level
    @Query("SELECT * FROM words WHERE levelId = :levelId AND masteryLevel = :masteryLevel")
    fun getWordsByMasteryLevel(levelId: Long, masteryLevel: Int): Flow<List<Word>>

    // Get mastered words (masteryLevel = 5)
    @Query("SELECT * FROM words WHERE levelId = :levelId AND masteryLevel = 5")
    fun getMasteredWords(levelId: Long): Flow<List<Word>>

    // Get unmastered words (masteryLevel < 5)
    @Query("SELECT * FROM words WHERE levelId = :levelId AND masteryLevel < 5")
    fun getUnmasteredWords(levelId: Long): Flow<List<Word>>

    // Search words
    @Query("""
        SELECT * FROM words
        WHERE english LIKE '%' || :query || '%'
        OR japanese LIKE '%' || :query || '%'
        ORDER BY english ASC
    """)
    fun searchWords(query: String): Flow<List<Word>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(word: Word): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(words: List<Word>): List<Long>

    @Update
    suspend fun update(word: Word)

    @Delete
    suspend fun delete(word: Word)

    @Query("DELETE FROM words WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM words WHERE levelId = :levelId")
    suspend fun deleteByLevel(levelId: Long)

    @Query("DELETE FROM words")
    suspend fun deleteAll()

    // Count queries
    @Query("SELECT COUNT(*) FROM words")
    fun getTotalWordCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM words WHERE levelId = :levelId")
    fun getWordCountByLevel(levelId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM words WHERE levelId = :levelId AND masteryLevel = 5")
    fun getMasteredCountByLevel(levelId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM words WHERE masteryLevel = 5")
    fun getTotalMasteredCount(): Flow<Int>

    // Suspend count queries for statistics screen
    @Query("SELECT COUNT(*) FROM words")
    suspend fun getTotalWordCountSync(): Int

    @Query("SELECT COUNT(*) FROM words WHERE masteryLevel >= 5")
    suspend fun getTotalMasteredCountSync(): Int

    // Mastery distribution for statistics screen
    @Query("SELECT masteryLevel, COUNT(*) as count FROM words GROUP BY masteryLevel ORDER BY masteryLevel ASC")
    suspend fun getMasteryDistribution(): List<MasteryCount>

    // Update mastery level
    @Query("""
        UPDATE words
        SET masteryLevel = :masteryLevel,
            nextReviewAt = :nextReviewAt,
            reviewCount = reviewCount + 1,
            updatedAt = :updatedAt
        WHERE id = :wordId
    """)
    suspend fun updateMastery(
        wordId: Long,
        masteryLevel: Int,
        nextReviewAt: Long?,
        updatedAt: Long = System.currentTimeMillis()
    )
}
