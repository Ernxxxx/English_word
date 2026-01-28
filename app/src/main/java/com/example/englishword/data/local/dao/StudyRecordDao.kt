package com.example.englishword.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.englishword.data.local.entity.StudyRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyRecordDao {

    @Query("SELECT * FROM study_records ORDER BY reviewedAt DESC")
    fun getAllRecords(): Flow<List<StudyRecord>>

    @Query("SELECT * FROM study_records WHERE sessionId = :sessionId ORDER BY reviewedAt ASC")
    fun getRecordsBySession(sessionId: Long): Flow<List<StudyRecord>>

    @Query("SELECT * FROM study_records WHERE sessionId = :sessionId ORDER BY reviewedAt ASC")
    suspend fun getRecordsBySessionSync(sessionId: Long): List<StudyRecord>

    @Query("SELECT * FROM study_records WHERE wordId = :wordId ORDER BY reviewedAt DESC")
    fun getRecordsByWord(wordId: Long): Flow<List<StudyRecord>>

    @Query("SELECT * FROM study_records WHERE wordId = :wordId ORDER BY reviewedAt DESC")
    suspend fun getRecordsByWordSync(wordId: Long): List<StudyRecord>

    @Query("SELECT * FROM study_records WHERE id = :id")
    fun getRecordById(id: Long): Flow<StudyRecord?>

    @Query("SELECT * FROM study_records WHERE id = :id")
    suspend fun getRecordByIdSync(id: Long): StudyRecord?

    // Get records within a date range
    @Query("""
        SELECT * FROM study_records
        WHERE reviewedAt >= :startTime AND reviewedAt <= :endTime
        ORDER BY reviewedAt DESC
    """)
    fun getRecordsInRange(startTime: Long, endTime: Long): Flow<List<StudyRecord>>

    // Get recent records for a word
    @Query("SELECT * FROM study_records WHERE wordId = :wordId ORDER BY reviewedAt DESC LIMIT :limit")
    suspend fun getRecentRecordsForWord(wordId: Long, limit: Int = 5): List<StudyRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: StudyRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<StudyRecord>): List<Long>

    @Delete
    suspend fun delete(record: StudyRecord)

    @Query("DELETE FROM study_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM study_records WHERE sessionId = :sessionId")
    suspend fun deleteBySession(sessionId: Long)

    @Query("DELETE FROM study_records WHERE wordId = :wordId")
    suspend fun deleteByWord(wordId: Long)

    @Query("DELETE FROM study_records")
    suspend fun deleteAll()

    // Statistics
    @Query("SELECT COUNT(*) FROM study_records")
    fun getTotalRecordCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM study_records WHERE sessionId = :sessionId")
    fun getRecordCountBySession(sessionId: Long): Flow<Int>

    // Count by result type
    @Query("SELECT COUNT(*) FROM study_records WHERE sessionId = :sessionId AND result = :result")
    suspend fun getResultCount(sessionId: Long, result: Int): Int

    // Get accuracy for a session (percentage of result = 2)
    @Query("""
        SELECT CAST(SUM(CASE WHEN result = 2 THEN 1 ELSE 0 END) AS FLOAT) / COUNT(*) * 100
        FROM study_records
        WHERE sessionId = :sessionId
    """)
    suspend fun getSessionAccuracy(sessionId: Long): Float?

    // Get accuracy for a word
    @Query("""
        SELECT CAST(SUM(CASE WHEN result = 2 THEN 1 ELSE 0 END) AS FLOAT) / COUNT(*) * 100
        FROM study_records
        WHERE wordId = :wordId
    """)
    suspend fun getWordAccuracy(wordId: Long): Float?
}
