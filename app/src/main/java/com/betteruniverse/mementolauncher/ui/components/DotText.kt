package com.betteruniverse.mementolauncher.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.compositionLocalOf

/**
 * Composition local to provide the current font scale across the entire app
 * without needing to pass it to every DotText instance manually.
 */
val LocalFontScale = compositionLocalOf { 1.0f }

/** Largest total text scale the dot-matrix layouts are designed to survive. */
internal const val MAX_EFFECTIVE_FONT_SCALE = 1.5f

/**
 * Combines the user's in-app text-size preference with the device's system font scale.
 *
 * The app used to honour its own SMALL/MEDIUM/LARGE setting *only*. Someone who had raised the
 * system font size — the standard accommodation for low vision, and the one people actually
 * reach for — saw absolutely no change here, because every glyph is drawn as raw dots on a
 * Canvas and so is invisible to the platform's text scaling. Reading the setting explicitly is
 * the only way custom-drawn text can respect it.
 *
 * The product is capped: the two multiply (both express the same intent, so a user who has asked
 * for large text twice gets more of it), but the total is clamped, because dot-matrix strings
 * neither reflow nor wrap and past roughly 1.5x the longer labels stop fitting the narrow
 * screens this app supports. Clamping trades a little size at the extreme for text that is still
 * on-screen — [AutoScaledDotText] then shrinks anything that would still overflow.
 */
@Composable
fun effectiveFontScale(): Float {
    val systemScale = LocalDensity.current.fontScale
    return (LocalFontScale.current * systemScale).coerceIn(0.8f, MAX_EFFECTIVE_FONT_SCALE)
}

/**
 * A Text composable that renders characters using a dot matrix style
 * to match the Memento wallpaper aesthetic.
 * 
 * Optimized to use a single Canvas for the entire text block, reducing
 * layout overhead and draw calls.
 */
@Composable
fun DotText(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    dotSize: Dp = 3.dp,
    spacing: Dp = 1.dp,
    alignment: Alignment.Horizontal = Alignment.Start
) {
    val fontScale = effectiveFontScale()
    val scaledDotSize = dotSize * fontScale
    val scaledSpacing = spacing * fontScale

    // Expensive layout calculations remembered to avoid re-computing on every recomposition
    val layout = remember(text, scaledDotSize, scaledSpacing) {
        calculateLayout(text, scaledDotSize, scaledSpacing)
    }

    // The glyphs are drawn as raw circles on a Canvas, so without this the text is invisible to
    // accessibility services — the whole launcher surfaces to TalkBack as blank, unlabelled boxes.
    // Blank strings are left unlabelled so they do not become empty focus stops.
    val accessibilityModifier = remember(text) {
        if (text.isNotBlank()) Modifier.semantics { contentDescription = text } else Modifier
    }

    Canvas(
        modifier = modifier
            .size(layout.width, layout.height)
            .then(accessibilityModifier)
    ) {
        val dotPx = scaledDotSize.toPx()
        val spacePx = scaledSpacing.toPx()
        val radius = dotPx / 2f

        // `size(layout.width, ...)` is coerced by the parent's constraints, so when the text is
        // wider than the space it was given the node ends up narrower than the glyphs it is about
        // to draw. DrawScope does not clip, so those glyphs used to paint straight over whatever
        // sat outside this node — including, for a HorizontalPager page, the *neighbouring page*:
        // the calendar page's "SET YOUR BIRTH DATE" hint overflowed on any screen under ~410dp
        // and left its final "E" stranded in the home screen's left gutter.
        // A draw-time clipRect keeps overflow local without the extra render layer that
        // Modifier.clipToBounds() (a graphicsLayer) would add to every DotText in a long list.
        clipRect {
            var currentY = 0f

            layout.lines.forEach { line ->
                // Handle horizontal alignment within the Canvas
                var currentX = when (alignment) {
                    Alignment.CenterHorizontally -> (size.width - line.width.toPx()) / 2f
                    Alignment.End -> size.width - line.width.toPx()
                    else -> 0f
                }

                line.chars.forEach { charItem ->
                    if (charItem.char == ' ') {
                        currentX += (dotPx * 2) + dotPx * 1.5f // Match spacing between chars
                    } else {
                        val pattern = charItem.pattern
                        val charRows = pattern.size
                        val charCols = pattern[0].length

                        // Sit every glyph on a shared baseline. Laying out from the top of the line
                        // instead makes any glyph shorter than the tallest one float, and any taller
                        // one hang below its neighbours. The font is uniformly 5 rows today, so this
                        // is normally zero — it keeps mixed-height glyphs correct if any are added.
                        val baselineOffset = (line.maxRows - charRows) * (dotPx + spacePx)

                        for (r in 0 until charRows) {
                            for (c in 0 until charCols) {
                                if (pattern[r][c] == 'X') {
                                    val x = currentX + c * (dotPx + spacePx) + radius
                                    val y = currentY + baselineOffset + r * (dotPx + spacePx) + radius
                                    drawCircle(
                                        color = color,
                                        radius = radius,
                                        center = Offset(x, y)
                                    )
                                }
                            }
                        }
                        currentX += (charCols * dotPx) + ((charCols - 1).coerceAtLeast(0) * spacePx) + dotPx * 1.5f
                    }
                }
                currentY += line.height.toPx() + (dotPx * 2) // Line spacing
            }
        }
    }
}

