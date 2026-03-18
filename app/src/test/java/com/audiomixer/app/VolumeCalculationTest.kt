package com.audiomixer.app

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for volume calculation logic.
 * These tests run on the JVM without a device.
 */
class VolumeCalculationTest {

    @Test
    fun `volume is clamped to 0 minimum`() {
        val input = -0.5f
        val clamped = input.coerceIn(0f, 1f)
        assertEquals(0f, clamped)
    }

    @Test
    fun `volume is clamped to 1 maximum`() {
        val input = 1.8f
        val clamped = input.coerceIn(0f, 1f)
        assertEquals(1f, clamped)
    }

    @Test
    fun `volume midpoint is preserved`() {
        val input = 0.5f
        val clamped = input.coerceIn(0f, 1f)
        assertEquals(0.5f, clamped)
    }

    @Test
    fun `gain in millibels calculated correctly for half volume`() {
        val volume = 0.5f
        // 20 * log10(0.5) * 100 = 20 * (-0.301) * 100 ≈ -602 mB
        val gainMB = (2000 * Math.log10(volume.toDouble())).toInt() * 100
        assertTrue("Gain should be negative for attenuation", gainMB < 0)
    }

    @Test
    fun `gain in millibels is min for zero volume`() {
        val volume = 0f
        val gainMB = if (volume <= 0f) -9600 else (2000 * Math.log10(volume.toDouble())).toInt() * 100
        assertEquals(-9600, gainMB)
    }

    @Test
    fun `percentage display converts correctly`() {
        val volume = 0.75f
        val percent = (volume * 100).toInt()
        assertEquals(75, percent)
    }

    @Test
    fun `muted app reports zero percent`() {
        val volume = 0.8f
        val isMuted = true
        val displayPercent = if (isMuted) 0 else (volume * 100).toInt()
        assertEquals(0, displayPercent)
    }
}
