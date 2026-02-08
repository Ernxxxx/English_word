package com.example.englishword.data.repository

import android.util.Log
import com.example.englishword.data.local.dao.StudyRecordDao
import com.example.englishword.data.local.dao.StudySessionDao
import com.example.englishword.data.local.dao.UserStatsDao
import com.example.englishword.data.local.dao.WordDao
import com.example.englishword.data.local.entity.StudyRecord
import com.example.englishword.data.local.entity.StudySession
import com.example.englishword.data.local.entity.UserStats
import com.example.englishword.data.local.entity.Word
import com.example.englishword.util.SrsCalculator
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class StudyRepositoryTest {

    private lateinit var studySessionDao: StudySessionDao
    private lateinit var studyRecordDao: StudyRecordDao
    private lateinit var userStatsDao: UserStatsDao
    private lateinit var wordDao: WordDao
    private lateinit var repository: StudyRepository

    @Before
    fun setUp() {
        // Mock android.util.Log to prevent crashes in unit tests
        mockkStatic(Log::class)
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>(), any()) } returns 0
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.v(any(), any()) } returns 0

        studySessionDao = mockk(relaxed = true)
        studyRecordDao = mockk(relaxed = true)
        userStatsDao = mockk(relaxed = true)
        wordDao = mockk(relaxed = true)

        repository = StudyRepository(
            studySessionDao = studySessionDao,
            studyRecordDao = studyRecordDao,
            userStatsDao = userStatsDao,
            wordDao = wordDao
        )
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    // ==================== startSession ====================

    @Test
    fun `startSession - creates session and returns valid ID`() = runTest {
        // Arrange
        val levelId = 1L
        val expectedSessionId = 42L
        val sessionSlot = slot<StudySession>()
        coEvery { studySessionDao.insert(capture(sessionSlot)) } returns expectedSessionId

        // Act
        val result = repository.startSession(levelId)

        // Assert
        assertEquals(expectedSessionId, result)
        assertEquals(levelId, sessionSlot.captured.levelId)
        assertTrue(sessionSlot.captured.startedAt > 0)
        coVerify(exactly = 1) { studySessionDao.insert(any()) }
    }

    @Test
    fun `startSession - returns -1 on DAO error`() = runTest {
        // Arrange
        coEvery { studySessionDao.insert(any()) } throws RuntimeException("DB error")

        // Act
        val result = repository.startSession(1L)

        // Assert
        assertEquals(-1L, result)
    }

    // ==================== completeSession ====================

    @Test
    fun `completeSession - updates session with correct word count and mastered count`() = runTest {
        // Arrange
        val sessionId = 10L
        val wordCount = 20
        val masteredCount = 15
        coEvery {
            studySessionDao.completeSession(
                sessionId = sessionId,
                completedAt = any(),
                wordCount = wordCount,
                masteredCount = masteredCount
            )
        } returns Unit
        // updateDailyStats path: stats already exist for today
        coEvery { userStatsDao.getStatsByDateSync(any()) } returns UserStats(
            id = 1L,
            date = "2026-02-07",
            studiedCount = 5,
            streak = 3
        )
        coEvery { userStatsDao.incrementStudiedCount(any(), any()) } returns Unit

        // Act
        val result = repository.completeSession(sessionId, wordCount, masteredCount)

        // Assert
        assertTrue(result)
        coVerify(exactly = 1) {
            studySessionDao.completeSession(
                sessionId = sessionId,
                completedAt = any(),
                wordCount = wordCount,
                masteredCount = masteredCount
            )
        }
    }

    @Test
    fun `completeSession - updates daily stats when stats already exist for today`() = runTest {
        // Arrange
        val sessionId = 10L
        val wordCount = 8
        coEvery {
            studySessionDao.completeSession(any(), any(), any(), any())
        } returns Unit
        coEvery { userStatsDao.getStatsByDateSync(any()) } returns UserStats(
            id = 1L,
            date = "2026-02-07",
            studiedCount = 5,
            streak = 2
        )

        // Act
        val result = repository.completeSession(sessionId, wordCount, 3)

        // Assert
        assertTrue(result)
        coVerify(exactly = 1) { userStatsDao.incrementStudiedCount(any(), wordCount) }
        coVerify(exactly = 0) { userStatsDao.insert(any()) }
    }

    @Test
    fun `completeSession - creates new daily stats when none exist for today`() = runTest {
        // Arrange
        val sessionId = 10L
        val wordCount = 12
        coEvery {
            studySessionDao.completeSession(any(), any(), any(), any())
        } returns Unit
        // No stats exist for today
        coEvery { userStatsDao.getStatsByDateSync(any()) } returns null
        coEvery { userStatsDao.insert(any()) } returns 1L

        // Act
        val result = repository.completeSession(sessionId, wordCount, 5)

        // Assert
        assertTrue(result)
        val statsSlot = slot<UserStats>()
        coVerify(exactly = 1) { userStatsDao.insert(capture(statsSlot)) }
        assertEquals(wordCount, statsSlot.captured.studiedCount)
    }

    @Test
    fun `completeSession - returns false on error`() = runTest {
        // Arrange
        coEvery {
            studySessionDao.completeSession(any(), any(), any(), any())
        } throws RuntimeException("DB error")

        // Act
        val result = repository.completeSession(10L, 5, 3)

        // Assert
        assertFalse(result)
    }

    // ==================== recordResult ====================

    @Test
    fun `recordResult - creates StudyRecord correctly and updates mastery`() = runTest {
        // Arrange
        val sessionId = 5L
        val wordId = 100L
        val result = 2 // KNOWN
        val responseTimeMs = 1500L
        val word = Word(
            id = wordId,
            levelId = 1L,
            english = "hello",
            japanese = "こんにちは",
            masteryLevel = 3
        )

        val recordSlot = slot<StudyRecord>()
        coEvery { studyRecordDao.insert(capture(recordSlot)) } returns 1L
        coEvery { wordDao.getWordByIdSync(wordId) } returns word
        coEvery { wordDao.updateMastery(any(), any(), any(), any()) } returns Unit

        // Act
        val success = repository.recordResult(sessionId, wordId, result, responseTimeMs)

        // Assert
        assertTrue(success)
        with(recordSlot.captured) {
            assertEquals(sessionId, this.sessionId)
            assertEquals(wordId, this.wordId)
            assertEquals(result, this.result)
            assertEquals(responseTimeMs, this.responseTimeMs)
            assertTrue(this.reviewedAt > 0)
        }
    }

    @Test
    fun `recordResult - returns false on error`() = runTest {
        // Arrange
        coEvery { studyRecordDao.insert(any()) } throws RuntimeException("Insert failed")

        // Act
        val success = repository.recordResult(1L, 1L, 2)

        // Assert
        assertFalse(success)
    }

    @Test
    fun `recordResult - calls updateWordMastery after recording`() = runTest {
        // Arrange
        val wordId = 50L
        val resultValue = 0 // AGAIN
        val word = Word(
            id = wordId,
            levelId = 1L,
            english = "difficult",
            japanese = "難しい",
            masteryLevel = 2
        )

        coEvery { studyRecordDao.insert(any()) } returns 1L
        coEvery { wordDao.getWordByIdSync(wordId) } returns word
        coEvery { wordDao.updateMastery(any(), any(), any(), any()) } returns Unit

        // Act
        repository.recordResult(1L, wordId, resultValue)

        // Assert: mastery should decrease by 1 for AGAIN (from 2 to 1)
        coVerify(exactly = 1) {
            wordDao.updateMastery(
                wordId = wordId,
                masteryLevel = 1, // SRS: currentLevel(2) - 1 for AGAIN
                nextReviewAt = any(),
                updatedAt = any()
            )
        }
    }

    // ==================== updateWordMastery (tested indirectly via recordResult) ====================

    @Test
    fun `updateWordMastery - calls wordDao updateMastery with SRS-calculated values for KNOWN`() = runTest {
        // Arrange
        val wordId = 10L
        val word = Word(
            id = wordId,
            levelId = 1L,
            english = "apple",
            japanese = "りんご",
            masteryLevel = 2
        )

        coEvery { studyRecordDao.insert(any()) } returns 1L
        coEvery { wordDao.getWordByIdSync(wordId) } returns word
        coEvery { wordDao.updateMastery(any(), any(), any(), any()) } returns Unit

        // Act: record KNOWN result (value = 2)
        val success = repository.recordResult(1L, wordId, 2)

        // Assert: mastery should increase from 2 to 3 for KNOWN
        assertTrue(success)
        coVerify(exactly = 1) {
            wordDao.updateMastery(
                wordId = wordId,
                masteryLevel = 3, // SRS: currentLevel(2) + 1 for KNOWN
                nextReviewAt = any(),
                updatedAt = any()
            )
        }
    }

    @Test
    fun `updateWordMastery - does not exceed MAX_LEVEL for KNOWN`() = runTest {
        // Arrange
        val wordId = 10L
        val word = Word(
            id = wordId,
            levelId = 1L,
            english = "mastered",
            japanese = "マスター済",
            masteryLevel = SrsCalculator.MAX_LEVEL // already at max (5)
        )

        coEvery { studyRecordDao.insert(any()) } returns 1L
        coEvery { wordDao.getWordByIdSync(wordId) } returns word
        coEvery { wordDao.updateMastery(any(), any(), any(), any()) } returns Unit

        // Act
        repository.recordResult(1L, wordId, 2) // KNOWN

        // Assert: should stay at MAX_LEVEL
        coVerify(exactly = 1) {
            wordDao.updateMastery(
                wordId = wordId,
                masteryLevel = SrsCalculator.MAX_LEVEL,
                nextReviewAt = any(),
                updatedAt = any()
            )
        }
    }

    @Test
    fun `updateWordMastery - does not go below MIN_LEVEL for AGAIN`() = runTest {
        // Arrange
        val wordId = 10L
        val word = Word(
            id = wordId,
            levelId = 1L,
            english = "new",
            japanese = "新しい",
            masteryLevel = SrsCalculator.MIN_LEVEL // already at min (0)
        )

        coEvery { studyRecordDao.insert(any()) } returns 1L
        coEvery { wordDao.getWordByIdSync(wordId) } returns word
        coEvery { wordDao.updateMastery(any(), any(), any(), any()) } returns Unit

        // Act
        repository.recordResult(1L, wordId, 0) // AGAIN

        // Assert: should stay at MIN_LEVEL
        coVerify(exactly = 1) {
            wordDao.updateMastery(
                wordId = wordId,
                masteryLevel = SrsCalculator.MIN_LEVEL,
                nextReviewAt = any(),
                updatedAt = any()
            )
        }
    }

    @Test
    fun `updateWordMastery - keeps same level for LATER`() = runTest {
        // Arrange
        val wordId = 10L
        val currentLevel = 3
        val word = Word(
            id = wordId,
            levelId = 1L,
            english = "later",
            japanese = "後で",
            masteryLevel = currentLevel
        )

        coEvery { studyRecordDao.insert(any()) } returns 1L
        coEvery { wordDao.getWordByIdSync(wordId) } returns word
        coEvery { wordDao.updateMastery(any(), any(), any(), any()) } returns Unit

        // Act
        repository.recordResult(1L, wordId, 1) // LATER

        // Assert: level should remain unchanged
        coVerify(exactly = 1) {
            wordDao.updateMastery(
                wordId = wordId,
                masteryLevel = currentLevel,
                nextReviewAt = any(),
                updatedAt = any()
            )
        }
    }

    @Test
    fun `updateWordMastery - skips update when word not found`() = runTest {
        // Arrange
        val wordId = 999L

        coEvery { studyRecordDao.insert(any()) } returns 1L
        coEvery { wordDao.getWordByIdSync(wordId) } returns null

        // Act
        val success = repository.recordResult(1L, wordId, 2)

        // Assert: record inserted successfully, but mastery update skipped
        assertTrue(success)
        coVerify(exactly = 0) { wordDao.updateMastery(any(), any(), any(), any()) }
    }

    // ==================== getCurrentStreak ====================

    @Test
    fun `getCurrentStreak - returns streak from latest stats`() = runTest {
        // Arrange
        val expectedStreak = 7
        coEvery { userStatsDao.getCurrentStreak() } returns expectedStreak

        // Act
        val streak = repository.getCurrentStreak()

        // Assert
        assertEquals(expectedStreak, streak)
    }

    @Test
    fun `getCurrentStreak - returns 0 when no stats exist`() = runTest {
        // Arrange
        coEvery { userStatsDao.getCurrentStreak() } returns null

        // Act
        val streak = repository.getCurrentStreak()

        // Assert
        assertEquals(0, streak)
    }

    @Test
    fun `getCurrentStreak - returns 0 on error`() = runTest {
        // Arrange
        coEvery { userStatsDao.getCurrentStreak() } throws RuntimeException("DB error")

        // Act
        val streak = repository.getCurrentStreak()

        // Assert
        assertEquals(0, streak)
    }

    // ==================== getIncompleteSessionForLevel ====================

    @Test
    fun `getIncompleteSessionForLevel - returns incomplete session when exists`() = runTest {
        // Arrange
        val levelId = 3L
        val expectedSession = StudySession(
            id = 42L,
            levelId = levelId,
            startedAt = System.currentTimeMillis() - 60_000,
            completedAt = null,
            wordIds = "1,2,3,4,5",
            currentIndex = 2,
            knownCount = 1,
            againCount = 1,
            laterCount = 0
        )
        coEvery { studySessionDao.getIncompleteSessionForLevel(levelId) } returns expectedSession

        // Act
        val session = repository.getIncompleteSessionForLevel(levelId)

        // Assert
        assertNotNull(session)
        assertEquals(expectedSession.id, session!!.id)
        assertEquals(levelId, session.levelId)
        assertNull(session.completedAt)
        assertEquals("1,2,3,4,5", session.wordIds)
        assertEquals(2, session.currentIndex)
    }

    @Test
    fun `getIncompleteSessionForLevel - returns null when no incomplete session`() = runTest {
        // Arrange
        val levelId = 3L
        coEvery { studySessionDao.getIncompleteSessionForLevel(levelId) } returns null

        // Act
        val session = repository.getIncompleteSessionForLevel(levelId)

        // Assert
        assertNull(session)
    }

    @Test
    fun `getIncompleteSessionForLevel - returns null on error`() = runTest {
        // Arrange
        coEvery {
            studySessionDao.getIncompleteSessionForLevel(any())
        } throws RuntimeException("DB error")

        // Act
        val session = repository.getIncompleteSessionForLevel(1L)

        // Assert
        assertNull(session)
    }
}
