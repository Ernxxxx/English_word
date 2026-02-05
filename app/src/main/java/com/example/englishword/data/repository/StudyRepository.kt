package com.example.englishword.data.repository

import com.example.englishword.data.local.dao.StudyRecordDao
import com.example.englishword.data.local.dao.StudySessionDao
import com.example.englishword.data.local.dao.UserStatsDao
import com.example.englishword.data.local.dao.WordDao
import com.example.englishword.data.local.entity.StudyRecord
import com.example.englishword.data.local.entity.StudySession
import com.example.englishword.data.local.entity.UserStats
import com.example.englishword.util.SrsCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for study session management, recording results, and updating SRS.
 * Handles the complete study workflow from session creation to completion.
 */
@Singleton
class StudyRepository @Inject constructor(
    private val studySessionDao: StudySessionDao,
    private val studyRecordDao: StudyRecordDao,
    private val userStatsDao: UserStatsDao,
    private val wordDao: WordDao
) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // ==================== Session Management ====================

    /**
     * Start a new study session for a specific level.
     * Returns the session ID, or -1 on error.
     */
    suspend fun startSession(levelId: Long): Long {
        return try {
            val session = StudySession(
                levelId = levelId,
                startedAt = System.currentTimeMillis()
            )
            studySessionDao.insert(session)
        } catch (e: Exception) {
            -1L
        }
    }

    /**
     * Complete a study session.
     * Updates the session with completion time and statistics.
     */
    suspend fun completeSession(
        sessionId: Long,
        wordCount: Int,
        masteredCount: Int
    ): Boolean {
        return try {
            studySessionDao.completeSession(
                sessionId = sessionId,
                completedAt = System.currentTimeMillis(),
                wordCount = wordCount,
                masteredCount = masteredCount
            )

            // Update daily stats
            updateDailyStats(wordCount)

            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get a session by ID.
     */
    fun getSessionById(sessionId: Long): Flow<StudySession?> {
        return studySessionDao.getSessionById(sessionId)
            .catch { e ->
                emit(null)
            }
    }

    /**
     * Get a session by ID synchronously.
     */
    suspend fun getSessionByIdSync(sessionId: Long): StudySession? {
        return try {
            studySessionDao.getSessionByIdSync(sessionId)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get all sessions ordered by start time.
     */
    fun getAllSessions(): Flow<List<StudySession>> {
        return studySessionDao.getAllSessions()
            .catch { e ->
                emit(emptyList())
            }
    }

    /**
     * Get sessions for a specific level.
     */
    fun getSessionsByLevel(levelId: Long): Flow<List<StudySession>> {
        return studySessionDao.getSessionsByLevel(levelId)
            .catch { e ->
                emit(emptyList())
            }
    }

    /**
     * Get incomplete (in-progress) sessions.
     */
    fun getIncompleteSessions(): Flow<List<StudySession>> {
        return studySessionDao.getIncompleteSessions()
            .catch { e ->
                emit(emptyList())
            }
    }

    /**
     * Get completed sessions.
     */
    fun getCompletedSessions(): Flow<List<StudySession>> {
        return studySessionDao.getCompletedSessions()
            .catch { e ->
                emit(emptyList())
            }
    }

    /**
     * Get recent sessions.
     */
    fun getRecentSessions(limit: Int = 10): Flow<List<StudySession>> {
        return studySessionDao.getRecentSessions(limit)
            .catch { e ->
                emit(emptyList())
            }
    }

    /**
     * Delete a session and its associated records.
     */
    suspend fun deleteSession(sessionId: Long): Boolean {
        return try {
            studyRecordDao.deleteBySession(sessionId)
            studySessionDao.deleteById(sessionId)
            true
        } catch (e: Exception) {
            false
        }
    }

    // ==================== Session Progress Persistence ====================

    /**
     * Save session progress for later recovery.
     * Call this periodically during study to prevent data loss.
     */
    suspend fun saveSessionProgress(
        sessionId: Long,
        currentIndex: Int,
        knownCount: Int,
        againCount: Int,
        laterCount: Int,
        wordIds: List<Long>,
        laterQueueIds: List<Long>,
        isReversed: Boolean
    ): Boolean {
        return try {
            studySessionDao.updateSessionProgress(
                sessionId = sessionId,
                currentIndex = currentIndex,
                knownCount = knownCount,
                againCount = againCount,
                laterCount = laterCount,
                wordIds = StudySession.wordIdsToString(wordIds),
                laterQueueIds = StudySession.wordIdsToString(laterQueueIds),
                isReversed = isReversed
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get an incomplete (in-progress) session for a level.
     * Returns null if no incomplete session exists.
     */
    suspend fun getIncompleteSessionForLevel(levelId: Long): StudySession? {
        return try {
            studySessionDao.getIncompleteSessionForLevel(levelId)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Start a new session with initial progress data.
     * Used when starting a fresh session (not resuming).
     */
    suspend fun startSessionWithProgress(
        levelId: Long,
        wordIds: List<Long>,
        isReversed: Boolean
    ): Long {
        return try {
            val session = StudySession(
                levelId = levelId,
                startedAt = System.currentTimeMillis(),
                wordIds = StudySession.wordIdsToString(wordIds),
                isReversed = isReversed
            )
            studySessionDao.insert(session)
        } catch (e: Exception) {
            -1L
        }
    }

    /**
     * Clean up old incomplete sessions (older than 24 hours).
     * Should be called periodically to prevent database bloat.
     */
    suspend fun cleanupOldIncompleteSessions(): Boolean {
        return try {
            val oneDayAgo = System.currentTimeMillis() - 24 * 60 * 60 * 1000
            studySessionDao.deleteOldIncompleteSessions(oneDayAgo)
            true
        } catch (e: Exception) {
            false
        }
    }

    // ==================== Study Record Management ====================

    /**
     * Record a study result for a word in a session.
     * Also updates the word's mastery level using SRS algorithm.
     *
     * @param sessionId The current study session ID
     * @param wordId The word being studied
     * @param result The result (0=forgot, 1=hard, 2=easy)
     */
    suspend fun recordResult(
        sessionId: Long,
        wordId: Long,
        result: Int
    ): Boolean {
        return try {
            // Insert the study record
            val record = StudyRecord(
                sessionId = sessionId,
                wordId = wordId,
                result = result,
                reviewedAt = System.currentTimeMillis()
            )
            studyRecordDao.insert(record)

            // Update word mastery using SRS
            updateWordMastery(wordId, result)

            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Update word mastery based on study result.
     * Delegates to SrsCalculator for consistent SRS logic across the app.
     *
     * @param wordId The ID of the word to update
     * @param result The review result (0=AGAIN, 1=LATER, 2=KNOWN)
     */
    private suspend fun updateWordMastery(wordId: Long, result: Int) {
        val word = wordDao.getWordByIdSync(wordId) ?: return

        // Use centralized SRS calculator for consistent mastery updates
        val (newMasteryLevel, nextReviewAt) = SrsCalculator.calculateNextReview(
            currentLevel = word.masteryLevel,
            result = result
        )

        wordDao.updateMastery(
            wordId = wordId,
            masteryLevel = newMasteryLevel,
            nextReviewAt = nextReviewAt
        )
    }

    /**
     * Get all records for a session.
     */
    fun getRecordsBySession(sessionId: Long): Flow<List<StudyRecord>> {
        return studyRecordDao.getRecordsBySession(sessionId)
            .catch { e ->
                emit(emptyList())
            }
    }

    /**
     * Get all records for a word.
     */
    fun getRecordsByWord(wordId: Long): Flow<List<StudyRecord>> {
        return studyRecordDao.getRecordsByWord(wordId)
            .catch { e ->
                emit(emptyList())
            }
    }

    /**
     * Get session accuracy percentage.
     */
    suspend fun getSessionAccuracy(sessionId: Long): Float {
        return try {
            studyRecordDao.getSessionAccuracy(sessionId) ?: 0f
        } catch (e: Exception) {
            0f
        }
    }

    /**
     * Get word accuracy percentage.
     */
    suspend fun getWordAccuracy(wordId: Long): Float {
        return try {
            studyRecordDao.getWordAccuracy(wordId) ?: 0f
        } catch (e: Exception) {
            0f
        }
    }

    // ==================== Daily Stats Management ====================

    /**
     * Update daily statistics.
     */
    private suspend fun updateDailyStats(wordsStudied: Int) {
        try {
            val today = dateFormat.format(Date())
            val existingStats = userStatsDao.getStatsByDateSync(today)

            if (existingStats != null) {
                // Update existing stats
                userStatsDao.incrementStudiedCount(today, wordsStudied)
            } else {
                // Create new stats for today
                val yesterday = getYesterdayDate()
                val yesterdayStats = userStatsDao.getStatsByDateSync(yesterday)

                val newStreak = if (yesterdayStats != null && yesterdayStats.studiedCount > 0) {
                    yesterdayStats.streak + 1
                } else {
                    1
                }

                userStatsDao.insert(
                    UserStats(
                        date = today,
                        studiedCount = wordsStudied,
                        streak = newStreak
                    )
                )
            }
        } catch (e: Exception) {
            // Silently fail for stats updates
        }
    }

    private fun getYesterdayDate(): String {
        val yesterday = Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000)
        return dateFormat.format(yesterday)
    }

    /**
     * Get today's stats.
     */
    fun getTodayStats(): Flow<UserStats?> {
        val today = dateFormat.format(Date())
        return userStatsDao.getStatsByDate(today)
            .catch { e ->
                emit(null)
            }
    }

    /**
     * Get recent stats for the last N days.
     */
    fun getRecentStats(days: Int = 7): Flow<List<UserStats>> {
        return userStatsDao.getRecentStats(days)
            .catch { e ->
                emit(emptyList())
            }
    }

    /**
     * Get current study streak.
     */
    suspend fun getCurrentStreak(): Int {
        return try {
            userStatsDao.getCurrentStreak() ?: 0
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Get maximum study streak.
     */
    suspend fun getMaxStreak(): Int {
        return try {
            userStatsDao.getMaxStreak() ?: 0
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Get total words studied across all time.
     */
    fun getTotalWordsStudied(): Flow<Int> {
        return userStatsDao.getTotalStudiedCount()
            .map { it ?: 0 }
            .catch { e ->
                emit(0)
            }
    }

    /**
     * Get average daily studied count.
     */
    fun getAverageStudiedCount(): Flow<Float> {
        return userStatsDao.getAverageStudiedCount()
            .map { it ?: 0f }
            .catch { e ->
                emit(0f)
            }
    }

    // ==================== Session Statistics ====================

    /**
     * Get total number of completed sessions.
     */
    fun getCompletedSessionCount(): Flow<Int> {
        return studySessionDao.getCompletedSessionCount()
            .catch { e ->
                emit(0)
            }
    }

    /**
     * Get total words studied in sessions.
     */
    fun getTotalSessionWordsStudied(): Flow<Int> {
        return studySessionDao.getTotalWordsStudied()
            .map { it ?: 0 }
            .catch { e ->
                emit(0)
            }
    }

    /**
     * Get total words mastered in sessions.
     */
    fun getTotalSessionWordsMastered(): Flow<Int> {
        return studySessionDao.getTotalWordsMastered()
            .map { it ?: 0 }
            .catch { e ->
                emit(0)
            }
    }

    // ==================== Cleanup Operations ====================

    /**
     * Delete all study records.
     */
    suspend fun deleteAllRecords(): Boolean {
        return try {
            studyRecordDao.deleteAll()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Delete all sessions and their records.
     */
    suspend fun deleteAllSessions(): Boolean {
        return try {
            studyRecordDao.deleteAll()
            studySessionDao.deleteAll()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Delete all stats.
     */
    suspend fun deleteAllStats(): Boolean {
        return try {
            userStatsDao.deleteAll()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Reset all study data (sessions, records, stats).
     */
    suspend fun resetAllStudyData(): Boolean {
        return try {
            studyRecordDao.deleteAll()
            studySessionDao.deleteAll()
            userStatsDao.deleteAll()
            true
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * Data class representing a study session with its records.
 */
data class SessionWithRecords(
    val session: StudySession,
    val records: List<StudyRecord>,
    val accuracy: Float
)

/**
 * Data class representing daily study statistics.
 */
data class DailyStudyStats(
    val date: String,
    val wordsStudied: Int,
    val streak: Int,
    val sessionsCompleted: Int = 0
)
