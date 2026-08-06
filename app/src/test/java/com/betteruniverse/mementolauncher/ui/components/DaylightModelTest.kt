package com.betteruniverse.mementolauncher.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The daylight sky is a pure function of the minute of day, which makes the whole 24h cycle
 * testable on the JVM — including the property that actually matters for a launcher: however
 * bright the sky gets, the white dot-matrix text on top of it must stay readable.
 */
class DaylightModelTest {

    private fun channels(argb: Long) = Triple(
        (argb shr 16 and 0xFF).toInt(), (argb shr 8 and 0xFF).toInt(), (argb and 0xFF).toInt()
    )

    @Test
    fun `deep night is full stars on black`() {
        val spec = DaylightModel.specFor(2 * 60)
        assertEquals(1.0f, spec.starAlpha, 0.01f)
        assertEquals(0xFF000000, spec.zenithArgb)
    }

    @Test
    fun `midday has no stars and is the brightest point of the day`() {
        val noon = DaylightModel.specFor(13 * 60)
        assertEquals(0.0f, noon.starAlpha, 0.01f)
        val noonSum = channels(noon.horizonArgb).toList().sum()
        for (m in intArrayOf(0, 3 * 60, 6 * 60, 9 * 60, 16 * 60, 20 * 60, 23 * 60)) {
            val other = channels(DaylightModel.specFor(m).horizonArgb).toList().sum()
            assertTrue("horizon at ${m / 60}:00 ($other) should not outshine noon ($noonSum)", other <= noonSum)
        }
    }

    @Test
    fun `dawn brightens monotonically and fades the stars`() {
        var lastLight = -1
        var lastStars = 2f
        for (m in (5 * 60)..(8 * 60) step 15) {
            val spec = DaylightModel.specFor(m)
            val light = channels(spec.zenithArgb).toList().sum() + channels(spec.horizonArgb).toList().sum()
            assertTrue("stars must not increase through dawn ($m)", spec.starAlpha <= lastStars + 0.001f)
            lastStars = spec.starAlpha
            // Light may plateau (keyframes), but must never go backwards during sunrise.
            if (m >= 5 * 60 + 30) {
                assertTrue("sky must not darken during sunrise ($m): $light < $lastLight", light >= lastLight)
            }
            lastLight = light
        }
        assertEquals(0.0f, DaylightModel.specFor(8 * 60).starAlpha, 0.01f)
    }

    @Test
    fun `sunset horizon is warm - red leads blue`() {
        val (r, _, b) = channels(DaylightModel.specFor(19 * 60).horizonArgb)
        assertTrue("sunset horizon should glow warm (r=$r, b=$b)", r > b * 2)
    }

    @Test
    fun `the cycle is continuous across midnight`() {
        val before = DaylightModel.specFor(23 * 60 + 59)
        val after = DaylightModel.specFor(0)
        assertEquals(before.starAlpha, after.starAlpha, 0.05f)
        val (r1, g1, b1) = channels(before.horizonArgb)
        val (r2, g2, b2) = channels(after.horizonArgb)
        assertTrue(Math.abs(r1 - r2) <= 2 && Math.abs(g1 - g2) <= 2 && Math.abs(b1 - b2) <= 2)
    }

    @Test
    fun `no minute of the day may exceed the contrast ceiling`() {
        // MAX_CHANNEL = 0x40 keeps white text above ~7:1 against the sky. Sweeping every minute
        // means a future keyframe edit that washes out the launcher fails here, not on a phone.
        for (m in 0 until 24 * 60) {
            val spec = DaylightModel.specFor(m)
            for (argb in longArrayOf(spec.zenithArgb, spec.horizonArgb)) {
                val (r, g, b) = channels(argb)
                assertTrue(
                    "minute $m exceeds MAX_CHANNEL: r=$r g=$g b=$b",
                    r <= DaylightModel.MAX_CHANNEL && g <= DaylightModel.MAX_CHANNEL && b <= DaylightModel.MAX_CHANNEL
                )
            }
        }
    }

    @Test
    fun `star alpha is always within 0 and 1`() {
        for (m in 0 until 24 * 60 step 5) {
            val a = DaylightModel.specFor(m).starAlpha
            assertTrue("starAlpha out of range at $m: $a", a in 0.0f..1.0f)
        }
    }
}
