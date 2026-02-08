package com.example.englishword.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ReviewResultTest {

    // ---- Each enum has the correct integer value ----

    @Test
    fun again_hasValue0() {
        assertEquals(0, ReviewResult.AGAIN.value)
    }

    @Test
    fun later_hasValue1() {
        assertEquals(1, ReviewResult.LATER.value)
    }

    @Test
    fun known_hasValue2() {
        assertEquals(2, ReviewResult.KNOWN.value)
    }

    // ---- fromValue returns the correct enum for valid inputs ----

    @Test
    fun fromValue_0_returnsAgain() {
        assertEquals(ReviewResult.AGAIN, ReviewResult.fromValue(0))
    }

    @Test
    fun fromValue_1_returnsLater() {
        assertEquals(ReviewResult.LATER, ReviewResult.fromValue(1))
    }

    @Test
    fun fromValue_2_returnsKnown() {
        assertEquals(ReviewResult.KNOWN, ReviewResult.fromValue(2))
    }

    // ---- fromValue defaults to LATER for invalid inputs ----

    @Test
    fun fromValue_negative1_defaultsToLater() {
        assertEquals(ReviewResult.LATER, ReviewResult.fromValue(-1))
    }

    @Test
    fun fromValue_99_defaultsToLater() {
        assertEquals(ReviewResult.LATER, ReviewResult.fromValue(99))
    }

    @Test
    fun fromValue_intMin_defaultsToLater() {
        assertEquals(ReviewResult.LATER, ReviewResult.fromValue(Int.MIN_VALUE))
    }

    @Test
    fun fromValue_intMax_defaultsToLater() {
        assertEquals(ReviewResult.LATER, ReviewResult.fromValue(Int.MAX_VALUE))
    }

    // ---- Companion object constants match enum values ----

    @Test
    fun againValue_matchesAgainEnumValue() {
        assertEquals(ReviewResult.AGAIN.value, ReviewResult.AGAIN_VALUE)
    }

    @Test
    fun laterValue_matchesLaterEnumValue() {
        assertEquals(ReviewResult.LATER.value, ReviewResult.LATER_VALUE)
    }

    @Test
    fun knownValue_matchesKnownEnumValue() {
        assertEquals(ReviewResult.KNOWN.value, ReviewResult.KNOWN_VALUE)
    }
}
