package com.optimistswe.mementolauncher.generator

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import com.optimistswe.mementolauncher.data.DotStyle
import com.optimistswe.mementolauncher.domain.CalendarMetrics
import com.optimistswe.mementolauncher.domain.LifeCalendarCalculator

/**
 * Generates Memento calendar images as bitmaps for use as wallpaper.
 *
 * This class uses Android's Canvas API to draw a grid of circles representing
 * weeks of life. Filled circles represent weeks lived, empty circles represent
 * future weeks.
 *
 * ## Design Philosophy
 * - Uses percentage-based sizing for universal screen compatibility
 * - Grid is always horizontally centered with equal margins
 * - Labels are positioned outside the grid area
 * - Accounts for lock screen UI elements (status bar, clock, bottom controls)
 *
 * @see CalendarConfig for customization options
 * @see CalendarMetrics for the input data
 */
class CalendarImageGenerator {

    // Reusable Paint and Path objects to avoid per-cell allocation
    private val paint = Paint().apply { isAntiAlias = true }
    private val labelPaint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }
    private val diamondPath = Path()

    /**
     * Generates a calendar bitmap based on the provided metrics and configuration.
     *
     * @param metrics Memento calendar metrics containing weeks lived/remaining
     * @param config Visual configuration for the calendar
     * @return A [Bitmap] ready to be set as wallpaper
     */
    fun generate(metrics: CalendarMetrics, config: CalendarConfig): Bitmap? {
        val safeWidth = config.width.coerceIn(1, 4096)
        val safeHeight = config.height.coerceIn(1, 8192)
        val bitmap = try {
            Bitmap.createBitmap(safeWidth, safeHeight, Bitmap.Config.RGB_565)
        } catch (e: OutOfMemoryError) {
            return null
        }
        val canvas = Canvas(bitmap)

        // Draw background
        canvas.drawColor(config.backgroundColor)

        // Calculate layout using safe dimensions
        val safeConfig = if (safeWidth != config.width || safeHeight != config.height) {
            config.copy(width = safeWidth, height = safeHeight)
        } else config
        val layout = calculateLayout(metrics.lifeExpectancy, safeConfig)

        // Draw in order: labels first, then grid on top
        drawLabels(canvas, layout, safeConfig)
        drawGrid(canvas, metrics, layout, safeConfig)

        return bitmap
    }

    /**
     * Calculates the complete layout with proper centering.
     *
     * Algorithm:
     * 1. Define safe zone (area where grid can be placed)
     * 2. Calculate optimal cell size to fit in safe zone
     * 3. Calculate actual grid dimensions
     * 4. Center grid horizontally within screen
     * 5. Position grid vertically in upper portion
     */
    private fun calculateLayout(lifeExpectancy: Int, config: CalendarConfig): Layout {
        val columns = LifeCalendarCalculator.WEEKS_PER_YEAR
        val rows = lifeExpectancy

        // Calculate safe zone using percentages of screen dimensions
        val topMargin = config.height * config.topMarginPercent
        val bottomMargin = config.height * config.bottomMarginPercent
        val margin = config.width * config.marginPercent

        // Exact bounds to align dynamically with 24.dp outer padded Compose layouts
        val contentLeft = margin
        val contentRight = config.width - margin

        // Available space for the grid (spanning full width between margins)
        val availableWidth = contentRight - contentLeft
        val availableHeight = config.height - topMargin - bottomMargin

        // Calculate cell size exactly to fit available width
        val exactCellWidth = (availableWidth - (columns - 1) * config.cellSpacing) / columns.toFloat()
        val cellSize = exactCellWidth.coerceAtLeast(config.minCellSize)

        // Calculate actual grid dimensions
        val gridWidth = columns * cellSize + (columns - 1) * config.cellSpacing
        val gridHeight = rows * cellSize + (rows - 1) * config.cellSpacing

        // Center grid horizontally perfectly aligned with UI margins
        val gridStartX = contentLeft

        // Center grid vertically within safe zone
        val verticalSpace = availableHeight - gridHeight
        val gridStartY = topMargin + (verticalSpace / 2)

        return Layout(
            gridStartX = gridStartX,
            gridStartY = gridStartY,
            gridWidth = gridWidth,
            gridHeight = gridHeight,
            cellSize = cellSize,
            columns = columns,
            rows = rows,
            contentLeft = contentLeft,
            contentRight = contentRight
        )
    }

    /**
     * Draws labels around the grid.
     *
     * Labels are positioned:
     * - "WEEK OF THE YEAR": Above grid, left-aligned to grid
     * - "MEMENTO": Above grid, right-aligned to grid
     * - "YEAR OF YOUR LIFE": Left of grid, vertical (rotated 90° CCW)
     */
    /**
     * Draws labels around the grid using dot-matrix style.
     */
    private fun drawLabels(canvas: Canvas, layout: Layout, config: CalendarConfig) {
        // Calculate dot parameters based on screen width to match text size
        // Standard text size is ~1.8% of width. 
        // A 5-row dot char is approx 5*dotSize + 4*spacing high.
        // Let's ensure the label height matches the intended text size.
        
        val intendedHeight = config.width * config.labelTextSizePercent
        // 5 rows of dots, 4 spaces. Let spacing be 30% of dotSize.
        // height = 5*d + 4*0.3*d = 6.2d => d = height/6.2
        
        val dotSize = intendedHeight / 6f
        val spacing = dotSize * 0.4f
        val color = config.labelColor

        val actualTextHeight = (5 * dotSize) + (4 * spacing)
        val labelY = layout.gridStartY - padding(dotSize) - actualTextHeight

        // Top-left label: "WEEK OF THE YEAR"
        drawDotText(
            canvas, "WEEK OF THE YEAR", layout.gridStartX, labelY, 
            color, dotSize, spacing
        )

        // Top-right label: "MEMENTO"
        val mementoWidth = measureDotText("MEMENTO", dotSize, spacing)
        drawDotText(
            canvas, "MEMENTO",
            layout.gridStartX + layout.gridWidth - mementoWidth, labelY,
            color, dotSize, spacing
        )

        // Left side label: "YEAR OF YOUR LIFE" (rotated 90° counter-clockwise)
        canvas.save()

        // Position at left of grid, strictly padding(dotSize) away from grid start
        val leftLabelX = layout.gridStartX - padding(dotSize) - (actualTextHeight / 2f)
        val leftLabelY = layout.gridStartY + (layout.gridHeight / 2)

        canvas.rotate(-90f, leftLabelX, leftLabelY)

        val yearLabelText = "YEAR OF YOUR LIFE"
        val yearLabelWidth = measureDotText(yearLabelText, dotSize, spacing)
        
        drawDotText(
            canvas, yearLabelText,
            leftLabelX - (yearLabelWidth / 2),
            leftLabelY - (intendedHeight / 2), // Adjust for height of text 
            color, dotSize, spacing
        )

        canvas.restore()
    }
    
    private fun padding(dotSize: Float): Float = dotSize * 3.5f

    /**
     * Draws the complete grid of week circles.
     */
    private fun drawGrid(
        canvas: Canvas,
        metrics: CalendarMetrics,
        layout: Layout,
        config: CalendarConfig
    ) {
        var weekIndex = 0

        for (row in 0 until layout.rows) {
            for (col in 0 until layout.columns) {
                val centerX = layout.gridStartX + col * (layout.cellSize + config.cellSpacing) + layout.cellSize / 2f
                val centerY = layout.gridStartY + row * (layout.cellSize + config.cellSpacing) + layout.cellSize / 2f
                val radius = (layout.cellSize / 2f) * 0.85f
                val isFilled = weekIndex < metrics.weeksLived

                paint.color = if (isFilled) config.filledColor else config.emptyColor

                when (config.dotStyle) {
                    DotStyle.FILLED_CIRCLE -> {
                        paint.style = if (isFilled) Paint.Style.FILL else Paint.Style.STROKE
                        paint.strokeWidth = config.emptyCircleStrokeWidth
                        canvas.drawCircle(centerX, centerY, radius, paint)
                    }
                    DotStyle.RING -> {
                        paint.style = Paint.Style.STROKE
                        paint.strokeWidth = if (isFilled) config.emptyCircleStrokeWidth * 2f else config.emptyCircleStrokeWidth
                        canvas.drawCircle(centerX, centerY, radius, paint)
                    }
                    DotStyle.SQUARE -> {
                        paint.style = if (isFilled) Paint.Style.FILL else Paint.Style.STROKE
                        paint.strokeWidth = config.emptyCircleStrokeWidth
                        val halfSize = radius * 0.9f
                        canvas.drawRect(centerX - halfSize, centerY - halfSize, centerX + halfSize, centerY + halfSize, paint)
                    }
                    DotStyle.DIAMOND -> {
                        paint.style = if (isFilled) Paint.Style.FILL else Paint.Style.STROKE
                        paint.strokeWidth = config.emptyCircleStrokeWidth
                        diamondPath.reset()
                        diamondPath.moveTo(centerX, centerY - radius)
                        diamondPath.lineTo(centerX + radius, centerY)
                        diamondPath.lineTo(centerX, centerY + radius)
                        diamondPath.lineTo(centerX - radius, centerY)
                        diamondPath.close()
                        canvas.drawPath(diamondPath, paint)
                    }
                }

                weekIndex++
            }
        }
    }

    private fun measureDotText(text: String, dotSize: Float, spacing: Float): Float {
        var width = 0f
        text.forEach { char ->
            val pattern = getCharPattern(char)
            val cols = if (pattern.isNotEmpty()) pattern[0].length else 0
            val charWidth = (cols * dotSize) + ((cols - 1).coerceAtLeast(0) * spacing)
            width += charWidth + spacing // Add spacing between chars
        }
        return if (width > 0) width - spacing else 0f // Remove trailing spacing
    }

    private fun drawDotText(
        canvas: Canvas,
        text: String,
        startX: Float,
        startY: Float,
        color: Int,
        dotSize: Float,
        spacing: Float
    ) {
        labelPaint.color = color

        var currentX = startX
        
        text.forEach { char ->
            if (char == ' ') {
                currentX += (dotSize * 2) + spacing
                return@forEach
            }
            
            val pattern = getCharPattern(char)
            val rows = pattern.size
            val cols = if (pattern.isNotEmpty()) pattern[0].length else 0
            val charWidth = (cols * dotSize) + ((cols - 1).coerceAtLeast(0) * spacing)
            
            // Draw pattern
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    if (pattern[r][c] == 'X') {
                        val cx = currentX + c * (dotSize + spacing) + dotSize / 2
                        val cy = startY + r * (dotSize + spacing) + dotSize / 2
                        canvas.drawCircle(cx, cy, dotSize / 2, labelPaint)
                    }
                }
            }
            
            currentX += charWidth + spacing
        }
    }

    private fun getCharPattern(char: Char): List<String> {
        return when (char.uppercaseChar()) {
            'A' -> listOf(".XX.", "X..X", "XXXX", "X..X", "X..X")
            'B' -> listOf("XXX.", "X..X", "XXX.", "X..X", "XXX.")
            'C' -> listOf(".XXX", "X...", "X...", "X...", ".XXX")
            'D' -> listOf("XX..", "X.X.", "X.X.", "X.X.", "XX..")
            'E' -> listOf("XXXX", "X...", "XXX.", "X...", "XXXX")
            'F' -> listOf("XXXX", "X...", "XXX.", "X...", "X...")
            'G' -> listOf(".XXX", "X...", "X.XX", "X..X", ".XX.")
            'H' -> listOf("X..X", "X..X", "XXXX", "X..X", "X..X")
            'I' -> listOf("XXX", ".X.", ".X.", ".X.", "XXX")
            'J' -> listOf("..XX", "...X", "...X", "X..X", ".XX.")
            'K' -> listOf("X..X", "X.X.", "XX..", "X.X.", "X..X")
            'L' -> listOf("X...", "X...", "X...", "X...", "XXXX")
            'M' -> listOf("X...X", "XX.XX", "X.X.X", "X...X", "X...X")
            'N' -> listOf("X..X", "XX.X", "X.XX", "X..X", "X..X")
            'O' -> listOf(".XX.", "X..X", "X..X", "X..X", ".XX.")
            'P' -> listOf("XXX.", "X..X", "XXX.", "X...", "X...")
            'Q' -> listOf(".XX.", "X..X", "X..X", ".XX.", "...X")
            'R' -> listOf("XXX.", "X..X", "XXX.", "X.X.", "X..X")
            'S' -> listOf(".XXX", "X...", ".XX.", "...X", "XXX.")
            'T' -> listOf("XXX", ".X.", ".X.", ".X.", ".X.")
            'U' -> listOf("X..X", "X..X", "X..X", "X..X", ".XX.")
            'V' -> listOf("X...X", "X...X", ".X.X.", ".X.X.", "..X..")
            'W' -> listOf("X...X", "X...X", "X.X.X", "XX.XX", "X...X")
            'X' -> listOf("X...X", ".X.X.", "..X..", ".X.X.", "X...X")
            'Y' -> listOf("X..X", ".XX.", "..X.", "..X.", "..X.")
            'Z' -> listOf("XXXX", "...X", "..X.", ".X..", "XXXX")
            ' ' -> listOf("..") // Spacer
            else -> listOf("....", ".XX.", "....", ".XX.", "....") // Default
        }
    }
}

