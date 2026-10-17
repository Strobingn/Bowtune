package com.strobingn.bowtune.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculatorsTest {
    @Test
    fun focFromBalancePoint() {
        val r = Calculators.foc(lengthIn = 28.0, balanceFromNockIn = 17.0, shaftGr = 270.0, pointGr = 100.0)
        assertEquals((17.0 - 14.0) / 28.0 * 100.0, r.focPercent!!, 1e-9)
        assertEquals(370.0, r.totalGrains!!, 1e-9)
    }

    @Test
    fun focNeedsLengthAndBalance() {
        val r = Calculators.foc(lengthIn = null, balanceFromNockIn = 17.0, pointGr = 100.0)
        assertNull(r.focPercent)
        assertEquals(100.0, r.totalGrains!!, 1e-9)
    }

    @Test
    fun spineExposesNumericBand() {
        val r = Calculators.spine(drawWeightLbs = 62.0, arrowLengthIn = 28.0, pointWeightGr = 100.0)
        assertEquals(340, r.weakSpine)
        assertEquals(300, r.stiffSpine)
        assertTrue(r.suggestedRange.startsWith("340–300"))
        assertTrue(r.weakSpine!! >= r.stiffSpine!!)
    }

    @Test
    fun spineOutOfRangeHasNoBand() {
        val r = Calculators.spine(drawWeightLbs = 10.0, arrowLengthIn = null, pointWeightGr = null)
        assertNull(r.weakSpine)
        assertNull(r.stiffSpine)
    }

    @Test
    fun energyFormula() {
        val r = Calculators.energy(400.0, 280.0)
        assertEquals(400.0 * 280 * 280 / 450240.0, r.kineticFtLbs!!, 1e-9)
        assertEquals(400.0 * 280 / 225218.0, r.momentumSlugFtS!!, 1e-9)
        assertNull(Calculators.energy(null, 280.0).kineticFtLbs)
    }
}
