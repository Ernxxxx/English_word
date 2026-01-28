package com.example.englishword.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.englishword.data.local.entity.UserStats
import kotlinx.coroutines.flow.Flow

@Dao
interface UserStatsDao {

    @Query("SELECT * FROM user_stats ORDER BY date DESC")
    fun getAllStats(): Flow<List<UserStats>>

    @Query("SELECT * FROM user_stats WHERE date = :date")
    fun getStatsByDate(date: String): Flow<UserStats?>

    @Query("SELECT * FROM user_stats WHERE date = :date")
    suspend fun getStatsByDateSync(date: String): UserStats?

    @Query("SELECT * FROM user_stats ORDER BY date DESC LIMIT 1")
    fun getLatestStats(): Flow<UserStats?>

    @Query("SELECT * FROM user_stats ORDER BY date DESC LIMIT 1")
    suspend fun getLatestStatsSync(): UserStats?

    // Get stats for a date range
    @Query("SELECT * FROM user_stats WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC")
    fun getStatsInRange(startDate: String, endDate: String): Flow<List<UserStats>>

    // Get recent stats
    @Query("SELECT * FROM user_stats ORDER BY date DESC LIMIT :days")
    fun getRecentStats(days: Int = 7): Flow<List<UserStats>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(stats: UserStats): Long

    @Update
    suspend fun update(stats: UserStats)

    @Delete
    suspend fun delete(stats: UserStats)

    @Query("DELETE FROM user_stats WHERE date = :date")
    suspend fun deleteByDate(date: String)

    @Query("DELETE FROM user_stats")
    suspend fun deleteAll()

    // Update or insert today's stats
    @Query("""
        INSERT INTO user_stats (date, studiedCount, streak)
        VALUES (:date, :studiedCount, :streak)
        ON CONFLICT(date) DO UPDATE SET
            studiedCount = :studiedCount,
            streak = :streak
    """)
    suspend fun upsertStats(date: String, studiedCount: Int, streak: Int)

    // Increment studied count for today
    @Query("""
        UPDATE user_stats
        SET studiedCount = studiedCount + :increment
        WHERE date = :date
    """)
    suspend fun incrementStudiedCount(date: String, increment: Int = 1)

    // Get current streak
    @Query("SELECT streak FROM user_stats ORDER BY date DESC LIMIT 1")
    suspend fun getCurrentStreak(): Int?

    // Get max streak
    @Query("SELECT MAX(streak) FROM user_stats")
    suspend fun getMaxStreak(): Int?

    // Get total studied count
    @Query("SELECT SUM(studiedCount) FROM user_stats")
    fun getTotalStudiedCount(): Flow<Int?>

    // Get average daily studied count
    @Query("SELECT AVG(studiedCount) FROM user_stats")
    fun getAverageStudiedCount(): Flow<Float?>
}