/**
 * A Text composable that scales down its dotSize and spacing to fit its width on a single line.
 */
@Composable
fun AutoScaledDotText(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    baseDotSize: Dp = 6.dp,
    baseSpacing: Dp = 1.5.dp,
    alignment: Alignment.Horizontal = Alignment.CenterHorizontally
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        // Honour `alignment` for the block's position, not just for wrapped lines inside it.
        // This Box fills the width, so pinning it to Center made every caller centred no matter
        // what they asked for — a Start-aligned caller silently came out centred.
        contentAlignment = when (alignment) {
            Alignment.End -> Alignment.CenterEnd
            Alignment.Start -> Alignment.CenterStart
            else -> Alignment.Center
        }
    ) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        val maxWidthDp = with(density) { constraints.maxWidth.toDp() }
        
        val fontScale = effectiveFontScale()
        val scaledDotSize = baseDotSize * fontScale
        val scaledSpacing = baseSpacing * fontScale

        // Cache the layout calculation to avoid computing it twice
        val requiredSize = remember(text, scaledDotSize, scaledSpacing) {
            calculateLayout(text, scaledDotSize, scaledSpacing)
        }

        val scale = if (requiredSize.width > maxWidthDp && maxWidthDp > 0.dp) {
            (maxWidthDp.value / requiredSize.width.value) * 0.95f
        } else {
            1f
        }

        // Pass base sizes * scale so DotText applies fontScale only once
        DotText(
            text = text,
            color = color,
            dotSize = baseDotSize * scale,
            spacing = baseSpacing * scale,
            alignment = alignment
        )
    }
}

internal data class TextLayout(
    val width: Dp,
    val height: Dp,
    val lines: List<LineLayout>
)

internal data class LineLayout(
    val width: Dp,
    val height: Dp,
    /** Tallest glyph on the line, in dot rows. Used to sit every glyph on a shared baseline. */
    val maxRows: Int,
    val chars: List<CharLayout>
)

internal data class CharLayout(
    val char: Char,
    val pattern: List<String>
)

