package com.strobingn.bowtune.data

import kotlin.math.hypot

data class MeasurementPoint(val x: Float, val y: Float)

data class GroupMeasurementResult(
    val sizeInches: Double,
    val widestPair: Pair<Int, Int>,
    val pixelsPerInch: Double
)

/**
 * Measures the widest center-to-center distance in an arrow group.
 *
 * Points must be in the same image coordinate system. The first two reference
 * points describe a known length photographed in the target plane.
 */
object GroupMeasurement {
    fun calculate(
        referencePoints: List<MeasurementPoint>,
        referenceInches: Double?,
        arrowCenters: List<MeasurementPoint>
    ): GroupMeasurementResult? {
        if (referencePoints.size != 2 ||
            referenceInches == null ||
            !referenceInches.isFinite() ||
            referenceInches <= 0.0 ||
            arrowCenters.size < 2
        ) {
            return null
        }

        val referencePixels = distance(referencePoints[0], referencePoints[1])
        if (!referencePixels.isFinite() || referencePixels <= 0.0) return null

        var widestPixels = -1.0
        var widestPair = 0 to 1
        for (first in 0 until arrowCenters.lastIndex) {
            for (second in first + 1..arrowCenters.lastIndex) {
                val distance = distance(arrowCenters[first], arrowCenters[second])
                if (distance > widestPixels) {
                    widestPixels = distance
                    widestPair = first to second
                }
            }
        }

        val pixelsPerInch = referencePixels / referenceInches
        return GroupMeasurementResult(
            sizeInches = widestPixels / pixelsPerInch,
            widestPair = widestPair,
            pixelsPerInch = pixelsPerInch
        )
    }

    private fun distance(first: MeasurementPoint, second: MeasurementPoint): Double =
        hypot(
            (second.x - first.x).toDouble(),
            (second.y - first.y).toDouble()
        )
}
