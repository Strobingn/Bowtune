package com.strobingn.bowtune.data

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * 2D point in normalized or pixel coordinates.
 * In screen/image space: +x is right, +y is down.
 */
data class GroupPoint(
    val x: Float,
    val y: Float
)

data class GroupMetrics(
    val arrowCount: Int,
    /** Center-to-center maximum distance between any two arrows (inches). */
    val extremeSpreadIn: Double,
    /** Outer-edge to outer-edge spread assuming [shaftDiameterIn] (inches). */
    val outerSpreadIn: Double,
    /** Horizontal spread (bounding box width, inches). */
    val widthIn: Double,
    /** Vertical spread (bounding box height, inches). */
    val heightIn: Double,
    /** Average distance from group centroid / Mean Point of Impact (inches). */
    val meanRadiusIn: Double,
    /** 50% Circular Error Probable radius estimate (inches). */
    val cep50In: Double,
    /** Radial standard deviation (inches). */
    val radialStdDevIn: Double,
    /** Angular size in Minute of Angle (MOA) if distance (yd) is provided. */
    val moa: Double?,
    /** Centroid in screen/canvas coordinates. */
    val centroidPx: GroupPoint,
    /** Indices of the two arrows that define the Extreme Spread. */
    val extremePairIndices: Pair<Int, Int>?,
    /** Maximum distance from centroid in pixels (for drawing bounding circle). */
    val maxRadiusFromCentroidPx: Float,
    /** Mean radius in pixels (for drawing mean-radius ring). */
    val meanRadiusPx: Float,
    /** Horizontal POI offset vs Aim Point in inches (+left from archer, i.e. -x on target). */
    val poiLeftRightIn: Double?,
    /** Vertical POI offset vs Aim Point in inches (+high on target, i.e. -y in screen space). */
    val poiHighLowIn: Double?,
    /** 1-based index of a statistical flyer arrow if detected. */
    val suspectedFlyerNumber: Int?,
    /** Actionable archery diagnostic advice based on group geometry. */
    val shapeAdvice: String,
    /** Compact summary for TuneSession.scoreOrGroup. */
    val compactSummary: String
)

object GroupCalculator {
    const val DEFAULT_SHAFT_DIAMETER_IN = 0.29 // ~9/32" standard hunting/target carbon shaft

