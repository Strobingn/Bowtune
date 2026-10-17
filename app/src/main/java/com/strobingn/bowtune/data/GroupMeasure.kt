package com.strobingn.bowtune.data

import kotlin.math.PI
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.tan

/** A point in photo pixels. X grows right, Y grows down, matching a bitmap. */
data class GroupPoint(val x: Double, val y: Double)

data class GroupCircle(val cx: Double, val cy: Double, val radius: Double) {
    val diameter: Double get() = radius * 2.0
}

enum class GroupPattern { ROUND, VERTICAL, HORIZONTAL }

data class AimOffset(
    /** Inches right of the aim mark. Negative is left. */
    val rightInches: Double,
    /** Inches above the aim mark. Negative is low. */
    val highInches: Double
)

data class GroupMetrics(
    val count: Int,
    /** Longest center-to-center distance. This is the group size. */
    val extremeSpread: Double,
    val extremeIndexA: Int,
    val extremeIndexB: Int,
    val horizontalSpread: Double,
    val verticalSpread: Double,
    val centroidX: Double,
    val centroidY: Double,
    /** Average distance from the centroid. */
    val meanRadius: Double,
    /** Smallest circle that covers every center. Its diameter can exceed the extreme spread. */
    val covering: GroupCircle
)

data class GroupReadout(
    val metrics: GroupMetrics,
    val inchesPerPixel: Double,
    val extremeInches: Double,
    val horizontalInches: Double,
    val verticalInches: Double,
    val meanRadiusInches: Double,
    val coveringDiameterInches: Double,
    /** Center-to-center plus one shaft diameter. Null when diameter was not given. */
    val outsideToOutsideInches: Double?,
    val moa: Double?,
    val pattern: GroupPattern,
    val patternNote: String,
    val sizeNote: String?,
    val aim: AimOffset?,
    val aimNote: String?
) {
    fun scoreLabel(): String = "${formatInches(extremeInches)} in"

    fun logNotes(
        referenceLabel: String,
        distanceYards: Double?,
        shaftDiameterIn: Double?
    ): String = buildString {
        appendLine("Shot Vision — group measure")
        appendLine("Arrows: ${metrics.count}")
        appendLine("Group size (center-to-center extreme spread): ${formatInches(extremeInches)} in")
        appendLine(
            "Horizontal ${formatInches(horizontalInches)} in · vertical ${formatInches(verticalInches)} in"
        )
        appendLine("Mean radius from the group center: ${formatInches(meanRadiusInches)} in")
        appendLine(
            "Smallest circle around the centers: ${formatInches(coveringDiameterInches)} in across. " +
                "That circle is only the picture. The recorded group size is the extreme spread."
        )
        moa?.let { appendLine("Group subtends ${formatInches(it)} MOA at ${distanceYards?.let(::formatInches) ?: "?"} yd.") }
        outsideToOutsideInches?.let {
            appendLine(
                "Outside-to-outside: ${formatInches(it)} in " +
                    "(extreme spread plus one shaft diameter of ${formatInches(shaftDiameterIn ?: 0.0)} in)."
            )
        }
        appendLine("Scale: $referenceLabel")
        appendLine(patternNote)
        sizeNote?.let { appendLine(it) }
        aimNote?.let { appendLine(it) }
    }.trim()
}

object GroupMeasure {
    /**
     * 1 MOA at 100 yards, from tan(1/60 degree) × 3600 inches.
     * About 1.047 in, not the rounded "1 inch at 100 yards" rule.
     */
    val INCHES_PER_MOA_AT_100_YD: Double = tan(PI / 10800.0) * 3600.0

    fun inchesPerPixel(referenceInches: Double, referencePixels: Double): Double? {
        if (referenceInches <= 0.0 || referencePixels <= 1.0) return null
        return referenceInches / referencePixels
    }

    /** True angular minutes. Null when the distance is missing. */
    fun moa(sizeInches: Double, distanceYards: Double): Double? {
        if (sizeInches < 0.0 || distanceYards <= 0.0) return null
        val inchesPerMoa = INCHES_PER_MOA_AT_100_YD * (distanceYards / 100.0)
        return sizeInches / inchesPerMoa
    }

