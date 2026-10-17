package com.strobingn.bowtune.data

import kotlin.math.sqrt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TuneMathTest {

    @Test
    fun focUsesAmoFormulaFromTheNockThroat() {
        val result = Calculators.foc(lengthIn = 30.0, balanceFromNockIn = 18.0)
        assertEquals(10.0, result.focPercent!!, 0.001)
        assertTrue(result.note.contains("hunting") || result.note.contains("3D"))
    }

    @Test
    fun focRejectsABalancePointOffTheShaft() {
        val result = Calculators.foc(lengthIn = 28.0, balanceFromNockIn = 30.0)
        assertNull(result.focPercent)
        assertTrue(result.note.contains("on the shaft"))
    }

    @Test
    fun highFocDoesNotClaimMoreWindDrift() {
        val result = Calculators.foc(lengthIn = 28.0, balanceFromNockIn = 19.0)
        // (19 - 14) / 28 * 100 = 17.86%
        assertTrue(result.focPercent!! > 15.0)
        assertTrue(result.note.contains("drift less"))
        assertTrue(!result.note.contains("more wind drift"))
        assertTrue(result.note.contains("weaker"))
    }

    @Test
    fun gpiLookingShaftWeightIsCalledOut() {
        val result = Calculators.foc(lengthIn = 28.0, balanceFromNockIn = 16.0, shaftGr = 9.5)
        assertTrue(result.note.contains("GPI"))
    }

    @Test
    fun kineticEnergyAndMomentumUsePublishedConstants() {
        val result = Calculators.energy(400.0, 300.0)
        val expectedKe = (400.0 * 300.0 * 300.0) / 450240.0
        val expectedMom = (400.0 * 300.0) / 225218.0
        assertEquals(expectedKe, result.kineticFtLbs!!, 0.0001)
        assertEquals(expectedMom, result.momentumSlugFtS!!, 0.0001)
        assertTrue(result.note.contains("elk"))
    }

    @Test
    fun deerBandStartsAt40FootPounds() {
        val light = Calculators.energy(350.0, 250.0)
        // 350 * 62500 / 450240 ≈ 48.6, still deer. Use a slower arrow for the light band.
        val marginal = Calculators.energy(350.0, 200.0)
        // 350 * 40000 / 450240 ≈ 31.1
        assertTrue(marginal.kineticFtLbs!! in 25.0..40.0)
        assertTrue(marginal.note.contains("40 ft-lb"))
        assertTrue(light.kineticFtLbs!! > 40.0)
        assertTrue(light.note.contains("deer"))
    }

    @Test
    fun eastonChartMatchesPublishedCells() {
        assertEquals("400–350 spine (weaker→stiffer)", spineRange(60.0, 28.0, 100.0))
        assertEquals("350–300 spine (weaker→stiffer)", spineRange(70.0, 29.0, 100.0))
        assertEquals("500–400 spine (weaker→stiffer)", spineRange(40.0, 28.0, 100.0))
        assertEquals("600–500 spine (weaker→stiffer)", spineRange(30.0, 28.0, 100.0))
        assertEquals("300–250 spine (weaker→stiffer)", spineRange(80.0, 30.0, 100.0))
    }

    @Test
    fun heavyBowAt28InchesIsNotToldToRunA200Spine() {
        val result = Calculators.spine(72.0, 28.0, 100.0)
        assertEquals(350, result.weaker)
        assertEquals(300, result.stiffer)
        assertTrue((result.stiffer ?: 0) >= 250)
    }

    @Test
    fun pointWeightAndSpeedMoveTheChartTheEastonWay() {
        assertEquals("350–300 spine (weaker→stiffer)", spineRange(60.0, 28.0, 125.0))
        assertEquals("350–300 spine (weaker→stiffer)", spineRange(60.0, 28.5, 100.0))
        val fast = Calculators.spine(60.0, 28.0, 100.0, ataSpeedFps = 360.0)
        assertEquals(350, fast.weaker)
        assertEquals(300, fast.stiffer)
        assertTrue(fast.notes.any { it.contains("351") })
        val insert = Calculators.spine(60.0, 28.0, 100.0, insertWeightGr = 50.0)
        assertEquals("350–300 spine (weaker→stiffer)", insert.suggestedRange)
    }

    @Test
    fun shortArrowOnALightBowIsBlankOnTheChart() {
        val result = Calculators.spine(25.0, 23.0, 100.0)
        assertNull(result.weaker)
        assertTrue(result.suggestedRange.contains("No Easton"))
    }

    @Test
    fun groupSizeIsTheLongestCenterToCenter() {
        val points = listOf(
            GroupPoint(0.0, 0.0),
            GroupPoint(100.0, 0.0),
            GroupPoint(40.0, 30.0)
        )
        val metrics = GroupMeasure.metrics(points)!!
        assertEquals(100.0, metrics.extremeSpread, 0.001)
        assertEquals(100.0, metrics.horizontalSpread, 0.001)
        assertEquals(30.0, metrics.verticalSpread, 0.001)
        val readout = GroupMeasure.readout(
            points = points,
            inchesPerPixel = 0.1,
            distanceYards = 20.0,
            shaftDiameterIn = 0.25,
            aim = GroupPoint(50.0, 10.0)
        )!!
        assertEquals(10.0, readout.extremeInches, 0.001)
        assertEquals(10.25, readout.outsideToOutsideInches!!, 0.001)
        assertEquals("10 in", readout.scoreLabel())
        assertTrue(readout.pattern == GroupPattern.HORIZONTAL)
        assertTrue(readout.patternNote.contains("bare shaft"))
        assertTrue(readout.aimNote!!.contains("sight"))
        assertTrue(readout.aimNote!!.contains("rest"))
    }

    @Test
    fun verticalStringIsNotCalledASpineProblem() {
        val points = listOf(
            GroupPoint(0.0, 0.0),
            GroupPoint(1.0, 40.0),
            GroupPoint(0.5, 80.0)
        )
        val readout = GroupMeasure.readout(points, inchesPerPixel = 0.05, distanceYards = 20.0, shaftDiameterIn = null, aim = null)!!
        assertEquals(GroupPattern.VERTICAL, readout.pattern)
        assertTrue(readout.patternNote.contains("nock"))
        assertTrue(readout.patternNote.contains("not a spine"))
    }

    @Test
    fun coveringCircleOfAnEquilateralIsWiderThanTheExtremeSpread() {
        val side = 2.0
        val points = listOf(
            GroupPoint(0.0, 0.0),
            GroupPoint(side, 0.0),
            GroupPoint(side / 2.0, side * sqrt(3.0) / 2.0)
        )
        val metrics = GroupMeasure.metrics(points)!!
        assertEquals(side, metrics.extremeSpread, 0.001)
        val expectedDiameter = side / (sqrt(3.0) / 2.0)
        assertEquals(expectedDiameter, metrics.covering.diameter, 0.01)
        assertTrue(metrics.covering.diameter > metrics.extremeSpread)
    }

    @Test
    fun collinearGroupCircleMatchesTheExtremeSpread() {
        val points = listOf(GroupPoint(0.0, 0.0), GroupPoint(1.0, 0.0), GroupPoint(3.0, 0.0))
        val metrics = GroupMeasure.metrics(points)!!
        assertEquals(3.0, metrics.extremeSpread, 0.001)
        assertEquals(3.0, metrics.covering.diameter, 0.001)
        assertEquals(1.5, metrics.covering.cx, 0.001)
    }

    @Test
    fun insidePointDoesNotGrowTheCoveringCircle() {
        val points = listOf(
            GroupPoint(0.0, 0.0),
            GroupPoint(4.0, 0.0),
            GroupPoint(4.0, 4.0),
            GroupPoint(0.0, 4.0),
            GroupPoint(1.0, 1.0)
        )
        val metrics = GroupMeasure.metrics(points)!!
        assertEquals(hypot(4.0, 4.0), metrics.extremeSpread, 0.001)
        assertEquals(hypot(4.0, 4.0), metrics.covering.diameter, 0.01)
        assertEquals(2.0, metrics.covering.cx, 0.01)
        assertEquals(2.0, metrics.covering.cy, 0.01)
    }

    @Test
    fun moaUsesTheTangentDefinition() {
        val oneMoa = GroupMeasure.moa(GroupMeasure.INCHES_PER_MOA_AT_100_YD, 100.0)!!
        assertEquals(1.0, oneMoa, 0.000001)
        val atTwenty = GroupMeasure.moa(2.0, 20.0)!!
        assertEquals(2.0 / (GroupMeasure.INCHES_PER_MOA_AT_100_YD * 0.2), atTwenty, 0.000001)
        assertNull(GroupMeasure.moa(2.0, 0.0))
        assertNull(GroupMeasure.inchesPerPixel(10.0, 0.5))
    }

    @Test
    fun aimOnTheMarkDoesNotAskForASightMove() {
        val points = listOf(GroupPoint(0.0, 0.0), GroupPoint(10.0, 0.0))
        val readout = GroupMeasure.readout(
            points,
            inchesPerPixel = 0.01,
            distanceYards = 20.0,
            shaftDiameterIn = null,
            aim = GroupPoint(5.0, 0.0)
        )!!
        assertTrue(readout.aimNote!!.contains("No sight move"))
    }

    @Test
    fun faceReferenceConvertsCentimetersAt254() {
        assertEquals(40.0 / 2.54, GroupReference.FACE_40.inches, 0.000001)
        assertNull(GroupReference.CUSTOM.resolvedInches(0.0))
        assertEquals(8.5, GroupReference.CUSTOM.resolvedInches(8.5)!!, 0.0)
    }

    @Test
    fun formCuesScaleWithTheShoulderSpan() {
        assertEquals(FormCueRules.ShoulderLine.TILTED, FormCueRules.shoulderLine(19f))
        assertEquals(FormCueRules.ShoulderLine.MILD, FormCueRules.shoulderLine(12f))
        assertEquals(FormCueRules.ShoulderLine.LEVEL, FormCueRules.shoulderLine(4f))
        assertEquals(FormCueRules.BowShoulder.HIGH, FormCueRules.bowShoulder(-0.09f))
        assertEquals(FormCueRules.BowShoulder.LEVEL, FormCueRules.bowShoulder(-0.02f))
        assertEquals(FormCueRules.BowShoulder.LOW, FormCueRules.bowShoulder(0.2f))
        assertTrue(FormCueRules.headShifted(0.4f))
        assertTrue(!FormCueRules.headShifted(0.1f))
        assertTrue(FormCueRules.headDropped(0.6f))
        assertTrue(FormCueRules.leaning(0.3f))
        assertTrue(!FormCueRules.leaning(0.1f))
    }

    @Test
    fun twoArrowsAreRequired() {
        assertNull(GroupMeasure.metrics(listOf(GroupPoint(1.0, 1.0))))
        assertNull(GroupMeasure.metrics(emptyList()))
    }

    private fun spineRange(weight: Double, length: Double, point: Double): String =
        Calculators.spine(weight, length, point).suggestedRange

    private fun hypot(x: Double, y: Double): Double = kotlin.math.hypot(x, y)
}