    /**
     * Calculates complete arrow group statistics from screen/image points and a 2-point calibration scale.
     *
     * @param arrows List of marked arrow impact coordinates in canvas/image pixel space.
     * @param refStart Start point of the reference ruler in the same pixel space.
     * @param refEnd End point of the reference ruler in the same pixel space.
     * @param refLengthIn Known real-world length of the reference ruler in inches.
     * @param aimPoint Optional Point of Aim (POA) in the same pixel space.
     * @param distanceYd Optional shooting distance in yards (for MOA calculation).
     * @param shaftDiameterIn Arrow shaft outside diameter in inches (default 0.29").
     */
    fun calculate(
        arrows: List<GroupPoint>,
        refStart: GroupPoint,
        refEnd: GroupPoint,
        refLengthIn: Double,
        aimPoint: GroupPoint? = null,
        distanceYd: Double? = null,
        shaftDiameterIn: Double = DEFAULT_SHAFT_DIAMETER_IN
    ): GroupMetrics? {
        if (arrows.isEmpty() || refLengthIn <= 0.0) return null
        val refPx = hypot(
            (refEnd.x - refStart.x).toDouble(),
            (refEnd.y - refStart.y).toDouble()
        )
        if (refPx < 1e-3) return null
        val pxPerInch = refPx / refLengthIn

        val n = arrows.size
        val cxPx = arrows.map { it.x.toDouble() }.average()
        val cyPx = arrows.map { it.y.toDouble() }.average()
        val centroidPx = GroupPoint(cxPx.toFloat(), cyPx.toFloat())

        // Convert to inches relative to centroid (+X right, +Y up/high)
        val relXIn = DoubleArray(n) { i -> (arrows[i].x - cxPx) / pxPerInch }
        val relYIn = DoubleArray(n) { i -> (cyPx - arrows[i].y) / pxPerInch }

        var maxDistIn = 0.0
        var extremePair: Pair<Int, Int>? = null
        for (i in 0 until n) {
            for (j in i + 1 until n) {
                val d = hypot(relXIn[i] - relXIn[j], relYIn[i] - relYIn[j])
                if (d >= maxDistIn) {
                    maxDistIn = d
                    extremePair = i to j
                }
            }
        }

        val widthIn = if (n >= 2) (relXIn.max() - relXIn.min()) else 0.0
        val heightIn = if (n >= 2) (relYIn.max() - relYIn.min()) else 0.0
        val outerSpreadIn = if (n >= 2) maxDistIn + shaftDiameterIn.coerceAtLeast(0.0) else shaftDiameterIn

        val radiiIn = DoubleArray(n) { i -> hypot(relXIn[i], relYIn[i]) }
        val meanRadiusIn = if (n >= 2) radiiIn.average() else 0.0
        val maxRadiusIn = radiiIn.maxOrNull() ?: 0.0

        val (radialStdDevIn, cep50In) = if (n >= 2) {
            val varX = relXIn.sumOf { it * it } / (n - 1).coerceAtLeast(1)
            val varY = relYIn.sumOf { it * it } / (n - 1).coerceAtLeast(1)
            val sdX = sqrt(varX)
            val sdY = sqrt(varY)
            val rSd = sqrt(varX + varY)
            // Standard ballistic approximation: CEP50 ≈ 0.5887 * (sigma_x + sigma_y)
            val cep = 0.5887 * (sdX + sdY)
            rSd to cep
        } else {
            0.0 to 0.0
        }

        // Flyer detection: if N >= 4 and one arrow is > 2.0x mean radius and adds > 1.25"
        var flyerNum: Int? = null
        if (n >= 4 && meanRadiusIn > 0.25) {
            val maxIdx = radiiIn.indices.maxByOrNull { radiiIn[it] } ?: -1
            if (maxIdx >= 0 && radiiIn[maxIdx] > meanRadiusIn * 1.95 && (radiiIn[maxIdx] - meanRadiusIn) > 0.75) {
                flyerNum = maxIdx + 1
            }
        }

        // POI vs Aim: consistent with TuneHints (+left from archer = left of aim = -X on target; +high = -Y on screen)
        val poiLeftRightIn = aimPoint?.let { aim ->
            (aim.x - cxPx) / pxPerInch
        }
        val poiHighLowIn = aimPoint?.let { aim ->
            (aim.y - cyPx) / pxPerInch
        }

        // 1 MOA at 100 yd = 1.04719755 in -> at D yd = 0.010472 * D in
        val moa = if (distanceYd != null && distanceYd > 0.0 && n >= 2) {
            maxDistIn / (0.01047198 * distanceYd)
        } else {
            null
        }

        val shapeAdvice = buildShapeAdvice(
            n = n,
            extremeSpreadIn = maxDistIn,
            widthIn = widthIn,
            heightIn = heightIn,
            poiLeftRightIn = poiLeftRightIn,
            poiHighLowIn = poiHighLowIn,
            flyerNum = flyerNum
        )

        val compactSummary = if (n >= 2) {
            "${maxDistIn.fmt2()}\" ES (${widthIn.fmt1()}×${heightIn.fmt1()}\", $n arrows)"
        } else {
            "1 arrow marked"
        }

        return GroupMetrics(
            arrowCount = n,
            extremeSpreadIn = maxDistIn,
            outerSpreadIn = outerSpreadIn,
            widthIn = widthIn,
            heightIn = heightIn,
            meanRadiusIn = meanRadiusIn,
            cep50In = cep50In,
            radialStdDevIn = radialStdDevIn,
            moa = moa,
            centroidPx = centroidPx,
            extremePairIndices = extremePair,
            maxRadiusFromCentroidPx = (maxRadiusIn * pxPerInch).toFloat(),
            meanRadiusPx = (meanRadiusIn * pxPerInch).toFloat(),
            poiLeftRightIn = poiLeftRightIn,
            poiHighLowIn = poiHighLowIn,
            suspectedFlyerNumber = flyerNum,
            shapeAdvice = shapeAdvice,
            compactSummary = compactSummary
        )
    }

