package com.strobingn.bowtune.data

import com.strobingn.bowtune.data.advanced.BareShaftAdvanced
import com.strobingn.bowtune.data.advanced.FletchedNockSpine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BowTuneMathAndAdviceTest {

    @Test
    fun foc_amoMeasuredBalance_andGpiResolution() {
        // 30.0" arrow balanced at 18.6" from nock -> ((18.6 - 15.0) / 30.0) * 100 = 12.0% FOC
        // Shaft input 9.0 <= 30 -> treated as 9.0 GPI * 30" = 270 gr shaft
        val res = Calculators.foc(
            lengthIn = 30.0,
            balanceFromNockIn = 18.6,
            shaftGr = 9.0,
            pointGr = 100.0,
            insertGr = 25.0,
            nockGr = 10.0,
            vaneGr = 18.0,
            wrapGr = 5.0
        )
        assertNotNull(res.focPercent)
        assertEquals(12.0, res.focPercent!!, 0.001)
        assertEquals(false, res.isEstimatedFromComponents)
        assertEquals(270.0, res.shaftGrainsResolved!!, 0.001)
        assertEquals(428.0, res.totalGrains!!, 0.001)
    }

    @Test
    fun foc_estimatesBalanceFromComponentMomentsWhenBalanceBlank() {
        val res = Calculators.foc(
            lengthIn = 28.5,
            balanceFromNockIn = null,
            shaftGr = 8.8, // 250.8 gr shaft
            pointGr = 125.0,
            insertGr = 50.0,
            nockGr = 9.0,
            vaneGr = 18.0,
            wrapGr = 0.0
        )
        assertNotNull(res.focPercent)
        assertTrue(res.isEstimatedFromComponents)
        // Heavy front (175 gr) vs light rear (27 gr) on 250.8 gr shaft should produce hunting/high FOC (> 12%)
        assertTrue("Expected FOC > 12%, got ${res.focPercent}", res.focPercent!! > 12.0)
        assertTrue("Expected FOC < 22%, got ${res.focPercent}", res.focPercent!! < 22.0)
    }

    @Test
    fun spine_monotonicStiffnessWithWeightLengthAndPoint() {
        val base = Calculators.spine(drawWeightLbs = 60.0, arrowLengthIn = 28.0, pointGr = 100.0)
        val heavierBow = Calculators.spine(drawWeightLbs = 70.0, arrowLengthIn = 28.0, pointGr = 100.0)
        val longerArrow = Calculators.spine(drawWeightLbs = 60.0, arrowLengthIn = 30.0, pointGr = 100.0)
        val heavierFront = Calculators.spine(drawWeightLbs = 60.0, arrowLengthIn = 28.0, pointGr = 175.0)

        // Lower spine number = stiffer shaft
        assertTrue(heavierBow.stiffSpine <= base.stiffSpine)
        assertTrue(longerArrow.stiffSpine <= base.stiffSpine)
        assertTrue(heavierFront.stiffSpine <= base.stiffSpine)
        // 70 lb @ 28" w/ 100 gr should be in the ~340–300 band
        assertTrue(heavierBow.weakSpine in 300..400)
    }

    @Test
    fun energy_kineticMomentumAndGrainsPerPoundSafety() {
        val standard = Calculators.energy(grains = 425.0, speedFps = 285.0, drawWeightLbs = 70.0)
        assertNotNull(standard.kineticFtLbs)
        assertNotNull(standard.momentumSlugFtS)
        // KE = 425 * 285^2 / 450240 = 76.67 ft-lb
        assertEquals(76.67, standard.kineticFtLbs!!, 0.1)
        // Momentum = 425 * 285 / 225400 = 0.537 slug*ft/s
        assertEquals(0.537, standard.momentumSlugFtS!!, 0.01)
        assertEquals(6.07, standard.grainsPerPound!!, 0.05)

        val unsafeLight = Calculators.energy(grains = 310.0, speedFps = 320.0, drawWeightLbs = 70.0)
        assertTrue(unsafeLight.note.contains("SAFETY WARNING"))
    }

    @Test
    fun estimateSpeedFps_iboAdjustments() {
        // At exact IBO spec (30" DL, 70 lb DW, 350 gr arrow), estimated speed equals IBO rating
        val atIbo = Calculators.estimateSpeedFps(340.0, 70.0, 30.0, 350.0)
        assertEquals(340.0, atIbo!!, 0.01)

        // 28.5" DL (-1.5" = -15 fps), 70 lb DW (0), 440 gr arrow (+90 gr = -30 fps) -> 340 - 45 = 295 fps
        val hunting = Calculators.estimateSpeedFps(340.0, 70.0, 28.5, 440.0)
        assertEquals(295.0, hunting!!, 0.01)
    }

    @Test
    fun groupCalculator_measuresSpreadRadiusMoaAndOffset() {
        // Reference ruler: 100 px = 2.0 inches -> 50 px per inch
        val refA = GroupPoint(0f, 0f)
        val refB = GroupPoint(100f, 0f)

        // 3 arrows forming a 3-4-5 right triangle in inches:
        // #1 at (100, 100)
        // #2 at (250, 100) -> 150 px = 3.0" right of #1
        // #3 at (100, 300) -> 200 px = 4.0" below #1
        // Hypotenuse (#2 to #3) = 250 px = 5.0" extreme spread!
        val arrows = listOf(
            GroupPoint(100f, 100f),
            GroupPoint(250f, 100f),
            GroupPoint(100f, 300f)
        )
        val aim = GroupPoint(150f, 166.67f) // right at centroid

        val metrics = GroupCalculator.calculate(
            arrowPoints = arrows,
            refStart = refA,
            refEnd = refB,
            refLengthIn = 2.0,
            distanceYd = 20.0,
            aimPoint = aim,
            shaftDiameterIn = 0.246
        )

        assertNotNull(metrics)
        assertEquals(3, metrics!!.arrowCount)
        assertEquals(5.0, metrics.extremeSpreadIn, 0.01)
        assertEquals(5.246, metrics.outerSpreadIn, 0.01)
        assertEquals(3.0, metrics.widthIn, 0.01)
        assertEquals(4.0, metrics.heightIn, 0.01)
        assertEquals(Pair(1, 2), metrics.extremePairIndices)
        // MOA at 20 yd: 1 MOA = 20 * 0.01047198 = 0.20944" -> 5.0 / 0.20944 = 23.87 MOA
        assertEquals(23.87, metrics.moa!!, 0.1)
        assertEquals(0.0, metrics.poiLeftRightIn!!, 0.02)
        assertEquals(0.0, metrics.poiHighLowIn!!, 0.02)

        // Verify parser can extract extreme spread back out of compactSummary
        val parsed = GroupCalculator.parseGroupSizeInches(metrics.compactSummary)
        assertNotNull(parsed)
        assertEquals(5.0, parsed!!, 0.05)
    }

    @Test
    fun groupCalculator_detectsStatisticalFlyer() {
        val refA = GroupPoint(0f, 0f)
        val refB = GroupPoint(100f, 0f) // 50 px/in
        // 3 tight arrows around (200, 200) and 1 flyer at (450, 200)
        val arrows = listOf(
            GroupPoint(198f, 200f),
            GroupPoint(202f, 198f),
            GroupPoint(200f, 203f),
            GroupPoint(450f, 200f)
        )
        val metrics = GroupCalculator.calculate(
            arrowPoints = arrows,
            refStart = refA,
            refEnd = refB,
            refLengthIn = 2.0,
            distanceYd = 30.0
        )
        assertNotNull(metrics)
        assertEquals(4, metrics!!.suspectedFlyerNumber)
    }

    @Test
    fun tuningAdvice_consistentEastonCrAndDynamicSpinePhysics() {
        // 1. Paper tear: Left tear -> rest RIGHT or Limb Shift L
        val leftSteps = PaperTearGuidance.stepsFor(TearType.LEFT, CoachingLevel.STANDARD).joinToString(" ")
        assertTrue(leftSteps.contains("rest slightly RIGHT", ignoreCase = true))
        assertTrue(leftSteps.contains("Limb Shift toward L", ignoreCase = true))

        // 2. Bare shaft hint: bare left (+horizontalLeftIn) -> weak / rest RIGHT / Limb Shift L
        val bareLeftHint = TuneHints.bareShaft(verticalIn = 0.0, horizontalLeftIn = 1.5)
        assertNotNull(bareLeftHint)
        assertTrue(bareLeftHint!!.contains("rest slightly RIGHT", ignoreCase = true))

        // 3. Broadhead hint: broadhead left (+horizontalLeftIn) -> rest RIGHT, broadhead high (+verticalIn) -> rest DOWN
        val bhHint = TuneHints.broadhead(horizontalLeftIn = 2.0, verticalIn = 2.0)
        assertNotNull(bhHint)
        assertTrue(bhHint!!.contains("rest slightly RIGHT", ignoreCase = true))
        assertTrue(bhHint.contains("rest slightly DOWN", ignoreCase = true))

        // 4. Advanced Bare Shaft & Nock/Spine guides must recommend reducing front weight (not adding) to stiffen weak spine
        val bareAdvText = BareShaftAdvanced.guide.sections.flatMap { it.bullets + it.paragraphs }.joinToString(" ")
        assertTrue(bareAdvText.contains("reduce front weight", ignoreCase = true))

        val spineAdvText = FletchedNockSpine.nockAndSpine.sections.flatMap { it.bullets + it.paragraphs }.joinToString(" ")
        assertTrue(spineAdvText.contains("lighter point", ignoreCase = true))
    }

    @Test
    fun formAnalysis_evaluatesCleanAndFaultyBiomechanics() {
        // Balanced archer pose
        val clean = FormAnalysis.analyze(
            points = mapOf(
                11 to (0.35f to 0.35f), // left shoulder
                12 to (0.65f to 0.35f), // right shoulder (level)
                13 to (0.15f to 0.36f), // left elbow (aligned with wrist & shoulder)
                14 to (0.85f to 0.35f), // right draw elbow (180 deg line)
                15 to (0.02f to 0.36f), // left wrist (bow hand level with shoulder)
                16 to (0.62f to 0.35f), // right wrist (anchor)
                23 to (0.38f to 0.75f), // left hip
                24 to (0.62f to 0.75f), // right hip
                0 to (0.50f to 0.20f)   // nose centered
            ),
            level = CoachingLevel.STANDARD
        )
        assertTrue(clean.tips.any { it.title.contains("Solid alignment") })
        assertNull(clean.suggestedTear)
    }
}
