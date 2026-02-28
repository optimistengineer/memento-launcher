package com.optimistswe.mementolauncher.generator

import com.optimistswe.mementolauncher.data.DotStyle
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for [CalendarConfig] default values and construction.
 */
class CalendarConfigTest {

    @Test
    fun `default backgroundColor is black`() {
        val config = CalendarConfig(width = 1080, height = 2400)
        assertEquals(0xFF000000.toInt(), config.backgroundColor)
    }

    @Test
    fun `default filledColor is white`() {
        val config = CalendarConfig(width = 1080, height = 2400)
        assertEquals(0xFFFFFFFF.toInt(), config.filledColor)
    }

    @Test
    fun `default emptyColor is gray`() {
        val config = CalendarConfig(width = 1080, height = 2400)
        assertEquals(0xFF999999.toInt(), config.emptyColor)
    }

    @Test
    fun `default labelColor is dimmer gray`() {
        val config = CalendarConfig(width = 1080, height = 2400)
        assertEquals(0xFF888888.toInt(), config.labelColor)
    }

    @Test
    fun `default dotStyle is FILLED_CIRCLE`() {
        val config = CalendarConfig(width = 1080, height = 2400)
        assertEquals(DotStyle.FILLED_CIRCLE, config.dotStyle)
    }

    @Test
    fun `custom dotStyle is preserved`() {
        val config = CalendarConfig(width = 1080, height = 2400, dotStyle = DotStyle.DIAMOND)
        assertEquals(DotStyle.DIAMOND, config.dotStyle)
    }

    @Test
    fun `width and height are preserved`() {
        val config = CalendarConfig(width = 720, height = 1280)
        assertEquals(720, config.width)
        assertEquals(1280, config.height)
    }

    @Test
    fun `topMarginPercent default is 0_15f`() {
        val config = CalendarConfig(width = 1080, height = 2400)
        assertEquals(0.15f, config.topMarginPercent, 0.001f)
    }

    @Test
    fun `bottomMarginPercent default is 0_15f`() {
        val config = CalendarConfig(width = 1080, height = 2400)
        assertEquals(0.15f, config.bottomMarginPercent, 0.001f)
    }

    @Test
    fun `marginPercent default is 0_09f`() {
        val config = CalendarConfig(width = 1080, height = 2400)
        assertEquals(0.09f, config.marginPercent, 0.001f)
    }

    @Test
    fun `cellSpacing default is 2f`() {
        val config = CalendarConfig(width = 1080, height = 2400)
        assertEquals(2f, config.cellSpacing, 0.001f)
    }

    @Test
    fun `minCellSize default is 5f`() {
        val config = CalendarConfig(width = 1080, height = 2400)
        assertEquals(5f, config.minCellSize, 0.001f)
    }

    @Test
    fun `emptyCircleStrokeWidth default is 1_5f`() {
        val config = CalendarConfig(width = 1080, height = 2400)
        assertEquals(1.5f, config.emptyCircleStrokeWidth, 0.001f)
    }

    @Test
    fun `labelTextSizePercent default is 0_018f`() {
        val config = CalendarConfig(width = 1080, height = 2400)
        assertEquals(0.018f, config.labelTextSizePercent, 0.001f)
    }

    @Test
    fun `copy with overrides works correctly`() {
        val original = CalendarConfig(width = 1080, height = 2400)
        val modified = original.copy(
            backgroundColor = 0xFFFFFFFF.toInt(),
            filledColor = 0xFF000000.toInt(),
            dotStyle = DotStyle.SQUARE
        )
        assertEquals(0xFFFFFFFF.toInt(), modified.backgroundColor)
        assertEquals(0xFF000000.toInt(), modified.filledColor)
        assertEquals(DotStyle.SQUARE, modified.dotStyle)
        // Unchanged values preserved
        assertEquals(1080, modified.width)
        assertEquals(2400, modified.height)
    }

    @Test
    fun `all DotStyle values can be set`() {
        DotStyle.entries.forEach { style ->
            val config = CalendarConfig(width = 100, height = 100, dotStyle = style)
            assertEquals(style, config.dotStyle)
        }
    }
}