    fun metrics(points: List<GroupPoint>): GroupMetrics? {
        if (points.size < 2) return null
        var best = -1.0
        var indexA = 0
        var indexB = 1
        for (i in points.indices) {
            for (j in i + 1 until points.size) {
                val d = distance(points[i], points[j])
                if (d > best) {
                    best = d
                    indexA = i
                    indexB = j
                }
            }
        }
        val xs = points.map { it.x }
        val ys = points.map { it.y }
        val cx = xs.average()
        val cy = ys.average()
        val mean = points.map { hypot(it.x - cx, it.y - cy) }.average()
        return GroupMetrics(
            count = points.size,
            extremeSpread = best,
            extremeIndexA = indexA,
            extremeIndexB = indexB,
            horizontalSpread = (xs.max() - xs.min()),
            verticalSpread = (ys.max() - ys.min()),
            centroidX = cx,
            centroidY = cy,
            meanRadius = mean,
            covering = smallestEnclosingCircle(points)
        )
    }

    fun readout(
        points: List<GroupPoint>,
        inchesPerPixel: Double,
        distanceYards: Double?,
        shaftDiameterIn: Double?,
        aim: GroupPoint?
    ): GroupReadout? {
        if (inchesPerPixel <= 0.0) return null
        val metrics = metrics(points) ?: return null
        val extremeIn = metrics.extremeSpread * inchesPerPixel
        val horizontalIn = metrics.horizontalSpread * inchesPerPixel
        val verticalIn = metrics.verticalSpread * inchesPerPixel
        val pattern = patternOf(horizontalIn, verticalIn, extremeIn)
        val aimOffset = aim?.let {
            AimOffset(
                rightInches = (metrics.centroidX - it.x) * inchesPerPixel,
                highInches = (it.y - metrics.centroidY) * inchesPerPixel
            )
        }
        val diameter = shaftDiameterIn?.takeIf { it > 0.0 }
        return GroupReadout(
            metrics = metrics,
            inchesPerPixel = inchesPerPixel,
            extremeInches = extremeIn,
            horizontalInches = horizontalIn,
            verticalInches = verticalIn,
            meanRadiusInches = metrics.meanRadius * inchesPerPixel,
            coveringDiameterInches = metrics.covering.diameter * inchesPerPixel,
            outsideToOutsideInches = diameter?.let { extremeIn + it },
            moa = distanceYards?.let { moa(extremeIn, it) },
            pattern = pattern,
            patternNote = patternNote(pattern),
            sizeNote = sizeNote(extremeIn, distanceYards),
            aim = aimOffset,
            aimNote = aimOffset?.let(::aimNote)
        )
    }

    fun patternOf(horizontalIn: Double, verticalIn: Double, extremeIn: Double): GroupPattern {
        if (extremeIn <= 0.0) return GroupPattern.ROUND
        return when {
            verticalIn > horizontalIn * 1.75 && verticalIn > extremeIn * 0.65 -> GroupPattern.VERTICAL
            horizontalIn > verticalIn * 1.75 && horizontalIn > extremeIn * 0.65 -> GroupPattern.HORIZONTAL
            else -> GroupPattern.ROUND
        }
    }

    fun patternNote(pattern: GroupPattern): String = when (pattern) {
        GroupPattern.VERTICAL ->
            "The arrows are strung vertically. On a fletched group that is mostly nock height, third axis, peep, or form — not a spine problem. Confirm with paper and a bare shaft before you move the rest up or down."
        GroupPattern.HORIZONTAL ->
            "The arrows are strung sideways. Left-right on fletched arrows is often grip torque or a sight move. Centershot and spine show up on paper and a bare shaft, not on this fletched group alone."
        GroupPattern.ROUND ->
            "The group is fairly round. The misses are spread around the center, which is usually form, wind, or execution rather than one tune axis."
    }

    fun aimNote(aim: AimOffset): String {
        val right = aim.rightInches
        val high = aim.highInches
        if (kotlin.math.abs(right) < 0.25 && kotlin.math.abs(high) < 0.25) {
            return "The group center is on the aim mark (inside 1/4 in). No sight move shows up in this group."
        }
        val side = when {
            right >= 0.25 -> "${formatInches(right)} in right"
            right <= -0.25 -> "${formatInches(-right)} in left"
            else -> "centered left-right"
        }
        val vert = when {
            high >= 0.25 -> "${formatInches(high)} in high"
            high <= -0.25 -> "${formatInches(-high)} in low"
            else -> "centered up-down"
        }
        return "The group center is $side and $vert of the mark you aimed at. " +
            "Move the sight toward the group (chase the arrows). " +
            "Do not move the rest to chase a fletched group — rest and spine come from paper and bare shaft."
    }

