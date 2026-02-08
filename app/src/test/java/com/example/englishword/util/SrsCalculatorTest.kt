package com.example.englishword.util

import com.example.englishword.domain.model.ReviewResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SrsCalculatorTest {

    // --- Constants for readability ---
    private val TOLERANCE_MS = 1000L

    private val INTERVAL_IMMEDIATE = 0L
    private val INTERVAL_ONE_HOUR = 1L * 60 * 60 * 1000
    private val INTERVAL_EIGHT_HOURS = 8L * 60 * 60 * 1000
    private val INTERVAL_ONE_DAY = 24L * 60 * 60 * 1000
    private val INTERVAL_THREE_DAYS = 3L * 24 * 60 * 60 * 1000
    private val INTERVAL_SEVEN_DAYS = 7L * 24 * 60 * 60 * 1000

    private val expectedIntervalByLevel = mapOf(
        0 to INTERVAL_IMMEDIATE,
        1 to INTERVAL_ONE_HOUR,
        2 to INTERVAL_EIGHT_HOURS,
        3 to INTERVAL_ONE_DAY,
        4 to INTERVAL_THREE_DAYS,
        5 to INTERVAL_SEVEN_DAYS
    )

    // =========================================================================
    // 1. KNOWN result increases level by 1 (for each level 0-4)
    // =========================================================================

    @Test
    fun knownResult_atLevel0_increasesToLevel1() {
        val (newLevel, _) = SrsCalculator.calculateNextReview(0, ReviewResult.KNOWN.value)
        assertEquals(1, newLevel)
    }

    @Test
    fun knownResult_atLevel1_increasesToLevel2() {
        val (newLevel, _) = SrsCalculator.calculateNextReview(1, ReviewResult.KNOWN.value)
        assertEquals(2, newLevel)
    }

    @Test
    fun knownResult_atLevel2_increasesToLevel3() {
        val (newLevel, _) = SrsCalculator.calculateNextReview(2, ReviewResult.KNOWN.value)
        assertEquals(3, newLevel)
    }

    @Test
    fun knownResult_atLevel3_increasesToLevel4() {
        val (newLevel, _) = SrsCalculator.calculateNextReview(3, ReviewResult.KNOWN.value)
        assertEquals(4, newLevel)
    }

    @Test
    fun knownResult_atLevel4_increasesToLevel5() {
        val (newLevel, _) = SrsCalculator.calculateNextReview(4, ReviewResult.KNOWN.value)
        assertEquals(5, newLevel)
    }

    // =========================================================================
    // 2. KNOWN at MAX_LEVEL stays at MAX_LEVEL
    // =========================================================================

    @Test
    fun knownResult_atMaxLevel_staysAtMaxLevel() {
        val (newLevel, _) = SrsCalculator.calculateNextReview(SrsCalculator.MAX_LEVEL, ReviewResult.KNOWN.value)
        assertEquals(SrsCalculator.MAX_LEVEL, newLevel)
    }

    @Test
    fun knownResult_atLevel5_doesNotExceed5() {
        val (newLevel, _) = SrsCalculator.calculateNextReview(5, ReviewResult.KNOWN.value)
        assertEquals(5, newLevel)
    }

    // =========================================================================
    // 3. AGAIN result decreases level by 1 (for each level 1-5)
    // =========================================================================

    @Test
    fun againResult_atLevel1_decreasesToLevel0() {
        val (newLevel, _) = SrsCalculator.calculateNextReview(1, ReviewResult.AGAIN.value)
        assertEquals(0, newLevel)
    }

    @Test
    fun againResult_atLevel2_decreasesToLevel1() {
        val (newLevel, _) = SrsCalculator.calculateNextReview(2, ReviewResult.AGAIN.value)
        assertEquals(1, newLevel)
    }

    @Test
    fun againResult_atLevel3_decreasesToLevel2() {
        val (newLevel, _) = SrsCalculator.calculateNextReview(3, ReviewResult.AGAIN.value)
        assertEquals(2, newLevel)
    }

    @Test
    fun againResult_atLevel4_decreasesToLevel3() {
        val (newLevel, _) = SrsCalculator.calculateNextReview(4, ReviewResult.AGAIN.value)
        assertEquals(3, newLevel)
    }

    @Test
    fun againResult_atLevel5_decreasesToLevel4() {
        val (newLevel, _) = SrsCalculator.calculateNextReview(5, ReviewResult.AGAIN.value)
        assertEquals(4, newLevel)
    }

    // =========================================================================
    // 4. AGAIN at MIN_LEVEL stays at MIN_LEVEL
    // =========================================================================

    @Test
    fun againResult_atMinLevel_staysAtMinLevel() {
        val (newLevel, _) = SrsCalculator.calculateNextReview(SrsCalculator.MIN_LEVEL, ReviewResult.AGAIN.value)
        assertEquals(SrsCalculator.MIN_LEVEL, newLevel)
    }

    @Test
    fun againResult_atLevel0_doesNotGoBelowZero() {
        val (newLevel, _) = SrsCalculator.calculateNextReview(0, ReviewResult.AGAIN.value)
        assertEquals(0, newLevel)
    }

    // =========================================================================
    // 5. LATER result keeps level unchanged (for each level 0-5)
    // =========================================================================

    @Test
    fun laterResult_atLevel0_keepsLevel0() {
        val (newLevel, _) = SrsCalculator.calculateNextReview(0, ReviewResult.LATER.value)
        assertEquals(0, newLevel)
    }

    @Test
    fun laterResult_atLevel1_keepsLevel1() {
        val (newLevel, _) = SrsCalculator.calculateNextReview(1, ReviewResult.LATER.value)
        assertEquals(1, newLevel)
    }

    @Test
    fun laterResult_atLevel2_keepsLevel2() {
        val (newLevel, _) = SrsCalculator.calculateNextReview(2, ReviewResult.LATER.value)
        assertEquals(2, newLevel)
    }

    @Test
    fun laterResult_atLevel3_keepsLevel3() {
        val (newLevel, _) = SrsCalculator.calculateNextReview(3, ReviewResult.LATER.value)
        assertEquals(3, newLevel)
    }

    @Test
    fun laterResult_atLevel4_keepsLevel4() {
        val (newLevel, _) = SrsCalculator.calculateNextReview(4, ReviewResult.LATER.value)
        assertEquals(4, newLevel)
    }

    @Test
    fun laterResult_atLevel5_keepsLevel5() {
        val (newLevel, _) = SrsCalculator.calculateNextReview(5, ReviewResult.LATER.value)
        assertEquals(5, newLevel)
    }

    // =========================================================================
    // 6. Invalid result value keeps level unchanged
    // =========================================================================

    @Test
    fun invalidResult_negativeOne_keepsLevelUnchanged() {
        val (newLevel, _) = SrsCalculator.calculateNextReview(3, -1)
        assertEquals(3, newLevel)
    }

    @Test
    fun invalidResult_three_keepsLevelUnchanged() {
        val (newLevel, _) = SrsCalculator.calculateNextReview(3, 3)
        assertEquals(3, newLevel)
    }

    @Test
    fun invalidResult_largeValue_keepsLevelUnchanged() {
        val (newLevel, _) = SrsCalculator.calculateNextReview(2, 100)
        assertEquals(2, newLevel)
    }

    @Test
    fun invalidResult_negativeHundred_keepsLevelUnchanged() {
        val (newLevel, _) = SrsCalculator.calculateNextReview(4, -100)
        assertEquals(4, newLevel)
    }

    @Test
    fun invalidResult_atLevel0_keepsLevel0() {
        val (newLevel, _) = SrsCalculator.calculateNextReview(0, 99)
        assertEquals(0, newLevel)
    }

    @Test
    fun invalidResult_atLevel5_keepsLevel5() {
        val (newLevel, _) = SrsCalculator.calculateNextReview(5, -5)
        assertEquals(5, newLevel)
    }

    // =========================================================================
    // 7. Next review time is approximately correct for each level
    // =========================================================================

    @Test
    fun nextReviewTime_level0_isImmediate() {
        val before = System.currentTimeMillis()
        val (_, nextReviewTime) = SrsCalculator.calculateNextReview(0, ReviewResult.LATER.value)
        val after = System.currentTimeMillis()

        // Level 0 interval is IMMEDIATE (0ms), so nextReviewTime ~= currentTimeMillis
        assertTrue(
            "nextReviewTime should be >= before",
            nextReviewTime >= before
        )
        assertTrue(
            "nextReviewTime should be <= after + tolerance",
            nextReviewTime <= after + TOLERANCE_MS
        )
    }

    @Test
    fun nextReviewTime_level1_isOneHour() {
        val before = System.currentTimeMillis()
        val (_, nextReviewTime) = SrsCalculator.calculateNextReview(1, ReviewResult.LATER.value)
        val after = System.currentTimeMillis()

        val expectedMin = before + INTERVAL_ONE_HOUR
        val expectedMax = after + INTERVAL_ONE_HOUR + TOLERANCE_MS

        assertTrue(
            "nextReviewTime ($nextReviewTime) should be >= expected ($expectedMin)",
            nextReviewTime >= expectedMin
        )
        assertTrue(
            "nextReviewTime ($nextReviewTime) should be <= expected ($expectedMax)",
            nextReviewTime <= expectedMax
        )
    }

    @Test
    fun nextReviewTime_level2_isEightHours() {
        val before = System.currentTimeMillis()
        val (_, nextReviewTime) = SrsCalculator.calculateNextReview(2, ReviewResult.LATER.value)
        val after = System.currentTimeMillis()

        val expectedMin = before + INTERVAL_EIGHT_HOURS
        val expectedMax = after + INTERVAL_EIGHT_HOURS + TOLERANCE_MS

        assertTrue(nextReviewTime >= expectedMin)
        assertTrue(nextReviewTime <= expectedMax)
    }

    @Test
    fun nextReviewTime_level3_isOneDay() {
        val before = System.currentTimeMillis()
        val (_, nextReviewTime) = SrsCalculator.calculateNextReview(3, ReviewResult.LATER.value)
        val after = System.currentTimeMillis()

        val expectedMin = before + INTERVAL_ONE_DAY
        val expectedMax = after + INTERVAL_ONE_DAY + TOLERANCE_MS

        assertTrue(nextReviewTime >= expectedMin)
        assertTrue(nextReviewTime <= expectedMax)
    }

    @Test
    fun nextReviewTime_level4_isThreeDays() {
        val before = System.currentTimeMillis()
        val (_, nextReviewTime) = SrsCalculator.calculateNextReview(4, ReviewResult.LATER.value)
        val after = System.currentTimeMillis()

        val expectedMin = before + INTERVAL_THREE_DAYS
        val expectedMax = after + INTERVAL_THREE_DAYS + TOLERANCE_MS

        assertTrue(nextReviewTime >= expectedMin)
        assertTrue(nextReviewTime <= expectedMax)
    }

    @Test
    fun nextReviewTime_level5_isSevenDays() {
        val before = System.currentTimeMillis()
        val (_, nextReviewTime) = SrsCalculator.calculateNextReview(5, ReviewResult.LATER.value)
        val after = System.currentTimeMillis()

        val expectedMin = before + INTERVAL_SEVEN_DAYS
        val expectedMax = after + INTERVAL_SEVEN_DAYS + TOLERANCE_MS

        assertTrue(nextReviewTime >= expectedMin)
        assertTrue(nextReviewTime <= expectedMax)
    }

    @Test
    fun nextReviewTime_afterKnownResult_usesNewLevelInterval() {
        // KNOWN at level 2 -> new level 3, interval should be ONE_DAY
        val before = System.currentTimeMillis()
        val (newLevel, nextReviewTime) = SrsCalculator.calculateNextReview(2, ReviewResult.KNOWN.value)
        val after = System.currentTimeMillis()

        assertEquals(3, newLevel)

        val expectedInterval = expectedIntervalByLevel[3]!!
        val expectedMin = before + expectedInterval
        val expectedMax = after + expectedInterval + TOLERANCE_MS

        assertTrue(nextReviewTime >= expectedMin)
        assertTrue(nextReviewTime <= expectedMax)
    }

    @Test
    fun nextReviewTime_afterAgainResult_usesNewLevelInterval() {
        // AGAIN at level 3 -> new level 2, interval should be EIGHT_HOURS
        val before = System.currentTimeMillis()
        val (newLevel, nextReviewTime) = SrsCalculator.calculateNextReview(3, ReviewResult.AGAIN.value)
        val after = System.currentTimeMillis()

        assertEquals(2, newLevel)

        val expectedInterval = expectedIntervalByLevel[2]!!
        val expectedMin = before + expectedInterval
        val expectedMax = after + expectedInterval + TOLERANCE_MS

        assertTrue(nextReviewTime >= expectedMin)
        assertTrue(nextReviewTime <= expectedMax)
    }

    @Test
    fun nextReviewTime_allLevelsWithKnown_useCorrectIntervals() {
        // Verify that KNOWN result from level N produces interval for level N+1
        for (level in 0..4) {
            val before = System.currentTimeMillis()
            val (newLevel, nextReviewTime) = SrsCalculator.calculateNextReview(level, ReviewResult.KNOWN.value)
            val after = System.currentTimeMillis()

            val expectedNewLevel = level + 1
            assertEquals("KNOWN at level $level should produce level $expectedNewLevel", expectedNewLevel, newLevel)

            val expectedInterval = expectedIntervalByLevel[expectedNewLevel]!!
            val expectedMin = before + expectedInterval
            val expectedMax = after + expectedInterval + TOLERANCE_MS

            assertTrue(
                "nextReviewTime at new level $expectedNewLevel should be >= expected",
                nextReviewTime >= expectedMin
            )
            assertTrue(
                "nextReviewTime at new level $expectedNewLevel should be <= expected",
                nextReviewTime <= expectedMax
            )
        }
    }

    // =========================================================================
    // 8. getIntervalDescription returns correct strings
    // =========================================================================

    @Test
    fun getIntervalDescription_level0_returnsImmediate() {
        assertEquals("Immediate", SrsCalculator.getIntervalDescription(0))
    }

    @Test
    fun getIntervalDescription_level1_returns1Hour() {
        assertEquals("1 hour", SrsCalculator.getIntervalDescription(1))
    }

    @Test
    fun getIntervalDescription_level2_returns8Hours() {
        assertEquals("8 hours", SrsCalculator.getIntervalDescription(2))
    }

    @Test
    fun getIntervalDescription_level3_returns1Day() {
        assertEquals("1 day", SrsCalculator.getIntervalDescription(3))
    }

    @Test
    fun getIntervalDescription_level4_returns3Days() {
        assertEquals("3 days", SrsCalculator.getIntervalDescription(4))
    }

    @Test
    fun getIntervalDescription_level5_returns7Days() {
        assertEquals("7 days", SrsCalculator.getIntervalDescription(5))
    }

    @Test
    fun getIntervalDescription_negativeLevel_returnsUnknown() {
        assertEquals("Unknown", SrsCalculator.getIntervalDescription(-1))
    }

    @Test
    fun getIntervalDescription_level6_returnsUnknown() {
        assertEquals("Unknown", SrsCalculator.getIntervalDescription(6))
    }

    @Test
    fun getIntervalDescription_level100_returnsUnknown() {
        assertEquals("Unknown", SrsCalculator.getIntervalDescription(100))
    }

    @Test
    fun getIntervalDescription_largeNegative_returnsUnknown() {
        assertEquals("Unknown", SrsCalculator.getIntervalDescription(-999))
    }

    // =========================================================================
    // 9. isMastered returns true for level 5+, false otherwise
    // =========================================================================

    @Test
    fun isMastered_level0_returnsFalse() {
        assertFalse(SrsCalculator.isMastered(0))
    }

    @Test
    fun isMastered_level1_returnsFalse() {
        assertFalse(SrsCalculator.isMastered(1))
    }

    @Test
    fun isMastered_level2_returnsFalse() {
        assertFalse(SrsCalculator.isMastered(2))
    }

    @Test
    fun isMastered_level3_returnsFalse() {
        assertFalse(SrsCalculator.isMastered(3))
    }

    @Test
    fun isMastered_level4_returnsFalse() {
        assertFalse(SrsCalculator.isMastered(4))
    }

    @Test
    fun isMastered_level5_returnsTrue() {
        assertTrue(SrsCalculator.isMastered(5))
    }

    @Test
    fun isMastered_level6_returnsTrue() {
        assertTrue(SrsCalculator.isMastered(6))
    }

    @Test
    fun isMastered_level100_returnsTrue() {
        assertTrue(SrsCalculator.isMastered(100))
    }

    @Test
    fun isMastered_negativeLevel_returnsFalse() {
        assertFalse(SrsCalculator.isMastered(-1))
    }

    @Test
    fun isMastered_exactlyMaxLevel_returnsTrue() {
        assertTrue(SrsCalculator.isMastered(SrsCalculator.MAX_LEVEL))
    }

    @Test
    fun isMastered_oneBelowMaxLevel_returnsFalse() {
        assertFalse(SrsCalculator.isMastered(SrsCalculator.MAX_LEVEL - 1))
    }

    // =========================================================================
    // 10. calculateNextReview with ReviewResult enum delegates correctly
    // =========================================================================

    @Test
    fun enumOverload_knownResult_matchesIntOverload() {
        val before = System.currentTimeMillis()
        val enumResult = SrsCalculator.calculateNextReview(2, ReviewResult.KNOWN)
        val intResult = SrsCalculator.calculateNextReview(2, ReviewResult.KNOWN.value)
        val after = System.currentTimeMillis()

        assertEquals(
            "Enum and int overloads should produce the same level",
            intResult.first, enumResult.first
        )

        // Both should be within tolerance of each other (called nearly simultaneously)
        val timeDiff = kotlin.math.abs(enumResult.second - intResult.second)
        assertTrue(
            "Time difference between overloads should be within tolerance ($timeDiff ms)",
            timeDiff <= TOLERANCE_MS
        )
    }

    @Test
    fun enumOverload_againResult_matchesIntOverload() {
        val enumResult = SrsCalculator.calculateNextReview(3, ReviewResult.AGAIN)
        val intResult = SrsCalculator.calculateNextReview(3, ReviewResult.AGAIN.value)

        assertEquals(intResult.first, enumResult.first)

        val timeDiff = kotlin.math.abs(enumResult.second - intResult.second)
        assertTrue(timeDiff <= TOLERANCE_MS)
    }

    @Test
    fun enumOverload_laterResult_matchesIntOverload() {
        val enumResult = SrsCalculator.calculateNextReview(4, ReviewResult.LATER)
        val intResult = SrsCalculator.calculateNextReview(4, ReviewResult.LATER.value)

        assertEquals(intResult.first, enumResult.first)

        val timeDiff = kotlin.math.abs(enumResult.second - intResult.second)
        assertTrue(timeDiff <= TOLERANCE_MS)
    }

    @Test
    fun enumOverload_knownAtLevel0_producesLevel1() {
        val (newLevel, _) = SrsCalculator.calculateNextReview(0, ReviewResult.KNOWN)
        assertEquals(1, newLevel)
    }

    @Test
    fun enumOverload_againAtLevel0_staysAtLevel0() {
        val (newLevel, _) = SrsCalculator.calculateNextReview(0, ReviewResult.AGAIN)
        assertEquals(0, newLevel)
    }

    @Test
    fun enumOverload_laterAtLevel3_staysAtLevel3() {
        val (newLevel, _) = SrsCalculator.calculateNextReview(3, ReviewResult.LATER)
        assertEquals(3, newLevel)
    }

    @Test
    fun enumOverload_knownAtMaxLevel_staysAtMaxLevel() {
        val (newLevel, _) = SrsCalculator.calculateNextReview(SrsCalculator.MAX_LEVEL, ReviewResult.KNOWN)
        assertEquals(SrsCalculator.MAX_LEVEL, newLevel)
    }

    @Test
    fun enumOverload_againAtMinLevel_staysAtMinLevel() {
        val (newLevel, _) = SrsCalculator.calculateNextReview(SrsCalculator.MIN_LEVEL, ReviewResult.AGAIN)
        assertEquals(SrsCalculator.MIN_LEVEL, newLevel)
    }

    // =========================================================================
    // Constants verification
    // =========================================================================

    @Test
    fun maxLevel_isFive() {
        assertEquals(5, SrsCalculator.MAX_LEVEL)
    }

    @Test
    fun minLevel_isZero() {
        assertEquals(0, SrsCalculator.MIN_LEVEL)
    }
}
