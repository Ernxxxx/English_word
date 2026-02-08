package com.example.englishword.data.repository

import androidx.room.withTransaction
import com.example.englishword.data.local.AppDatabase
import com.example.englishword.data.local.dao.UnitUnlockDao
import com.example.englishword.data.local.dao.UserSettingsDao
import com.example.englishword.data.local.entity.UnitUnlock
import com.example.englishword.data.local.entity.UserSettings
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UnlockRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var unitUnlockDao: UnitUnlockDao
    private lateinit var userSettingsDao: UserSettingsDao
    private lateinit var repository: UnlockRepository

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val today: String get() = dateFormat.format(Date())

    companion object {
        private const val KEY_TODAY_REVIEW_COUNT = "today_review_count"
        private const val KEY_REVIEW_COUNT_DATE = "review_count_date"
    }

    @Before
    fun setUp() {
        database = mockk(relaxed = true)
        unitUnlockDao = mockk(relaxed = true)
        userSettingsDao = mockk(relaxed = true)

        // Mock Room's withTransaction extension to just execute the block directly
        mockkStatic("androidx.room.RoomDatabaseKt")
        val blockSlot = slot<suspend () -> Any?>()
        coEvery { database.withTransaction(capture(blockSlot)) } coAnswers {
            blockSlot.captured.invoke()
        }

        repository = UnlockRepository(database, unitUnlockDao, userSettingsDao)
    }

    // ==================== isUnitUnlocked() ====================

    @Test
    fun `isUnitUnlocked - premium user always returns true`() = runTest {
        // Premium users should have access regardless of unlock status
        val result = repository.isUnitUnlocked(
            levelId = 1L,
            isPremium = true,
            isParentLevel = false
        )

        assertTrue(result)
        // Should not even check the DAO
        coVerify(exactly = 0) { unitUnlockDao.getUnlock(any()) }
    }

    @Test
    fun `isUnitUnlocked - parent level always returns true`() = runTest {
        // Parent levels (grade levels) should always be accessible
        val result = repository.isUnitUnlocked(
            levelId = 1L,
            isPremium = false,
            isParentLevel = true
        )

        assertTrue(result)
        // Should not check the DAO for parent levels
        coVerify(exactly = 0) { unitUnlockDao.getUnlock(any()) }
    }

    @Test
    fun `isUnitUnlocked - child level with valid unlock returns true`() = runTest {
        // Unlock that expires 1 hour from now (still valid)
        val futureTimestamp = System.currentTimeMillis() + 3_600_000L
        val unlock = UnitUnlock(levelId = 5L, unlockUntil = futureTimestamp)

        coEvery { unitUnlockDao.getUnlock(5L) } returns unlock

        val result = repository.isUnitUnlocked(
            levelId = 5L,
            isPremium = false,
            isParentLevel = false
        )

        assertTrue(result)
        coVerify(exactly = 1) { unitUnlockDao.getUnlock(5L) }
    }

    @Test
    fun `isUnitUnlocked - child level with expired unlock returns false`() = runTest {
        // Unlock that expired 1 hour ago
        val pastTimestamp = System.currentTimeMillis() - 3_600_000L
        val unlock = UnitUnlock(levelId = 5L, unlockUntil = pastTimestamp)

        coEvery { unitUnlockDao.getUnlock(5L) } returns unlock

        val result = repository.isUnitUnlocked(
            levelId = 5L,
            isPremium = false,
            isParentLevel = false
        )

        assertFalse(result)
    }

    @Test
    fun `isUnitUnlocked - child level with no unlock record returns false`() = runTest {
        coEvery { unitUnlockDao.getUnlock(5L) } returns null

        val result = repository.isUnitUnlocked(
            levelId = 5L,
            isPremium = false,
            isParentLevel = false
        )

        assertFalse(result)
    }

    // ==================== canReviewMore() ====================

    @Test
    fun `canReviewMore - premium user always returns true`() = runTest {
        // Premium users should never be limited
        val result = repository.canReviewMore(isPremium = true)

        assertTrue(result)
        // Should not check review count for premium users
        coVerify(exactly = 0) { userSettingsDao.getValue(KEY_TODAY_REVIEW_COUNT) }
    }

    @Test
    fun `canReviewMore - free user under limit returns true`() = runTest {
        coEvery { userSettingsDao.getValue(KEY_REVIEW_COUNT_DATE) } returns today
        coEvery { userSettingsDao.getValue(KEY_TODAY_REVIEW_COUNT) } returns "5"

        val result = repository.canReviewMore(isPremium = false)

        assertTrue(result)
    }

    @Test
    fun `canReviewMore - free user at limit returns false`() = runTest {
        coEvery { userSettingsDao.getValue(KEY_REVIEW_COUNT_DATE) } returns today
        coEvery { userSettingsDao.getValue(KEY_TODAY_REVIEW_COUNT) } returns "10"

        val result = repository.canReviewMore(isPremium = false)

        assertFalse(result)
    }

    @Test
    fun `canReviewMore - free user over limit returns false`() = runTest {
        coEvery { userSettingsDao.getValue(KEY_REVIEW_COUNT_DATE) } returns today
        coEvery { userSettingsDao.getValue(KEY_TODAY_REVIEW_COUNT) } returns "15"

        val result = repository.canReviewMore(isPremium = false)

        assertFalse(result)
    }

    // ==================== getRemainingReviews() ====================

    @Test
    fun `getRemainingReviews - premium returns Int MAX_VALUE`() = runTest {
        val result = repository.getRemainingReviews(isPremium = true)

        assertEquals(Int.MAX_VALUE, result)
    }

    @Test
    fun `getRemainingReviews - free with 0 reviews returns 10`() = runTest {
        coEvery { userSettingsDao.getValue(KEY_REVIEW_COUNT_DATE) } returns today
        coEvery { userSettingsDao.getValue(KEY_TODAY_REVIEW_COUNT) } returns "0"

        val result = repository.getRemainingReviews(isPremium = false)

        assertEquals(10, result)
    }

    @Test
    fun `getRemainingReviews - free with 5 reviews returns 5`() = runTest {
        coEvery { userSettingsDao.getValue(KEY_REVIEW_COUNT_DATE) } returns today
        coEvery { userSettingsDao.getValue(KEY_TODAY_REVIEW_COUNT) } returns "5"

        val result = repository.getRemainingReviews(isPremium = false)

        assertEquals(5, result)
    }

    @Test
    fun `getRemainingReviews - free with 10 or more reviews returns 0`() = runTest {
        coEvery { userSettingsDao.getValue(KEY_REVIEW_COUNT_DATE) } returns today
        coEvery { userSettingsDao.getValue(KEY_TODAY_REVIEW_COUNT) } returns "12"

        val result = repository.getRemainingReviews(isPremium = false)

        assertEquals(0, result)
    }

    // ==================== incrementReviewCount() ====================

    @Test
    fun `incrementReviewCount - new day resets count to 1`() = runTest {
        // Saved date is yesterday (different from today)
        coEvery { userSettingsDao.getValue(KEY_REVIEW_COUNT_DATE) } returns "2020-01-01"

        repository.incrementReviewCount()

        coVerify {
            userSettingsDao.insert(UserSettings(KEY_REVIEW_COUNT_DATE, today))
        }
        coVerify {
            userSettingsDao.insert(UserSettings(KEY_TODAY_REVIEW_COUNT, "1"))
        }
    }

    @Test
    fun `incrementReviewCount - same day increments by 1`() = runTest {
        coEvery { userSettingsDao.getValue(KEY_REVIEW_COUNT_DATE) } returns today
        coEvery { userSettingsDao.getValue(KEY_TODAY_REVIEW_COUNT) } returns "7"

        repository.incrementReviewCount()

        coVerify {
            userSettingsDao.insert(UserSettings(KEY_TODAY_REVIEW_COUNT, "8"))
        }
    }

    // ==================== getTodayReviewCount() ====================

    @Test
    fun `getTodayReviewCount - new day returns 0 and resets`() = runTest {
        // Saved date is a past date (not today)
        coEvery { userSettingsDao.getValue(KEY_REVIEW_COUNT_DATE) } returns "2020-01-01"

        val result = repository.getTodayReviewCount()

        assertEquals(0, result)
        // Should reset the date and count
        coVerify {
            userSettingsDao.insert(UserSettings(KEY_REVIEW_COUNT_DATE, today))
        }
        coVerify {
            userSettingsDao.insert(UserSettings(KEY_TODAY_REVIEW_COUNT, "0"))
        }
    }

    @Test
    fun `getTodayReviewCount - same day returns stored count`() = runTest {
        coEvery { userSettingsDao.getValue(KEY_REVIEW_COUNT_DATE) } returns today
        coEvery { userSettingsDao.getValue(KEY_TODAY_REVIEW_COUNT) } returns "3"

        val result = repository.getTodayReviewCount()

        assertEquals(3, result)
    }
}
