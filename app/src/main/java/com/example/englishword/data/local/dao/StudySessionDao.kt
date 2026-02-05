package com.example.englishword.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.englishword.data.local.entity.StudySession
import kotlinx.coroutines.flow.Flow

@Dao
interface StudySessionDao {

    @Query("SELECT * FROM study_sessions ORDER BY startedAt DESC")
    fun getAllSessions(): Flow<List<StudySession>>

    @Query("SELECT * FROM study_sessions WHERE levelId = :levelId ORDER BY startedAt DESC")
    fun getSessionsByLevel(levelId: Long): Flow<List<StudySession>>

    @Query("SELECT * FROM study_sessions WHERE id = :id")
    fun getSessionById(id: Long): Flow<StudySession?>

    @Query("SELECT * FROM study_sessions WHERE id = :id")
    suspend fun getSessionByIdSync(id: Long): StudySession?

    // Get incomplete sessions (not yet completed)
    @Query("SELECT * FROM study_sessions WHERE completedAt IS NULL ORDER BY startedAt DESC")
    fun getIncompleteSessions(): Flow<List<StudySession>>

    // Get completed sessions
    @Query("SELECT * FROM study_sessions WHERE completedAt IS NOT NULL ORDER BY completedAt DESC")
    fun getCompletedSessions(): Flow<List<StudySession>>

    // Get sessions within a date range
    @Query("""
        SELECT * FROM study_sessions
        WHERE startedAt >= :startTime AND startedAt <= :endTime
        ORDER BY startedAt DESC
    """)
    fun getSessionsInRange(startTime: Long, endTime: Long): Flow<List<StudySession>>

    // Get recent sessions
    @Query("SELECT * FROM study_sessions ORDER BY startedAt DESC LIMIT :limit")
    fun getRecentSessions(limit: Int = 10): Flow<List<StudySession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: StudySession): Long

    @Update
    suspend fun update(session: StudySession)

    @Delete
    suspend fun delete(session: StudySession)

    @Query("DELETE FROM study_sessions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM study_sessions")
    suspend fun deleteAll()

    // Complete a session
    @Query("""
        UPDATE study_sessions
        SET completedAt = :completedAt,
            wordCount = :wordCount,
            masteredCount = :masteredCount
        WHERE id = :sessionId
    """)
    suspend fun completeSession(
        sessionId: Long,
        completedAt: Long = System.currentTimeMillis(),
        wordCount: Int,
        masteredCount: Int
    )

    // Statistics
    @Query("SELECT COUNT(*) FROM study_sessions")
    fun getTotalSessionCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM study_sessions WHERE completedAt IS NOT NULL")
    fun getCompletedSessionCount(): Flow<Int>

    @Query("SELECT SUM(wordCount) FROM study_sessions WHERE completedAt IS NOT NULL")
    fun getTotalWordsStudied(): Flow<Int?>

    @Query("SELECT SUM(masteredCount) FROM study_sessions WHERE completedAt IS NOT NULL")
    fun getTotalWordsMastered(): Flow<Int?>

    // Session progress persistence
    @Query("""
        UPDATE study_sessions
        SET currentIndex = :currentIndex,
            knownCount = :knownCount,
            againCount = :againCount,
            laterCount = :laterCount,
            wordIds = :wordIds,
            laterQueueIds = :laterQueueIds,
            isReversed = :isReversed
        WHERE id = :sessionId
    """)
    suspend fun updateSessionProgress(
        sessionId: Long,
        currentIndex: Int,
        knownCount: Int,
        againCount: Int,
        laterCount: Int,
        wordIds: String,
        laterQueueIds: String,
        isReversed: Boolean
    )

    // Get most recent incomplete session for a level
    @Query("""
        SELECT * FROM study_sessions
        WHERE levelId = :levelId AND completedAt IS NULL AND wordIds != ''
        ORDER BY startedAt DESC
        LIMIT 1
    """)
    suspend fun getIncompleteSessionForLevel(levelId: Long): StudySession?

    // Delete incomplete sessions older than given timestamp
    @Query("DELETE FROM study_sessions WHERE completedAt IS NULL AND startedAt < :beforeTime")
    suspend fun deleteOldIncompleteSessions(beforeTime: Long)
}