/**
 * Layout parameters calculated by the generator.
 */
private data class Layout(
    val gridStartX: Float,
    val gridStartY: Float,
    val gridWidth: Float,
    val gridHeight: Float,
    val cellSize: Float,
    val columns: Int,
    val rows: Int,
    val contentLeft: Float,
    val contentRight: Float
)

/**
 * Configuration for calendar image generation.
 *
 * Uses percentage-based values for universal screen compatibility.
 * All percentage values are 0.0 to 1.0 (e.g., 0.05 = 5%).
 *
 * @property width Canvas width in pixels
 * @property height Canvas height in pixels
 * @property backgroundColor Background color
 * @property filledColor Color for lived weeks
 * @property emptyColor Color for remaining weeks (outline)
 * @property labelColor Color for text labels
 * @property topMarginPercent Top margin as percentage of height (for status bar + clock)
 * @property bottomMarginPercent Bottom margin as percentage of height (for lock screen UI)
 * @property sideMarginPercent Side margins as percentage of width
 * @property labelSpacePercent Space for left vertical label as percentage of width
 * @property labelTextSizePercent Label text size as percentage of width
 * @property cellSpacing Fixed spacing between cells (looks consistent across screens)
 * @property minCellSize Minimum cell size to ensure visibility
 * @property emptyCircleStrokeWidth Stroke width for empty circles
 */
data class CalendarConfig(
    val width: Int,
    val height: Int,
    val backgroundColor: Int = 0xFF000000.toInt(),
    val filledColor: Int = 0xFFFFFFFF.toInt(),          // Bright white for filled
    val emptyColor: Int = 0xFF999999.toInt(),           // Lighter gray for better visibility
    val labelColor: Int = 0xFF888888.toInt(),           // Labels slightly dimmer
    // Percentage-based layout (universal across screen sizes)
    val topMarginPercent: Float = 0.15f,         // Adjusted for better balance
    val bottomMarginPercent: Float = 0.15f,      // 15% from bottom (shifted down)
    val marginPercent: Float = 0.09f,           // Exactly aligns with 24.dp in Compose Padding
    val labelTextSizePercent: Float = 0.018f,    // 1.8% of width for label text
    // Fixed values (look consistent)
    val cellSpacing: Float = 2f,                 // Tighter spacing for grid
    val minCellSize: Float = 5f,                 // Minimum cell size
    val emptyCircleStrokeWidth: Float = 1.5f,    // Thicker stroke for visibility
    val dotStyle: DotStyle = DotStyle.FILLED_CIRCLE
)
