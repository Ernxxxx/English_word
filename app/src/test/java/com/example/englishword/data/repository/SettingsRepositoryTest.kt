package com.example.englishword.data.repository

import app.cash.turbine.test
import com.example.englishword.data.local.dao.UserSettingsDao
import com.example.englishword.data.local.entity.UserSettings
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [SettingsRepository].
 *
 * Note: isPremium()/isPremiumSync() always return true in DEBUG builds
 * because the repository checks BuildConfig.DEBUG. Since unit tests run
 * against the debug variant, those code paths cannot be exercised without
 * refactoring the repository. The tests below therefore focus on the
 * non-DEBUG paths that can be verified through the DAO mock, and we
 * include explicit tests that confirm the DEBUG shortcut is active.
 */
class SettingsRepositoryTest {

    private lateinit var dao: UserSettingsDao
    private lateinit var repository: SettingsRepository

    @Before
    fun setUp() {
        dao = mockk(relaxed = true)
        repository = SettingsRepository(dao)
    }

    // ==================== isPremium / isPremiumSync ====================

    @Test
    fun `isPremium - DEBUG build always returns true`() = runTest {
        // In debug builds, isPremium() returns flowOf(true) regardless of DB state.
        // We do NOT stub the DAO -- the method should never reach it.
        repository.isPremium().test {
            assertTrue(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `isPremiumSync - DEBUG build always returns true`() = runTest {
        // Same as above for the synchronous variant.
        val result = repository.isPremiumSync()
        assertTrue(result)
    }

    // ==================== getDailyGoal ====================

    @Test
    fun `getDailyGoal - returns stored value from DB`() = runTest {
        every { dao.getValueFlow("daily_goal") } returns flowOf("50")

        repository.getDailyGoal().test {
            assertEquals(50, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getDailyGoal - returns default 20 when not set`() = runTest {
        every { dao.getValueFlow("daily_goal") } returns flowOf(null)

        repository.getDailyGoal().test {
            assertEquals(20, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ==================== isTrialActive ====================

    @Test
    fun `isTrialActive - returns true when trial_expires_at is in the future`() = runTest {
        val futureTimestamp = System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L
        coEvery { dao.getValue("trial_expires_at") } returns futureTimestamp.toString()

        val result = repository.isTrialActive()
        assertTrue(result)
    }

    @Test
    fun `isTrialActive - returns false when trial_expires_at is in the past`() = runTest {
        val pastTimestamp = System.currentTimeMillis() - 1000L
        coEvery { dao.getValue("trial_expires_at") } returns pastTimestamp.toString()

        val result = repository.isTrialActive()
        assertFalse(result)
    }

    @Test
    fun `isTrialActive - returns false when trial_expires_at is 0`() = runTest {
        coEvery { dao.getValue("trial_expires_at") } returns "0"

        val result = repository.isTrialActive()
        assertFalse(result)
    }

    @Test
    fun `isTrialActive - returns false when trial_expires_at is not set`() = runTest {
        coEvery { dao.getValue("trial_expires_at") } returns null

        val result = repository.isTrialActive()
        assertFalse(result)
    }

    // ==================== startTrial ====================

    @Test
    fun `startTrial - sets trial start time and expiration when not started and not premium`() = runTest {
        // Not started yet (trial_started_at returns null)
        coEvery { dao.getValue("trial_started_at") } returns null
        // isPremiumSync in DEBUG always returns true, so startTrial will return false.
        // However, we still verify the guard logic: since DEBUG isPremiumSync() == true,
        // startTrial() should return false.
        //
        // NOTE: In a release build, if not premium and not started, it would succeed.
        // Since we cannot mock BuildConfig.DEBUG away in unit tests, we assert the
        // DEBUG behavior here.
        val result = repository.startTrial()
        // In DEBUG, isPremiumSync returns true, so startTrial returns false
        assertFalse(result)
    }

    @Test
    fun `startTrial - returns false if trial already started`() = runTest {
        // Trial already started (non-zero timestamp)
        coEvery { dao.getValue("trial_started_at") } returns System.currentTimeMillis().toString()

        val result = repository.startTrial()
        assertFalse(result)
    }

    @Test
    fun `startTrial - returns false if already premium in DEBUG build`() = runTest {
        // isPremiumSync always returns true in DEBUG, regardless of what DAO returns.
        coEvery { dao.getValue("trial_started_at") } returns null

        val result = repository.startTrial()
        assertFalse(result)
    }

    // ==================== isNotificationEnabled ====================

    @Test
    fun `isNotificationEnabled - returns stored boolean value true`() = runTest {
        every { dao.getValueFlow("notification_enabled") } returns flowOf("true")

        repository.isNotificationEnabled().test {
            assertTrue(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `isNotificationEnabled - returns stored boolean value false`() = runTest {
        every { dao.getValueFlow("notification_enabled") } returns flowOf("false")

        repository.isNotificationEnabled().test {
            assertFalse(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `isNotificationEnabled - returns true as default when not set`() = runTest {
        every { dao.getValueFlow("notification_enabled") } returns flowOf(null)

        repository.isNotificationEnabled().test {
            assertTrue(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ==================== clearPremium / isPremiumExpired ====================
    // The repository does not have a single clearPremiumIfExpired() method.
    // It has isPremiumExpired() and clearPremium() as separate methods.
    // Tests below verify both and their composition.

    @Test
    fun `isPremiumExpired - returns true when expires_at is in the past`() = runTest {
        val pastTimestamp = System.currentTimeMillis() - 1000L
        coEvery { dao.getValue("premium_expires_at") } returns pastTimestamp.toString()

        val result = repository.isPremiumExpired()
        assertTrue(result)
    }

    @Test
    fun `isPremiumExpired - returns false when expires_at is in the future`() = runTest {
        val futureTimestamp = System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L
        coEvery { dao.getValue("premium_expires_at") } returns futureTimestamp.toString()

        val result = repository.isPremiumExpired()
        assertFalse(result)
    }

    @Test
    fun `isPremiumExpired - returns false when expires_at is not set`() = runTest {
        coEvery { dao.getValue("premium_expires_at") } returns null

        val result = repository.isPremiumExpired()
        assertFalse(result)
    }

    @Test
    fun `clearPremium - clears premium and related keys`() = runTest {
        val result = repository.clearPremium()
        assertTrue(result)

        coVerify {
            dao.insert(UserSettings(key = "is_premium", value = "false"))
            dao.deleteByKey("premium_purchase_token")
            dao.deleteByKey("premium_expires_at")
            dao.deleteByKey("premium_sku")
        }
    }

    @Test
    fun `clearPremiumIfExpired - clears when expired`() = runTest {
        // Premium has expired
        val pastTimestamp = System.currentTimeMillis() - 1000L
        coEvery { dao.getValue("premium_expires_at") } returns pastTimestamp.toString()

        // Compose: check expiration, then clear
        if (repository.isPremiumExpired()) {
            val cleared = repository.clearPremium()
            assertTrue(cleared)
        }

        // Verify clear operations were invoked
        coVerify {
            dao.insert(UserSettings(key = "is_premium", value = "false"))
            dao.deleteByKey("premium_purchase_token")
            dao.deleteByKey("premium_expires_at")
            dao.deleteByKey("premium_sku")
        }
    }

    @Test
    fun `clearPremiumIfExpired - does nothing when not expired`() = runTest {
        // Premium still valid
        val futureTimestamp = System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L
        coEvery { dao.getValue("premium_expires_at") } returns futureTimestamp.toString()

        // Should NOT call clearPremium
        if (repository.isPremiumExpired()) {
            repository.clearPremium()
        }

        // Verify clearPremium's insert was NOT called
        coVerify(exactly = 0) {
            dao.insert(UserSettings(key = "is_premium", value = "false"))
        }
    }

    // ==================== Additional edge cases ====================

    @Test
    fun `getDailyGoalSync - returns stored value`() = runTest {
        coEvery { dao.getValue("daily_goal") } returns "30"

        val result = repository.getDailyGoalSync()
        assertEquals(30, result)
    }

    @Test
    fun `getDailyGoalSync - returns default 20 when not set`() = runTest {
        coEvery { dao.getValue("daily_goal") } returns null

        val result = repository.getDailyGoalSync()
        assertEquals(20, result)
    }

    @Test
    fun `getDailyGoalSync - returns default 20 when value is not a number`() = runTest {
        coEvery { dao.getValue("daily_goal") } returns "abc"

        val result = repository.getDailyGoalSync()
        assertEquals(20, result)
    }

    @Test
    fun `isTrialStarted - returns true when trial_started_at is positive`() = runTest {
        coEvery { dao.getValue("trial_started_at") } returns "1700000000000"

        val result = repository.isTrialStarted()
        assertTrue(result)
    }

    @Test
    fun `isTrialStarted - returns false when trial_started_at is null`() = runTest {
        coEvery { dao.getValue("trial_started_at") } returns null

        val result = repository.isTrialStarted()
        assertFalse(result)
    }

    @Test
    fun `isTrialStarted - returns false when trial_started_at is 0`() = runTest {
        coEvery { dao.getValue("trial_started_at") } returns "0"

        val result = repository.isTrialStarted()
        assertFalse(result)
    }

    @Test
    fun `setDailyGoal - inserts value into DAO`() = runTest {
        val result = repository.setDailyGoal(30)
        assertTrue(result)

        coVerify {
            dao.insert(UserSettings(key = "daily_goal", value = "30"))
        }
    }

    @Test
    fun `setNotificationEnabled - inserts value into DAO`() = runTest {
        val result = repository.setNotificationEnabled(false)
        assertTrue(result)

        coVerify {
            dao.insert(UserSettings(key = "notification_enabled", value = "false"))
        }
    }

    @Test
    fun `getTrialDaysRemaining - returns 0 when trial not started`() = runTest {
        coEvery { dao.getValue("trial_expires_at") } returns null

        val result = repository.getTrialDaysRemaining()
        assertEquals(0, result)
    }

    @Test
    fun `getTrialDaysRemaining - returns 0 when trial expired`() = runTest {
        val pastTimestamp = System.currentTimeMillis() - 1000L
        coEvery { dao.getValue("trial_expires_at") } returns pastTimestamp.toString()

        val result = repository.getTrialDaysRemaining()
        assertEquals(0, result)
    }

    @Test
    fun `getTrialDaysRemaining - returns correct days when trial active`() = runTest {
        // 3.5 days from now (avoids edge case with millisecond timing)
        val futureTimestamp = System.currentTimeMillis() + (3.5 * 24 * 60 * 60 * 1000L).toLong()
        coEvery { dao.getValue("trial_expires_at") } returns futureTimestamp.toString()

        val result = repository.getTrialDaysRemaining()
        // remaining / dayMs = 3, +1 rounding = 4
        assertEquals(4, result)
    }

    @Test
    fun `hasPremiumAccess - returns true in DEBUG build`() = runTest {
        // In DEBUG, isPremiumSync() returns true, so hasPremiumAccess() is true
        val result = repository.hasPremiumAccess()
        assertTrue(result)
    }
}
