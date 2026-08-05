package com.betteruniverse.mementolauncher.generator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [calculateLayout].
 *
 * The layout is pure float math with no Android dependencies, so it can be exercised directly
 * without a device. These cover the safe-zone contract: the grid must stay inside the top and
 * bottom margins reserved for lock screen UI at every supported life expectancy.
 */
class CalendarLayoutTest {

    // A typical 1080x2400 phone.
    private val config = CalendarConfig(width = 1080, height = 2400)

    private fun safeZone(config: CalendarConfig): Pair<Float, Float> {
        val top = config.height * config.topMarginPercent
        val bottom = config.height - config.height * config.bottomMarginPercent
        return top to bottom
    }

    @Test
    fun `grid fits within the vertical safe zone at the default life expectancy`() {
        val layout = calculateLayout(80, config)
        val (top, bottom) = safeZone(config)

        assertTrue(
            "grid starts above the safe zone: ${layout.gridStartY} < $top",
            layout.gridStartY >= top - 0.01f
        )
        assertTrue(
            "grid ends below the safe zone: ${layout.gridStartY + layout.gridHeight} > $bottom",
            layout.gridStartY + layout.gridHeight <= bottom + 0.01f
        )
    }

    @Test
    fun `grid fits within the vertical safe zone at the maximum life expectancy`() {
        // Regression: cell size used to be derived from width alone, so 120 rows overflowed
        // the safe zone by roughly 366px on a 1080x2400 screen and ran under the lock screen UI.
        val layout = calculateLayout(120, config)
        val (top, bottom) = safeZone(config)

        assertTrue(
            "grid starts above the safe zone: ${layout.gridStartY} < $top",
            layout.gridStartY >= top - 0.01f
        )
        assertTrue(
            "grid ends below the safe zone: ${layout.gridStartY + layout.gridHeight} > $bottom",
            layout.gridStartY + layout.gridHeight <= bottom + 0.01f
        )
    }

    @Test
    fun `grid fits within the vertical safe zone across the whole supported range`() {
        val (top, bottom) = safeZone(config)
        for (lifeExpectancy in 50..120) {
            val layout = calculateLayout(lifeExpectancy, config)
            assertTrue(
                "lifeExpectancy=$lifeExpectancy starts above safe zone",
                layout.gridStartY >= top - 0.01f
            )
            assertTrue(
                "lifeExpectancy=$lifeExpectancy ends below safe zone",
                layout.gridStartY + layout.gridHeight <= bottom + 0.01f
            )
        }
    }

    @Test
    fun `grid never exceeds the horizontal content band`() {
        for (lifeExpectancy in 50..120) {
            val layout = calculateLayout(lifeExpectancy, config)
            assertTrue(
                "lifeExpectancy=$lifeExpectancy starts left of the content band",
                layout.gridStartX >= layout.contentLeft - 0.01f
            )
            assertTrue(
                "lifeExpectancy=$lifeExpectancy overflows the content band",
                layout.gridStartX + layout.gridWidth <= layout.contentRight + 0.01f
            )
        }
    }

    @Test
    fun `grid still spans the full content width when width is the binding constraint`() {
        // At 80 rows width is the limiting axis, so the fix must not shrink the existing layout.
        val layout = calculateLayout(80, config)
        val availableWidth = layout.contentRight - layout.contentLeft

        assertEquals(layout.contentLeft, layout.gridStartX, 0.01f)
        assertEquals(availableWidth, layout.gridWidth, 0.01f)
    }

    @Test
    fun `grid is horizontally centred when height is the binding constraint`() {
        val layout = calculateLayout(120, config)
        val leftGap = layout.gridStartX - layout.contentLeft
        val rightGap = layout.contentRight - (layout.gridStartX + layout.gridWidth)

        assertEquals("grid should be centred in the content band", leftGap, rightGap, 0.01f)
    }

    @Test
    fun `row count matches life expectancy`() {
        assertEquals(80, calculateLayout(80, config).rows)
        assertEquals(120, calculateLayout(120, config).rows)
    }

    @Test
    fun `zero life expectancy does not divide by zero`() {
        // Guards a corrupt restored preference reaching the layout math.
        val layout = calculateLayout(0, config)

        assertEquals(1, layout.rows)
        assertTrue("cellSize should be finite", layout.cellSize.isFinite())
        assertTrue("gridHeight should be finite", layout.gridHeight.isFinite())
    }
}