    fun sizeNote(extremeInches: Double, distanceYards: Double?): String? {
        if (distanceYards == null || distanceYards <= 0.0) return null
        val at20 = extremeInches * (20.0 / distanceYards)
        val scaled = if (kotlin.math.abs(distanceYards - 20.0) < 0.6) {
            "At 20 yd this group is ${formatInches(extremeInches)} in."
        } else {
            "If this group is angular, at 20 yd it would be about ${formatInches(at20)} in."
        }
        val band = when {
            at20 <= 1.5 -> "That is a tight compound group."
            at20 <= 4.0 ->
                "A lot of hunters want the group inside 4 in at 20 yd before they trust a shot at that distance."
            else ->
                "That is wider than a 4 in check at 20 yd. Confirm form and one tune axis before you change several things."
        }
        return "$scaled $band"
    }

    /**
     * Welzl's smallest enclosing circle. Order is fixed so the result does not depend on a random shuffle.
     */
    fun smallestEnclosingCircle(points: List<GroupPoint>): GroupCircle {
        require(points.isNotEmpty())
        return welzl(points, points.size, emptyList())
    }

    private fun welzl(points: List<GroupPoint>, n: Int, boundary: List<GroupPoint>): GroupCircle {
        if (n == 0 || boundary.size == 3) return circleFrom(boundary)
        val point = points[n - 1]
        val without = welzl(points, n - 1, boundary)
        if (contains(without, point)) return without
        return welzl(points, n - 1, boundary + point)
    }

    private fun contains(circle: GroupCircle, point: GroupPoint): Boolean {
        val slack = 1e-6 + circle.radius * 1e-7
        return hypot(point.x - circle.cx, point.y - circle.cy) <= circle.radius + slack
    }

    private fun circleFrom(boundary: List<GroupPoint>): GroupCircle = when (boundary.size) {
        0 -> GroupCircle(0.0, 0.0, 0.0)
        1 -> GroupCircle(boundary[0].x, boundary[0].y, 0.0)
        2 -> diameterCircle(boundary[0], boundary[1])
        else -> circumcircle(boundary[0], boundary[1], boundary[2])
    }

    private fun diameterCircle(a: GroupPoint, b: GroupPoint): GroupCircle {
        val cx = (a.x + b.x) / 2.0
        val cy = (a.y + b.y) / 2.0
        return GroupCircle(cx, cy, hypot(a.x - b.x, a.y - b.y) / 2.0)
    }

    private fun circumcircle(a: GroupPoint, b: GroupPoint, c: GroupPoint): GroupCircle {
        val d = 2.0 * (a.x * (b.y - c.y) + b.x * (c.y - a.y) + c.x * (a.y - b.y))
        if (kotlin.math.abs(d) < 1e-8) {
            return listOf(a to b, a to c, b to c)
                .maxBy { distance(it.first, it.second) }
                .let { diameterCircle(it.first, it.second) }
        }
        val a2 = a.x * a.x + a.y * a.y
        val b2 = b.x * b.x + b.y * b.y
        val c2 = c.x * c.x + c.y * c.y
        val ux = (a2 * (b.y - c.y) + b2 * (c.y - a.y) + c2 * (a.y - b.y)) / d
        val uy = (a2 * (c.x - b.x) + b2 * (a.x - c.x) + c2 * (b.x - a.x)) / d
        val center = GroupPoint(ux, uy)
        return GroupCircle(ux, uy, distance(center, a))
    }

    private fun distance(a: GroupPoint, b: GroupPoint): Double = hypot(a.x - b.x, a.y - b.y)
}

fun formatInches(value: Double): String {
    val hundredths = (value * 100.0).roundToInt() / 100.0
    val rounded = if (kotlin.math.abs(hundredths) >= 10.0) {
        (value * 10.0).roundToInt() / 10.0
    } else {
        hundredths
    }
    val asLong = rounded.toLong()
    return if (rounded == asLong.toDouble()) asLong.toString() else rounded.toString()
}

/** Face diameters are the outer scoring edge. Centimeters convert at 2.54 cm per inch. */
enum class GroupReference(
    val label: String,
    val inches: Double,
    val usesCircle: Boolean
) {
    FACE_40("40 cm face", 40.0 / 2.54, true),
    FACE_60("60 cm face", 60.0 / 2.54, true),
    FACE_80("80 cm face", 80.0 / 2.54, true),
    FACE_122("122 cm face", 122.0 / 2.54, true),
    RULER_12("12 in reference", 12.0, false),
    CUSTOM("Custom length", 0.0, true);

    fun resolvedInches(customInches: Double?): Double? = when (this) {
        CUSTOM -> customInches?.takeIf { it > 0.0 }
        else -> inches
    }
}
