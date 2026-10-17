package com.strobingn.bowtune.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculatorsTest {
    @Test
    fun focUsesNockThroatBalanceFormulaAndSumsComponents() {
        val result = Calculators.foc(
            lengthIn = 29.0,
            balanceFromNockIn = 18.0,
            shaftGr = 261.0,
            pointGr = 100.0,
            insertGr = 25.0,
            nockGr = 10.0,
            vaneGr = 21.0,
            wrapGr = 5.0
        )

        assertEquals(12.0689, requireNotNull(result.focPercent), 0.0001)
        assertEquals(422.0, requireNotNull(result.totalGrains), 0.0001)
        assertTrue(result.note.contains("Typical hunting"))
    }

    @Test
    fun focRejectsImpossibleBalancePointAndNegativeWeight() {
        assertNull(Calculators.foc(28.0, 29.0).focPercent)
        assertNull(Calculators.foc(28.0, 17.0, pointGr = -100.0).focPercent)
    }

    @Test
    fun energyMatchesStandardArcheryEquations() {
        val result = Calculators.energy(450.0, 280.0)

        assertEquals(78.36, requireNotNull(result.kineticFtLbs), 0.01)
        assertEquals(0.55945, requireNotNull(result.momentumSlugFtS), 0.00001)
    }

    @Test
    fun spineValidatesPhysicalInputRanges() {
        assertEquals(
            "Enter arrow length (20–36 in)",
            Calculators.spine(60.0, 12.0, 100.0).suggestedRange
        )
        assertEquals(
            "Enter point weight (50–300 gr)",
            Calculators.spine(60.0, 28.0, 500.0).suggestedRange
        )
    }
}
