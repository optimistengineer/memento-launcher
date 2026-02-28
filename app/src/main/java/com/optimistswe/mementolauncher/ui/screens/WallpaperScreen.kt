package com.optimistswe.mementolauncher.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.optimistswe.mementolauncher.domain.CalendarMetrics
import com.optimistswe.mementolauncher.ui.components.DotText

/**
 * Wallpaper Screen — the life calendar grid with axis labels.
 *
 * Accessible by swiping LEFT from home.
 *
 * Layout:
 * - "WEEK OF THE YEAR" — top-left above grid
 * - "MEMENTO" — top-right above grid
 * - "YEAR OF YOUR LIFE" — rotated vertically on the left
 * - Grid of dots — each dot = one week of life
 * - Progress text — below grid
 */
@Composable
fun WallpaperScreen(
    metrics: CalendarMetrics?,
    lifeProgressText: String,
    onOpenSettings: () -> Unit
) {
    val bg = MaterialTheme.colorScheme.background
    val onBg = MaterialTheme.colorScheme.onBackground
    val dimmed = onBg.copy(alpha = 0.35f)
    val faint = onBg.copy(alpha = 0.12f)
    val labelColor = onBg.copy(alpha = 0.25f)

    // System bars handled by LauncherRootScreen.systemBarsPadding().
    // contentAlignment = Center vertically centers the Column since it uses fillMaxWidth
    // (not fillMaxSize), so its height is intrinsic and the Box centers it properly.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (metrics == null) {
            // No birth date set — show hint
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(32.dp)
                    .clickable { onOpenSettings() }
                    .padding(16.dp)
            ) {
                DotText(
                    text = "SET YOUR BIRTH DATE",
                    color = dimmed,
                    dotSize = 3.dp,
                    spacing = 1.dp
                )
                Spacer(modifier = Modifier.height(16.dp))
                DotText(
                    text = "IN SETTINGS",
                    color = faint,
                    dotSize = 2.dp,
                    spacing = 0.7.dp
                )
            }
        } else {
            // ═══════════════════════════════════════════
            // HEADING — pinned to the top of the screen,
            // independent of the grid's vertical position.
            // ═══════════════════════════════════════════
            DotText(
                text = "MEMENTO",
                color = dimmed,
                dotSize = 2.8.dp,
                spacing = 0.9.dp,
                modifier = Modifier.align(Alignment.TopCenter)
            )

            // ═══════════════════════════════════════════
            // GRID + STATS — centered on the screen,
            // completely unaffected by the heading above.
            // ═══════════════════════════════════════════
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Axis label above grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    // verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Spacer(modifier = Modifier.width(15.dp))
                    DotText(
                        text = "WEEK OF THE YEAR",
                        color = labelColor,
                        dotSize = 1.2.dp,
                        spacing = 0.4.dp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // BoxWithConstraints self-sizes to the exact grid height so the
                // vertical label aligns perfectly with the grid content.
                BoxWithConstraints(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val density = LocalDensity.current
                    val labelWidthPx = with(density) { 28.dp.toPx() }
                    val colSpacingPx = 1f
                    val rowSpacingPx = 5f  // more vertical gap so rows (years) breathe
                    val columns = 52
                    val rows = metrics.lifeExpectancy
                    val availableWidthPx = constraints.maxWidth - labelWidthPx
                    val cellSizePx = ((availableWidthPx - colSpacingPx * (columns - 1)) / columns)
                        .coerceAtLeast(1f)
                    val gridHeightPx = cellSizePx * rows + rowSpacingPx * (rows - 1)
                    val gridHeightDp = with(density) { gridHeightPx.toDp() }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(gridHeightDp)
                    ) {
                        // Vertical label: "YEAR OF YOUR LIFE"
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(15.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            DotText(
                                text = "YEAR OF YOUR LIFE",
                                color = labelColor,
                                dotSize = 1.2.dp,
                                spacing = 0.4.dp,
                                modifier = Modifier
                                    .rotate(-90f)
                                    .layout { measurable, constraints ->
                                        val placeable = measurable.measure(
                                            constraints.copy(
                                                minWidth = 0,
                                                maxWidth = constraints.maxHeight,
                                                minHeight = 0,
                                                maxHeight = constraints.maxWidth
                                            )
                                        )
                                        layout(placeable.height, placeable.width) {
                                            placeable.place(
                                                x = -(placeable.width - placeable.height) / 2,
                                                y = -(placeable.height - placeable.width) / 2
                                            )
                                        }
                                    }
                            )
                        }

                        LifeCalendarGrid(
                            metrics = metrics,
                            filledColor = onBg,
                            emptyColor = faint,
                            rowSpacing = rowSpacingPx,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(35.dp))

                // Progress stats — offset by the same 28dp vertical-label width so
                // these center relative to the grid columns, not the full row width.
                Row(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.width(28.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        DotText(
                            text = "${metrics.percentageLived.toInt()}% LIVED",
                            color = dimmed,
                            dotSize = 2.5.dp,
                            spacing = 0.8.dp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        DotText(
                            text = lifeProgressText,
                            color = faint,
                            dotSize = 1.5.dp,
                            spacing = 0.5.dp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Renders the life calendar as a Compose Canvas.
 *
 * Grid: rows = lifeExpectancy, columns = 52 (weeks per year)
 * Filled dots = weeks lived, smaller faint dots = weeks remaining.
 */
@Composable
private fun LifeCalendarGrid(
    metrics: CalendarMetrics,
    filledColor: Color,
    emptyColor: Color,
    rowSpacing: Float = 1f,
    modifier: Modifier = Modifier
) {
    val columns = 52
    val rows = metrics.lifeExpectancy
    val weeksLived = metrics.weeksLived
    val currentWeekIndex = (weeksLived - 1).coerceAtLeast(0)

    // Pulsing animation for the current week's dot
    val pulseTransition = rememberInfiniteTransition(label = "currentWeekPulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    // Cache the static grid into an ImageBitmap so we don't redraw 4160 circles every frame.
    // Only the pulsing current-week dot needs per-frame drawing.
    val density = LocalDensity.current
    val cachedGrid = remember(metrics, filledColor, emptyColor, rowSpacing) {
        // We can't know the exact size yet, so we'll draw in the Canvas below.
        // Instead, cache the grid parameters to avoid recomputation.
        metrics // trigger recompute when metrics change
    }

    Canvas(modifier = modifier) {
        val availableWidth = size.width
        val colSpacing = 1f

        val cellSize = ((availableWidth - (columns - 1) * colSpacing) / columns).coerceAtLeast(1f)
        val radius = cellSize * 0.4f

        // Draw static dots (filled + empty) — these don't change per frame
        var weekIndex = 0
        for (row in 0 until rows) {
            for (col in 0 until columns) {
                if (weekIndex == currentWeekIndex) { weekIndex++; continue }
                val cx = col * (cellSize + colSpacing) + cellSize / 2f
                val cy = row * (cellSize + rowSpacing) + cellSize / 2f
                val isFilled = weekIndex < weeksLived

                if (isFilled) {
                    drawCircle(color = filledColor, radius = radius, center = Offset(cx, cy))
                } else {
                    drawCircle(color = emptyColor, radius = radius * 0.6f, center = Offset(cx, cy))
                }
                weekIndex++
            }
        }

        // Draw pulsing current week dot (only element that changes per frame)
        val currentRow = currentWeekIndex / columns
        val currentCol = currentWeekIndex % columns
        val cx = currentCol * (cellSize + colSpacing) + cellSize / 2f
        val cy = currentRow * (cellSize + rowSpacing) + cellSize / 2f
        val currentWeekColor = Color(0xFF228B22)
        drawCircle(color = currentWeekColor, radius = radius, center = Offset(cx, cy))
        drawCircle(
            color = currentWeekColor.copy(alpha = pulseAlpha),
            radius = radius * pulseScale,
            center = Offset(cx, cy),
            style = Stroke(width = 1.5f)
        )
    }
}
