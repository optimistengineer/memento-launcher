package com.optimistswe.mementolauncher.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.optimistswe.mementolauncher.data.DotStyle

/**
 * A specialized icon component that renders a dot-matrix representation 
 * of the various DotStyle aesthetics.
 */
@Composable
fun DotStyleIcon(
    style: DotStyle,
    color: Color,
    modifier: Modifier = Modifier,
    dotSize: Dp = 2.5.dp,
    spacing: Dp = 1.dp
) {
    val fontScale = com.optimistswe.mementolauncher.ui.components.LocalFontScale.current
    val scaledDotSize = dotSize * fontScale
    val scaledSpacing = spacing * fontScale

    val pattern = remember(style) { getStylePattern(style) }
    val rows = pattern.size
    val cols = if (pattern.isNotEmpty()) pattern[0].length else 0
    
    val width = (scaledDotSize * cols) + (scaledSpacing * (cols - 1))
    val height = (scaledDotSize * rows) + (scaledSpacing * (rows - 1))

    Canvas(modifier = modifier.size(width, height)) {
        val dotPx = scaledDotSize.toPx()
        val spacePx = scaledSpacing.toPx()
        val radius = dotPx / 2f

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                if (pattern[r][c] == 'X') {
                    val x = c * (dotPx + spacePx) + radius
                    val y = r * (dotPx + spacePx) + radius
                    drawCircle(
                        color = color,
                        radius = radius,
                        center = Offset(x, y)
                    )
                }
            }
        }
    }
}

private fun getStylePattern(style: DotStyle): List<String> {
    return when (style) {
        DotStyle.FILLED_CIRCLE -> listOf(
            ".XXX.",
            "XXXXX",
            "XXXXX",
            "XXXXX",
            ".XXX."
        )
        DotStyle.RING -> listOf(
            ".XXX.",
            "X...X",
            "X...X",
            "X...X",
            ".XXX."
        )
        DotStyle.SQUARE -> listOf(
            "XXXXX",
            "XXXXX",
            "XXXXX",
            "XXXXX",
            "XXXXX"
        )
        DotStyle.DIAMOND -> listOf(
            "..X..",
            ".XXX.",
            "XXXXX",
            ".XXX.",
            "..X.."
        )
    }
}