    private fun buildShapeAdvice(
        n: Int,
        extremeSpreadIn: Double,
        widthIn: Double,
        heightIn: Double,
        poiLeftRightIn: Double?,
        poiHighLowIn: Double?,
        flyerNum: Int?
    ): String {
        if (n < 2) {
            return "Tap at least 2 arrow impacts on the target to calculate group spread."
        }
        val parts = mutableListOf<String>()
        when {
            heightIn > 1.25 && heightIn >= widthIn * 1.65 -> {
                parts += "Vertical stringing (${heightIn.fmt1()}\" H vs ${widthIn.fmt1()}\" W): check peep/anchor consistency, D-loop pinch, cam timing sync, or bow-hand heel pressure."
            }
            widthIn > 1.25 && widthIn >= heightIn * 1.65 -> {
                parts += "Horizontal stringing (${widthIn.fmt1()}\" W vs ${heightIn.fmt1()}\" H): check bow-hand grip torque, sight bubble cant, release execution, or crosswind."
            }
            extremeSpreadIn <= 1.5 -> {
                parts += "Tight, well-balanced group (${extremeSpreadIn.fmt2()}\" center-to-center)."
            }
            else -> {
                parts += "Balanced group shape (${widthIn.fmt1()}\" W × ${heightIn.fmt1()}\" H)."
            }
        }
        if (flyerNum != null) {
            parts += "Arrow #$flyerNum sits outside the main cluster — number shafts and check nock index, spin, or vane contact if the same arrow repeats."
        }
        if (poiLeftRightIn != null && poiHighLowIn != null) {
            val lrLabel = when {
                abs(poiLeftRightIn) < 0.25 -> "centered L/R"
                poiLeftRightIn > 0 -> "${abs(poiLeftRightIn).fmt1()}\" left"
                else -> "${abs(poiLeftRightIn).fmt1()}\" right"
            }
            val hlLabel = when {
                abs(poiHighLowIn) < 0.25 -> "centered H/L"
                poiHighLowIn > 0 -> "${abs(poiHighLowIn).fmt1()}\" high"
                else -> "${abs(poiHighLowIn).fmt1()}\" low"
            }
            parts += "Group center vs aim: $hlLabel, $lrLabel."
        }
        return parts.joinToString(" ")
    }

    /**
     * Extracts a representative group size (in inches) from a free-text `scoreOrGroup` field
     * (e.g. `2.35" ES (1.8×2.1", 5 arrows)`, `2.5x3.0`, or `2.25"`).
     * Returns null if the string looks like a target score (e.g. `295/300`, `300 45X`) rather than inches.
     */
    fun parseGroupSizeInches(scoreOrGroup: String): Double? {
        val text = scoreOrGroup.trim()
        if (text.isBlank()) return null
        if (text.contains("/")) return null // e.g. 298/300 score

        // 1. Explicit ES pattern: "2.35\" ES" or "2.35 in ES"
        Regex("""(\d+(?:\.\d+)?)\s*(?:"|in\b)?\s*ES\b""", RegexOption.IGNORE_CASE)
            .find(text)?.groupValues?.getOrNull(1)?.toDoubleOrNull()?.let {
                if (it in 0.1..36.0) return it
            }

        // 2. WxH pattern: "2.5x3.0" or "2.5×3.0" -> use diagonal or max dimension
        Regex("""(\d+(?:\.\d+)?)\s*[x×X]\s*(\d+(?:\.\d+)?)""")
            .find(text)?.let { match ->
                val w = match.groupValues[1].toDoubleOrNull()
                val h = match.groupValues[2].toDoubleOrNull()
                if (w != null && h != null && w in 0.1..36.0 && h in 0.1..36.0) {
                    return ((hypot(w, h) * 100.0).roundToInt() / 100.0)
                }
            }

        // 3. Single inch measurement <= 24"
        Regex("""^(\d+(?:\.\d+)?)\s*(?:"|in\b|inch(?:es)?)?$""", RegexOption.IGNORE_CASE)
            .find(text)?.groupValues?.getOrNull(1)?.toDoubleOrNull()?.let {
                if (it in 0.1..24.0) return it
            }

        return null
    }

    private fun Double.fmt1(): String = ((this * 10.0).roundToInt() / 10.0).toString()
    private fun Double.fmt2(): String = ((this * 100.0).roundToInt() / 100.0).toString()
}
