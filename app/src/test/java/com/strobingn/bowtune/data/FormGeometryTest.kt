package com.strobingn.bowtune.data

import org.junit.Assert.assertEquals
import org.junit.Test

class FormGeometryTest {
    @Test
    fun shoulderTiltDoesNotDependOnLeftRightImageOrdering() {
        assertEquals(0f, FormGeometry.lineTiltDegrees(100f, 0f), 0.001f)
        assertEquals(0f, FormGeometry.lineTiltDegrees(-100f, 0f), 0.001f)
        assertEquals(
            FormGeometry.lineTiltDegrees(100f, 20f),
            FormGeometry.lineTiltDegrees(-100f, -20f),
            0.001f
        )
    }

    @Test
    fun shoulderTiltReturnsAcuteAngle() {
        assertEquals(45f, FormGeometry.lineTiltDegrees(-100f, 100f), 0.001f)
        assertEquals(90f, FormGeometry.lineTiltDegrees(0f, 100f), 0.001f)
    }
}
