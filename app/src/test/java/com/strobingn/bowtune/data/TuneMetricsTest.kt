package com.strobingn.bowtune.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TuneMetricsTest {
    @Test
    fun encodeDecodeRoundTrip() {
        val values = mapOf(TuneMetrics.BARE_V to 0.5, TuneMetrics.BARE_H to -1.25, TuneMetrics.walkBackKey(30) to 2.0)
        val json = TuneMetrics.encode(values)
        assertEquals(values, TuneMetrics.decode(json))
    }

    @Test
    fun decodeToleratesLegacyAndGarbage() {
        assertTrue(TuneMetrics.decode("{}").isEmpty())
        assertTrue(TuneMetrics.decode("").isEmpty())
        assertTrue(TuneMetrics.decode(null).isEmpty())
        assertTrue(TuneMetrics.decode("not json").isEmpty())
        assertEquals(mapOf("score" to 290.0), TuneMetrics.decode("{ \"score\" : 290, \"note\": \"x\" }"))
    }

    @Test
    fun encodeSkipsNonFiniteValues() {
        assertEquals("{\"a\":1}", TuneMetrics.encode(mapOf("a" to 1.0, "b" to Double.NaN)))
    }

    @Test
    fun formatNumberTrimsZeros() {
        assertEquals("70", formatNumber(70.0))
        assertEquals("29.5", formatNumber(29.50))
        assertEquals("0.13", formatNumber(0.125))
        assertEquals("0.125", formatNumber(0.125, 3))
        assertEquals("0", formatNumber(-0.0001))
        assertEquals("-1.25", formatNumber(-1.25))
    }

    @Test
    fun snapToStepClampsAndRounds() {
        assertEquals(29.5, snapToStep(29.6, 24.0, 34.0, 0.5), 1e-9)
        assertEquals(24.0, snapToStep(10.0, 24.0, 34.0, 0.5), 1e-9)
        assertEquals(34.0, snapToStep(99.0, 24.0, 34.0, 0.5), 1e-9)
        assertEquals(0.375, snapToStep(0.4, 0.0, 3.0, 0.125), 1e-9)
    }

    @Test
    fun strictNumberAcceptsUnitsButRejectsNotes() {
        assertEquals(70.0, strictNumberOrNull("70")!!, 1e-9)
        assertEquals(29.5, strictNumberOrNull("29.5 in")!!, 1e-9)
        assertEquals(29.5, strictNumberOrNull("29,5\"")!!, 1e-9)
        assertEquals(310.0, strictNumberOrNull("310fps")!!, 1e-9)
        assertEquals(-0.5, strictNumberOrNull("-0.5")!!, 1e-9)
        assertNull(strictNumberOrNull("2 clicks left"))
        assertNull(strictNumberOrNull("0.5 L"))
        assertNull(strictNumberOrNull(""))
        assertNull(strictNumberOrNull(null))
    }

    @Test
    fun parseLooseNumberHandlesFractions() {
        assertEquals(0.25, parseLooseNumber("1/4")!!, 1e-9)
        assertEquals(1.25, parseLooseNumber("1 1/4")!!, 1e-9)
        assertEquals(-0.5, parseLooseNumber("-1/2")!!, 1e-9)
        assertEquals(0.5, parseLooseNumber("+0.5 high")!!, 1e-9)
        assertNull(parseLooseNumber("none"))
    }

    @Test
    fun signedOffsetWordsOverrideSign() {
        assertEquals(0.5, parseSignedOffset("+0.5 high", VERTICAL_UP_WORDS, VERTICAL_DOWN_WORDS)!!, 1e-9)
        assertEquals(-0.25, parseSignedOffset("0.25 low", VERTICAL_UP_WORDS, VERTICAL_DOWN_WORDS)!!, 1e-9)
        assertEquals(0.25, parseSignedOffset("0.25 left", LEFT_WORDS, RIGHT_WORDS)!!, 1e-9)
        assertEquals(-0.5, parseSignedOffset("0.5 right", LEFT_WORDS, RIGHT_WORDS)!!, 1e-9)
        assertEquals(-0.5, parseSignedOffset("0.5 L", RIGHT_WORDS, LEFT_WORDS)!!, 1e-9)
        assertEquals(-0.75, parseSignedOffset("-0.75", LEFT_WORDS, RIGHT_WORDS)!!, 1e-9)
        assertNull(parseSignedOffset("", LEFT_WORDS, RIGHT_WORDS))
    }

    @Test
    fun describeRoundTripsThroughParser() {
        listOf(-1.5, -0.125, 0.0, 0.5, 2.25).forEach { v ->
            assertEquals(v, parseSignedOffset(describeVertical(v), VERTICAL_UP_WORDS, VERTICAL_DOWN_WORDS)!!, 1e-9)
            assertEquals(v, parseSignedOffset(describeLeftPositive(v), LEFT_WORDS, RIGHT_WORDS)!!, 1e-9)
            assertEquals(v, parseSignedOffset(describeRightPositive(v), RIGHT_WORDS, LEFT_WORDS)!!, 1e-9)
        }
    }

    @Test
    fun inchFractionUsesCommonFractions() {
        assertEquals("1/8", inchFraction(0.125))
        assertEquals("1/2", inchFraction(0.5))
        assertEquals("1 3/4", inchFraction(1.75))
        assertEquals("2", inchFraction(2.0))
        assertEquals("-1/4", inchFraction(-0.25))
        assertEquals("0.3", inchFraction(0.3))
    }
}
