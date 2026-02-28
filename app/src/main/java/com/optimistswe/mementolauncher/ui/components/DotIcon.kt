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

/**
 * Types of custom icons supported by the DotIcon component.
 */
enum class DotIconType {
    EDIT,
    HEART,
    STAR,
    SETTINGS,
    PHONE,
    MESSAGE,
    GLOBE,
    CALENDAR,
    CAMERA,
    CALCULATOR,
    MAPS,
    PHOTOS
}

/**
 * A custom icon component that renders shapes using a dot matrix style
 * to match the Memento aesthetic.
 */
@Composable
fun DotIcon(
    type: DotIconType,
    color: Color,
    modifier: Modifier = Modifier,
    dotSize: Dp = 3.dp,
    spacing: Dp = 1.dp
) {
    val fontScale = com.optimistswe.mementolauncher.ui.components.LocalFontScale.current
    val scaledDotSize = dotSize * fontScale
    val scaledSpacing = spacing * fontScale

    val pattern = remember(type) { getIconPattern(type) }
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

private fun getIconPattern(type: DotIconType): List<String> {
    return when (type) {
        DotIconType.EDIT -> listOf(
            "....XX.",
            "...X..X",
            "..X..X.",
            ".X..X..",
            "X..X...",
            "XXX....",
            "XX....."
        )
        DotIconType.HEART -> listOf(
            " . X . X . ",
            " X X X X X ",
            " X X X X X ",
            " . X X X . ",
            " . . X . . "
        )
        DotIconType.STAR -> listOf(
            " . . X . . ",
            " . X X X . ",
            " X X X X X ",
            " . X X X . ",
            " X . . . X "
        )
        DotIconType.SETTINGS -> listOf(
            "                         ",
            "           XXX           ",
            "           XXX           ",
            "           XXX           ",
            "     XX    XXX    XX     ",
            "    XXX  XXXXXXX  XXX    ",
            "    XXXXXXXXXXXXXXXXX    ",
            "      XXXXXXXXXXXXX      ",
            "      XXXX     XXXX      ",
            "     XXXX       XXXX     ",
            "     XXX         XXX     ",
            " XXXXXXX         XXXXXXX ",
            " XXXXXXX         XXXXXXX ",
            " XXXXXXX         XXXXXXX ",
            "     XXX         XXX     ",
            "     XXXX       XXXX     ",
            "      XXXX     XXXX      ",
            "      XXXXXXXXXXXXX      ",
            "    XXXXXXXXXXXXXXXXX    ",
            "    XXX  XXXXXXX  XXX    ",
            "     XX    XXX    XX     ",
            "           XXX           ",
            "           XXX           ",
            "           XXX           ",
            "                         "
        )

        // ═══════════════════════════════════════════
        // DOCK ICONS — Crisp designs for small rendering
        // ═══════════════════════════════════════════

        // Phone: classic curved handset
        DotIconType.PHONE -> listOf(
            "XXX......",
            "X.X......",
            "XXX......",
            ".XX......",
            "..XX.....",
            "...XX....",
            "....X.XXX",
            "......X.X",
            "......XXX"
        )

        // Chat bubble with tail
        DotIconType.MESSAGE -> listOf(
            ".XXXXXXX.",
            "XXXXXXXXX",
            "X.XXXXX.X",
            "XXXXXXXXX",
            "X.XXX...X",
            "XXXXXXXXX",
            ".XXXXXXXX",
            "..XX.....",
            "..X......"
        )

        // Camera: body outline with viewfinder bump and circular lens
        DotIconType.CAMERA -> listOf(
            "...XXXX....",
            "XXXXXXXXXXX",
            "X..XXXXX..X",
            "X.XX...XX.X",
            "X.X..X..X.X",
            "X.XX...XX.X",
            "X..XXXXX..X",
            "XXXXXXXXXXX",
            "..........."
        )

        // Calculator: screen on top, button grid below
        DotIconType.CALCULATOR -> listOf(
            ".XXXXXXX.",
            ".X.XXX.X.",
            ".X.XXX.X.",
            ".X.....X.",
            ".X.X.X.X.",
            ".X.....X.",
            ".X.X.X.X.",
            ".X.....X.",
            ".XXXXXXX."
        )

        // Maps: location pin marker
        DotIconType.MAPS -> listOf(
            "...XXX...",
            "..XXXXX..",
            ".XX...XX.",
            ".XX.X.XX.",
            ".XX...XX.",
            "..XXXXX..",
            "...XXX...",
            "....X....",
            "....X...."
        )

        // Photos: sun + mountain landscape in frame
        DotIconType.PHOTOS -> listOf(
            "XXXXXXXXX",
            "X..X....X",
            "X.X.X...X",
            "X..X....X",
            "X.....X.X",
            "X....X.XX",
            "X.X.X.X.X",
            "XX.X..XXX",
            "XXXXXXXXX"
        )

        DotIconType.GLOBE -> listOf(
            "..XXX..",
            ".X.X.X.",
            "XXXXXXX",
            "X.X.X.X",
            "XXXXXXX",
            ".X.X.X.",
            "..XXX.."
        )
        DotIconType.CALENDAR -> listOf(
            "X.X.X.X",
            "XXXXXXX",
            "X.....X",
            "X.X.X.X",
            "X.....X",
            "XXXXXXX"
        )
    }
}
