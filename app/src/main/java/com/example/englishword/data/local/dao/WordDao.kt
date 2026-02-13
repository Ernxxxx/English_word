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
    val masteredCount: Int,
    val inProgressCount: Int
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
               SUM(CASE WHEN masteryLevel >= :maxMasteryLevel THEN 1 ELSE 0 END) as masteredCount,
               SUM(CASE WHEN masteryLevel >= 1 AND masteryLevel < :maxMasteryLevel THEN 1 ELSE 0 END) as inProgressCount
        FROM words
        GROUP BY levelId
    """)
    suspend fun getLevelWordStats(maxMasteryLevel: Int): List<LevelWordStats>

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

    // Get unmastered words for study (due reviews first, then new words)
    @Query("""
        SELECT * FROM words
        WHERE levelId = :levelId
        AND masteryLevel < :maxMasteryLevel
        AND (nextReviewAt IS NULL OR nextReviewAt <= :currentTime)
        ORDER BY
            CASE WHEN nextReviewAt IS NULL THEN 1 ELSE 0 END,
            nextReviewAt ASC
        LIMIT :limit
    """)
    suspend fun getWordsForReview(
        levelId: Long,
        currentTime: Long,
        maxMasteryLevel: Int,
        limit: Int = 20
    ): List<Word>

    // Get only due review words (exclude new words)
    @Query("""
        SELECT * FROM words
        WHERE levelId = :levelId
        AND masteryLevel < :maxMasteryLevel
        AND nextReviewAt IS NOT NULL
        AND nextReviewAt <= :currentTime
        ORDER BY nextReviewAt ASC
        LIMIT :limit
    """)
    suspend fun getDueWordsForReview(
        levelId: Long,
        currentTime: Long,
        maxMasteryLevel: Int,
        limit: Int = 20
    ): List<Word>

    // Get only new words (never reviewed)
    @Query("""
        SELECT * FROM words
        WHERE levelId = :levelId
        AND masteryLevel < :maxMasteryLevel
        AND nextReviewAt IS NULL
        ORDER BY createdAt ASC
        LIMIT :limit
    """)
    suspend fun getNewWordsForReview(
        levelId: Long,
        maxMasteryLevel: Int,
        limit: Int = 20
    ): List<Word>

    // Get all words due for review across all levels
    @Query("""
        SELECT * FROM words
        WHERE nextReviewAt IS NULL OR nextReviewAt <= :currentTime
        ORDER BY
            CASE WHEN nextReviewAt IS NULL THEN 1 ELSE 0 END,
            nextReviewAt ASC
        LIMIT :limit
    """)
    suspend fun getAllWordsForReview(currentTime: Long, limit: Int = 20): List<Word>

    // Get words by mastery level
    @Query("SELECT * FROM words WHERE levelId = :levelId AND masteryLevel = :masteryLevel")
    fun getWordsByMasteryLevel(levelId: Long, masteryLevel: Int): Flow<List<Word>>

    // Get mastered words (masteryLevel >= maxMasteryLevel)
    @Query("SELECT * FROM words WHERE levelId = :levelId AND masteryLevel >= :maxMasteryLevel")
    fun getMasteredWords(levelId: Long, maxMasteryLevel: Int): Flow<List<Word>>

    // Get unmastered words (masteryLevel < maxMasteryLevel)
    @Query("SELECT * FROM words WHERE levelId = :levelId AND masteryLevel < :maxMasteryLevel")
    fun getUnmasteredWords(levelId: Long, maxMasteryLevel: Int): Flow<List<Word>>

    // Search words
    @Query("""
        SELECT * FROM words
        WHERE english LIKE '%' || :query || '%'
        OR japanese LIKE '%' || :query || '%'
        ORDER BY english ASC
    """)
    fun searchWords(query: String): Flow<List<Word>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(word: Word): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
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

    @Query("SELECT COUNT(*) FROM words WHERE levelId = :levelId AND masteryLevel >= :maxMasteryLevel")
    fun getMasteredCountByLevel(levelId: Long, maxMasteryLevel: Int): Flow<Int>

    @Query("SELECT COUNT(*) FROM words WHERE masteryLevel >= :maxMasteryLevel")
    fun getTotalMasteredCount(maxMasteryLevel: Int): Flow<Int>

    // Suspend count queries for statistics screen
    @Query("SELECT COUNT(*) FROM words")
    suspend fun getTotalWordCountSync(): Int

    @Query("SELECT COUNT(*) FROM words WHERE masteryLevel >= :maxMasteryLevel")
    suspend fun getTotalMasteredCountSync(maxMasteryLevel: Int): Int

    // Mastery distribution for statistics screen
    @Query("SELECT masteryLevel, COUNT(*) as count FROM words GROUP BY masteryLevel ORDER BY masteryLevel ASC")
    suspend fun getMasteryDistribution(): List<MasteryCount>

    // Get all mastered words (for stats detail view)
    @Query("SELECT * FROM words WHERE masteryLevel >= :maxMasteryLevel ORDER BY updatedAt DESC")
    suspend fun getMasteredWordsListSync(maxMasteryLevel: Int): List<Word>

    // Get words studied today (from study records)
    @Query("""
        SELECT DISTINCT w.* FROM words w
        INNER JOIN study_records sr ON w.id = sr.wordId
        WHERE sr.reviewedAt >= :todayStartMs
        ORDER BY sr.reviewedAt DESC
    """)
    suspend fun getTodayStudiedWordsSync(todayStartMs: Long): List<Word>

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
