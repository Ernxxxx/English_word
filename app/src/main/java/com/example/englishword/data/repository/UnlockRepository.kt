package com.example.englishword.data.repository

import androidx.room.withTransaction
import com.example.englishword.data.local.AppDatabase
import com.example.englishword.data.local.dao.UnitUnlockDao
import com.example.englishword.data.local.dao.UserSettingsDao
import com.example.englishword.data.local.entity.UnitUnlock
import com.example.englishword.data.local.entity.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UnlockRepository @Inject constructor(
    private val database: AppDatabase,
    private val unitUnlockDao: UnitUnlockDao,
    private val userSettingsDao: UserSettingsDao
) {
    companion object {
        const val UNLOCK_DURATION_HOURS = 3
        const val FREE_DAILY_REVIEW_LIMIT = 10

        private const val KEY_TODAY_REVIEW_COUNT = "today_review_count"
        private const val KEY_REVIEW_COUNT_DATE = "review_count_date"
        private const val KEY_LAST_TRUSTED_TIME_MS = "last_trusted_time_ms"
    }

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    // ==================== Unit Unlock ====================

    /**
     * Check if a unit (level) is unlocked.
     * Parent levels (学年) are always unlocked.
     * Child levels (ユニット) need to be unlocked via ad or premium.
     */
    suspend fun isUnitUnlocked(levelId: Long, isPremium: Boolean, isParentLevel: Boolean): Boolean {
        // Premium users have everything unlocked
        if (isPremium) return true

        // Parent levels are always accessible
        if (isParentLevel) return true

        // Check unlock status for child levels
        val unlock = unitUnlockDao.getUnlock(levelId)
        val trustedNow = getTrustedNowMillis()
        return unlock != null && unlock.unlockUntil > trustedNow
    }

    /**
     * Get unlock status as Flow.
     */
    fun getUnlockStatusFlow(levelId: Long): Flow<Boolean> {
        return unitUnlockDao.getUnlockFlow(levelId).map { unlock ->
            unlock?.isUnlocked() == true
        }
    }

    /**
     * Get all unlocks as Flow.
     */
    fun getAllUnlocksFlow(): Flow<List<UnitUnlock>> {
        return unitUnlockDao.getAllUnlocks()
    }

    /**
     * Unlock a unit for 3 hours after watching an ad.
     */
    suspend fun unlockUnitWithAd(levelId: Long) {
        val unlockUntil = getTrustedNowMillis() + (UNLOCK_DURATION_HOURS * 60 * 60 * 1000)
        val unlock = UnitUnlock(
            levelId = levelId,
            unlockUntil = unlockUntil
        )
        unitUnlockDao.insertOrUpdate(unlock)
    }

    /**
     * Get remaining unlock time in milliseconds.
     */
    suspend fun getRemainingUnlockTime(levelId: Long): Long {
        val unlock = unitUnlockDao.getUnlock(levelId) ?: return 0
        val remaining = unlock.unlockUntil - getTrustedNowMillis()
        return if (remaining > 0) remaining else 0
    }

    /**
     * Batch-load all currently unlocked level IDs.
     * Returns a set of level IDs that are currently unlocked (unlockUntil > now).
     * This avoids N+1 queries when checking unlock status for multiple levels.
     */
    suspend fun getUnlockedLevelIds(): Set<Long> {
        val now = getTrustedNowMillis()
        return unitUnlockDao.getActiveUnlocks(now)
            .map { it.levelId }
            .toSet()
    }

    /**
     * Batch-load remaining unlock times for all levels.
     * Returns a map of levelId to remaining time in milliseconds.
     * Only includes levels that are currently unlocked.
     */
    suspend fun getRemainingUnlockTimes(): Map<Long, Long> {
        val now = getTrustedNowMillis()
        return unitUnlockDao.getActiveUnlocks(now)
            .associate { unlock ->
                val remaining = unlock.unlockUntil - now
                unlock.levelId to if (remaining > 0) remaining else 0L
            }
    }

    // ==================== Daily Review Limit ====================

    /**
     * Get today's review count.
     * Uses a Room transaction to ensure the date-check and reset are atomic.
     */
    suspend fun getTodayReviewCount(): Int {
        return database.withTransaction {
            val today = toDateString(getTrustedNowMillisInternal())
            val savedDate = userSettingsDao.getValue(KEY_REVIEW_COUNT_DATE)

            // Reset count if it's a new day
            if (savedDate != today) {
                userSettingsDao.insert(UserSettings(KEY_REVIEW_COUNT_DATE, today))
                userSettingsDao.insert(UserSettings(KEY_TODAY_REVIEW_COUNT, "0"))
                0
            } else {
                userSettingsDao.getValue(KEY_TODAY_REVIEW_COUNT)?.toIntOrNull() ?: 0
            }
        }
    }

    /**
     * Check if user can review more words today (free tier).
     */
    suspend fun canReviewMore(isPremium: Boolean): Boolean {
        if (isPremium) return true
        return getTodayReviewCount() < FREE_DAILY_REVIEW_LIMIT
    }

    /**
     * Get remaining reviews for today.
     */
    suspend fun getRemainingReviews(isPremium: Boolean): Int {
        if (isPremium) return Int.MAX_VALUE
        val count = getTodayReviewCount()
        return (FREE_DAILY_REVIEW_LIMIT - count).coerceAtLeast(0)
    }

    /**
     * Increment today's review count.
     * Uses a Room transaction to ensure the date-check, reset, and increment are atomic.
     */
    suspend fun incrementReviewCount() {
        database.withTransaction {
            val today = toDateString(getTrustedNowMillisInternal())
            val savedDate = userSettingsDao.getValue(KEY_REVIEW_COUNT_DATE)

            if (savedDate != today) {
                // New day, reset count and set to 1
                userSettingsDao.insert(UserSettings(KEY_REVIEW_COUNT_DATE, today))
                userSettingsDao.insert(UserSettings(KEY_TODAY_REVIEW_COUNT, "1"))
            } else {
                val currentCount = userSettingsDao.getValue(KEY_TODAY_REVIEW_COUNT)?.toIntOrNull() ?: 0
                userSettingsDao.insert(UserSettings(KEY_TODAY_REVIEW_COUNT, (currentCount + 1).toString()))
            }
        }
    }

    /**
     * Get today's review count as Flow.
     */
    fun getTodayReviewCountFlow(): Flow<Int> {
        return userSettingsDao.getValueFlow(KEY_TODAY_REVIEW_COUNT).map { value ->
            value?.toIntOrNull() ?: 0
        }
    }

    private suspend fun getTrustedNowMillis(): Long {
        return database.withTransaction {
            getTrustedNowMillisInternal()
        }
    }

    private suspend fun getTrustedNowMillisInternal(): Long {
        val now = System.currentTimeMillis()
        val lastTrusted = userSettingsDao.getValue(KEY_LAST_TRUSTED_TIME_MS)?.toLongOrNull() ?: now
        val trustedNow = maxOf(now, lastTrusted)
        if (trustedNow != lastTrusted) {
            userSettingsDao.insert(UserSettings(KEY_LAST_TRUSTED_TIME_MS, trustedNow.toString()))
        }
        return trustedNow
    }

    private fun toDateString(epochMillis: Long): String {
        val date = Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        return date.format(dateFormatter)
    }
}
