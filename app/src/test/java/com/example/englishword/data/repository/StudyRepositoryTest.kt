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

    @Test
    fun `startSession - creates session and returns valid ID`() = runTest {
        val levelId = 1L
        val expectedSessionId = 42L
        val sessionSlot = slot<StudySession>()
        coEvery { studySessionDao.insert(capture(sessionSlot)) } returns expectedSessionId

        val result = repository.startSession(levelId)

        assertEquals(expectedSessionId, result)
        assertEquals(levelId, sessionSlot.captured.levelId)
        assertTrue(sessionSlot.captured.startedAt > 0)
        coVerify(exactly = 1) { studySessionDao.insert(any()) }
    }

    @Test
    fun `startSession - returns -1 on DAO error`() = runTest {
        coEvery { studySessionDao.insert(any()) } throws RuntimeException("DB error")

        val result = repository.startSession(1L)

        assertEquals(-1L, result)
    }

    @Test
    fun `completeSession - updates daily stats when stats already exist for today`() = runTest {
        val sessionId = 10L
        val wordCount = 8
        coEvery { studySessionDao.completeSession(any(), any(), any(), any()) } returns Unit
        coEvery { userStatsDao.getStatsByDateSync(any()) } returns UserStats(
            id = 1L,
            date = "2026-02-07",
            studiedCount = 5,
            streak = 2
        )

        val result = repository.completeSession(sessionId, wordCount, 3)

        assertTrue(result)
        coVerify(exactly = 1) { userStatsDao.incrementStudiedCount(any(), wordCount) }
        coVerify(exactly = 0) { userStatsDao.insert(any()) }
    }

    @Test
    fun `completeSession - creates new daily stats and increments streak when consecutive`() = runTest {
        val sessionId = 10L
        val wordCount = 12
        coEvery { studySessionDao.completeSession(any(), any(), any(), any()) } returns Unit
        coEvery { userStatsDao.getStatsByDateSync(any()) } returns null
        coEvery { userStatsDao.getLatestStatsSync() } returns UserStats(
            id = 1L,
            date = java.time.LocalDate.now().minusDays(1).toString(),
            studiedCount = 10,
            streak = 3
        )
        coEvery { userStatsDao.insert(any()) } returns 1L

        val result = repository.completeSession(sessionId, wordCount, 5)

        assertTrue(result)
        val statsSlot = slot<UserStats>()
        coVerify(exactly = 1) { userStatsDao.insert(capture(statsSlot)) }
        assertEquals(wordCount, statsSlot.captured.studiedCount)
        assertEquals(4, statsSlot.captured.streak)
    }

    @Test
    fun `completeSession - resets streak to 1 when latest stats are not consecutive`() = runTest {
        val sessionId = 10L
        val wordCount = 12
        coEvery { studySessionDao.completeSession(any(), any(), any(), any()) } returns Unit
        coEvery { userStatsDao.getStatsByDateSync(any()) } returns null
        coEvery { userStatsDao.getLatestStatsSync() } returns UserStats(
            id = 1L,
            date = java.time.LocalDate.now().minusDays(3).toString(),
            studiedCount = 10,
            streak = 7
        )
        coEvery { userStatsDao.insert(any()) } returns 1L

        val result = repository.completeSession(sessionId, wordCount, 5)

        assertTrue(result)
        val statsSlot = slot<UserStats>()
        coVerify(exactly = 1) { userStatsDao.insert(capture(statsSlot)) }
        assertEquals(1, statsSlot.captured.streak)
    }

    @Test
    fun `completeSession - returns false on error`() = runTest {
        coEvery { studySessionDao.completeSession(any(), any(), any(), any()) } throws RuntimeException("DB error")

        val result = repository.completeSession(10L, 5, 3)

        assertFalse(result)
    }

    @Test
    fun `recordResult - creates StudyRecord correctly and updates mastery in transaction`() = runTest {
        val sessionId = 5L
        val wordId = 100L
        val result = 2
        val responseTimeMs = 1500L
        val word = Word(
            id = wordId,
            levelId = 1L,
            english = "hello",
            japanese = "hello-ja",
            masteryLevel = 3
        )

        val recordSlot = slot<StudyRecord>()
        coEvery { wordDao.getWordByIdSync(wordId) } returns word
        coEvery {
            studyRecordDao.insertRecordAndUpdateMastery(
                record = capture(recordSlot),
                wordId = wordId,
                masteryLevel = 4,
                nextReviewAt = any()
            )
        } returns Unit

        val success = repository.recordResult(sessionId, wordId, result, responseTimeMs)

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
    fun `recordResult - returns false on transaction error`() = runTest {
        val wordId = 1L
        val word = Word(
            id = wordId,
            levelId = 1L,
            english = "error",
            japanese = "error-ja",
            masteryLevel = 1
        )

        coEvery { wordDao.getWordByIdSync(wordId) } returns word
        coEvery {
            studyRecordDao.insertRecordAndUpdateMastery(
                record = any(),
                wordId = wordId,
                masteryLevel = any(),
                nextReviewAt = any()
            )
        } throws RuntimeException("Transaction failed")

        val success = repository.recordResult(1L, wordId, 2)

        assertFalse(success)
    }

    @Test
    fun `recordResult - returns false when word not found`() = runTest {
        val wordId = 999L
        coEvery { wordDao.getWordByIdSync(wordId) } returns null

        val success = repository.recordResult(1L, wordId, 2)

        assertFalse(success)
        coVerify(exactly = 0) { studyRecordDao.insertRecordAndUpdateMastery(any(), any(), any(), any()) }
    }

    @Test
    fun `updateWordMastery - does not exceed MAX_LEVEL for KNOWN`() = runTest {
        val wordId = 10L
        val word = Word(
            id = wordId,
            levelId = 1L,
            english = "mastered",
            japanese = "mastered-ja",
            masteryLevel = SrsCalculator.MAX_LEVEL
        )

        coEvery { wordDao.getWordByIdSync(wordId) } returns word
        coEvery { studyRecordDao.insertRecordAndUpdateMastery(any(), wordId, SrsCalculator.MAX_LEVEL, any()) } returns Unit

        repository.recordResult(1L, wordId, 2)

        coVerify(exactly = 1) {
            studyRecordDao.insertRecordAndUpdateMastery(
                record = any(),
                wordId = wordId,
                masteryLevel = SrsCalculator.MAX_LEVEL,
                nextReviewAt = any()
            )
        }
    }

    @Test
    fun `updateWordMastery - does not go below MIN_LEVEL for AGAIN`() = runTest {
        val wordId = 10L
        val word = Word(
            id = wordId,
            levelId = 1L,
            english = "new",
            japanese = "new-ja",
            masteryLevel = SrsCalculator.MIN_LEVEL
        )

        coEvery { wordDao.getWordByIdSync(wordId) } returns word
        coEvery { studyRecordDao.insertRecordAndUpdateMastery(any(), wordId, SrsCalculator.MIN_LEVEL, any()) } returns Unit

        repository.recordResult(1L, wordId, 0)

        coVerify(exactly = 1) {
            studyRecordDao.insertRecordAndUpdateMastery(
                record = any(),
                wordId = wordId,
                masteryLevel = SrsCalculator.MIN_LEVEL,
                nextReviewAt = any()
            )
        }
    }

    @Test
    fun `updateWordMastery - keeps same level for LATER`() = runTest {
        val wordId = 10L
        val currentLevel = 3
        val word = Word(
            id = wordId,
            levelId = 1L,
            english = "later",
            japanese = "later-ja",
            masteryLevel = currentLevel
        )

        coEvery { wordDao.getWordByIdSync(wordId) } returns word
        coEvery { studyRecordDao.insertRecordAndUpdateMastery(any(), wordId, currentLevel, any()) } returns Unit

        repository.recordResult(1L, wordId, 1)

        coVerify(exactly = 1) {
            studyRecordDao.insertRecordAndUpdateMastery(
                record = any(),
                wordId = wordId,
                masteryLevel = currentLevel,
                nextReviewAt = any()
            )
        }
    }

    @Test
    fun `getCurrentStreak - returns streak from latest stats`() = runTest {
        val expectedStreak = 7
        coEvery { userStatsDao.getCurrentStreak() } returns expectedStreak

        val streak = repository.getCurrentStreak()

        assertEquals(expectedStreak, streak)
    }

    @Test
    fun `getCurrentStreak - returns 0 when no stats exist`() = runTest {
        coEvery { userStatsDao.getCurrentStreak() } returns null

        val streak = repository.getCurrentStreak()

        assertEquals(0, streak)
    }

    @Test
    fun `getCurrentStreak - returns 0 on error`() = runTest {
        coEvery { userStatsDao.getCurrentStreak() } throws RuntimeException("DB error")

        val streak = repository.getCurrentStreak()

        assertEquals(0, streak)
    }

    @Test
    fun `getIncompleteSessionForLevel - returns incomplete session when exists`() = runTest {
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

        val session = repository.getIncompleteSessionForLevel(levelId)

        assertNotNull(session)
        assertEquals(expectedSession.id, session!!.id)
        assertEquals(levelId, session.levelId)
        assertNull(session.completedAt)
        assertEquals("1,2,3,4,5", session.wordIds)
        assertEquals(2, session.currentIndex)
    }

    @Test
    fun `getIncompleteSessionForLevel - returns null when no incomplete session`() = runTest {
        val levelId = 3L
        coEvery { studySessionDao.getIncompleteSessionForLevel(levelId) } returns null

        val session = repository.getIncompleteSessionForLevel(levelId)

        assertNull(session)
    }

    @Test
    fun `getIncompleteSessionForLevel - returns null on error`() = runTest {
        coEvery { studySessionDao.getIncompleteSessionForLevel(any()) } throws RuntimeException("DB error")

        val session = repository.getIncompleteSessionForLevel(1L)

        assertNull(session)
    }
}
