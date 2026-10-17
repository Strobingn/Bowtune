package com.strobingn.bowtune.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GroupMeasurementTest {
    @Test
    fun calculatesWidestCenterToCenterGroupFromCalibration() {
        val result = GroupMeasurement.calculate(
            referencePoints = listOf(
                MeasurementPoint(0f, 0f),
                MeasurementPoint(200f, 0f)
            ),
            referenceInches = 4.0,
            arrowCenters = listOf(
                MeasurementPoint(20f, 20f),
                MeasurementPoint(70f, 20f),
                MeasurementPoint(20f, 140f)
            )
        )

        requireNotNull(result)
        assertEquals(2.6, result.sizeInches, 0.0001)
        assertEquals(1 to 2, result.widestPair)
        assertEquals(50.0, result.pixelsPerInch, 0.0001)
    }

    @Test
    fun measurementIsIndependentOfImageScale() {
        val small = GroupMeasurement.calculate(
            listOf(MeasurementPoint(0f, 0f), MeasurementPoint(40f, 0f)),
            2.0,
            listOf(MeasurementPoint(5f, 5f), MeasurementPoint(25f, 5f))
        )
        val large = GroupMeasurement.calculate(
            listOf(MeasurementPoint(0f, 0f), MeasurementPoint(400f, 0f)),
            2.0,
            listOf(MeasurementPoint(50f, 50f), MeasurementPoint(250f, 50f))
        )

        assertEquals(1.0, requireNotNull(small).sizeInches, 0.0001)
        assertEquals(small.sizeInches, requireNotNull(large).sizeInches, 0.0001)
    }

    @Test
    fun rejectsMissingOrInvalidInputs() {
        val reference = listOf(MeasurementPoint(0f, 0f), MeasurementPoint(10f, 0f))
        val arrows = listOf(MeasurementPoint(0f, 0f), MeasurementPoint(2f, 0f))

        assertNull(GroupMeasurement.calculate(reference.take(1), 1.0, arrows))
        assertNull(GroupMeasurement.calculate(reference, 0.0, arrows))
        assertNull(GroupMeasurement.calculate(reference, 1.0, arrows.take(1)))
        assertNull(
            GroupMeasurement.calculate(
                listOf(MeasurementPoint(1f, 1f), MeasurementPoint(1f, 1f)),
                1.0,
                arrows
            )
        )
    }
}