internal fun calculateLayout(text: String, dotSize: Dp, spacing: Dp): TextLayout {
    val lines = text.split("\n")
    val lineLayouts = lines.map { line ->
        var lineWidth = 0.dp
        var maxHeight = 0.dp
        var maxRows = 0
        val charLayouts = line.mapIndexed { index, char ->
            val pattern = getPattern(char)
            val charLayout = CharLayout(char, pattern)

            if (char == ' ') {
                lineWidth += (dotSize * 3.5f)
            } else {
                val rows = pattern.size
                val cols = pattern[0].length
                lineWidth += (dotSize * cols) + (spacing * (cols - 1))
                val charHeight = (dotSize * rows) + (spacing * (rows - 1))
                if (charHeight > maxHeight) maxHeight = charHeight
                if (rows > maxRows) maxRows = rows
            }

            if (index < line.length - 1) {
                lineWidth += dotSize * 1.5f // space between chars
            }
            charLayout
        }
        LineLayout(lineWidth, maxHeight, maxRows, charLayouts)
    }

    val totalWidth = lineLayouts.maxOfOrNull { it.width.value }?.dp ?: 0.dp
    val totalHeight = lineLayouts.sumOf { it.height.value.toDouble() }.toFloat().dp + 
                     ((lineLayouts.size - 1).coerceAtLeast(0) * (dotSize.value * 2)).dp
    
    return TextLayout(totalWidth, totalHeight, lineLayouts)
}

internal fun getPattern(char: Char): List<String> {
    return when (char.uppercaseChar()) {
        // Digits are 5 rows to match the letters' cap height. They used to be 6, and because
        // glyphs are laid out from the top of the line, every digit dropped one dot-row below
        // the letters beside it — visible in strings like "WED 5 AUG" or "WEEK 1877 OF 4160".
        '0' -> listOf(".XX.", "X..X", "X..X", "X..X", ".XX.")
        '1' -> listOf(".X..", "XX..", ".X..", ".X..", "XXX.")
        '2' -> listOf(".XX.", "X..X", "..X.", ".X..", "XXXX")
        '3' -> listOf("XXX.", "...X", ".XX.", "...X", "XXX.")
        '4' -> listOf("X..X", "X..X", "XXXX", "...X", "...X")
        '5' -> listOf("XXXX", "X...", "XXX.", "...X", "XXX.")
        '6' -> listOf(".XX.", "X...", "XXX.", "X..X", ".XX.")
        '7' -> listOf("XXXX", "...X", "..X.", ".X..", ".X..")
        '8' -> listOf(".XX.", "X..X", ".XX.", "X..X", ".XX.")
        '9' -> listOf(".XX.", "X..X", ".XXX", "...X", ".XX.")
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
        // Five wide, where every other round letter is four. '0' and 'O' were previously the
        // SAME 4x5 pattern — pixel-identical, so "WEEK 1877 OF 4160" drew the same glyph for the
        // letter and the digit. Widening the letter (rather than marking the digit) is what keeps
        // the clock stable: the digits stay uniformly 4 wide, so "10:00" and "11:11" measure the
        // same, while letters are already proportional here ('I' is 3 wide). Adding an interior
        // dot to the zero instead was rejected — it would have left the two differing by a single
        // dot, which at 1-2dp dot sizes is no separation at all.
        'O' -> listOf(".XXX.", "X...X", "X...X", "X...X", ".XXX.")
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
        '.' -> listOf("....", "....", "....", "....", ".X..")
        '%' -> listOf("X..X", "...X", "..X.", ".X..", "X..X")
        '!' -> listOf(".X.", ".X.", ".X.", "...", ".X.")
        '*' -> listOf("..X..", "X.X.X", ".XXX.", "X.X.X", "..X..")
        '<' -> listOf("...X", "..X.", ".X..", "..X.", "...X")
        '>' -> listOf("X...", ".X..", "..X.", ".X..", "X...")
        '[' -> listOf("XX", "X.", "X.", "X.", "XX")
        ']' -> listOf("XX", ".X", ".X", ".X", "XX")
        '(' -> listOf(".X.", "X..", "X..", "X..", ".X.")
        ')' -> listOf(".X.", "..X", "..X", "..X", ".X.")
        '+' -> listOf("...", ".X.", "XXX", ".X.", "...")
        '-' -> listOf("...", "...", "XXX", "...", "...")
        ':' -> listOf("...", ".X.", "...", ".X.", "...")
        '/' -> listOf("...X", "..X.", "..X.", ".X..", "X...")
        '?' -> listOf(".XX.", "X..X", "..X.", "....", "..X.")
        else -> listOf("....", ".XX.", "....", ".XX.", "....")
    }
}
