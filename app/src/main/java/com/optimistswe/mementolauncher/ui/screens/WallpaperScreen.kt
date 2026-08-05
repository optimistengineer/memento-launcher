package com.optimistswe.mementolauncher.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.optimistswe.mementolauncher.domain.CalendarMetrics
import com.optimistswe.mementolauncher.ui.components.AutoScaledDotText
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
    onOpenSettings: () -> Unit,
    /**
     * Whether this page is the one the user is actually looking at. The pager keeps this page
     * composed while the user is on the home page, so the current-week pulse must not animate
     * unless this is true — otherwise it drives a frame every frame off-screen.
     */
    isActive: Boolean = true
) {
    val bg = MaterialTheme.colorScheme.background
    val onBg = MaterialTheme.colorScheme.onBackground
    val dimmed = onBg.copy(alpha = 0.35f)
    // These were 0.12 (1.27:1) and 0.25 (2.03:1) — both carrying real text, not decoration.
    // CalendarImageGenerator draws the *same* labels into the wallpaper bitmap at 0xFF888888
    // (5.9:1) and the same unlived dots at 0xFF4A4A4A, so the in-app page was roughly three
    // times fainter than this app's own choice for identical content. Adopt those values so
    // the screen and the wallpaper agree.
    val gridEmpty = onBg.copy(alpha = 0.29f)    // #4A4A4A — the generator's emptyColor
    val labelColor = onBg.copy(alpha = 0.53f)   // #878787 — the generator's labelColor

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
                // Set on two lines and auto-scaled. On one line this needed 336dp of dot-matrix
                // at the default font scale, but the hint's own paddings leave only ~240dp on a
                // 360dp phone — so the first thing a new user saw on this page was
                // "SET YOUR BIRTH DAT" running off the screen edge.
                AutoScaledDotText(
                    text = "SET YOUR\nBIRTH DATE",
                    color = dimmed,
                    baseDotSize = 3.dp,
                    baseSpacing = 1.dp,
                    alignment = Alignment.CenterHorizontally
                )
                Spacer(modifier = Modifier.height(16.dp))
                DotText(
                    text = "IN SETTINGS",
                    color = labelColor,
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
                    .fillMaxWidth()
                    // Clearance for the MEMENTO heading, which is aligned TopCenter in the same
                    // Box. Now that this Column fills the page height (its grid child carries
                    // weight), its first row otherwise starts at the very top and the
                    // "WEEK OF THE YEAR" axis label prints straight through the heading.
                    .padding(top = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                // The grid child below carries weight(), which makes this Column fill the page
                // height; Center keeps the block visually centred (as it was when the Column was
                // intrinsically sized) instead of packing it to the top with a gap underneath.
                verticalArrangement = Arrangement.Center
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

                // weight(fill = false) hands this box exactly the height left over after the axis
                // label above and the stats block below have been measured, so the grid can be
                // fitted to it. Previously the cell size came from the available WIDTH alone with
                // no height term at all, and the resulting height was then silently coerced by the
                // Column. Three things broke at once, on a stock 360x640dp phone at the default
                // 80-year life expectancy: the grid drew 76 of 80 rows and ran off the bottom of
                // the screen (Canvas does not clip, so the surplus painted outside its node), the
                // "WEEK OF THE YEAR" axis label collided with the MEMENTO heading, and the Column
                // had nothing left for the stats, so "% LIVED" and "WEEK n OF m" were measured at
                // zero height and — since DotText draws inside a clipRect — vanished entirely.
                // Letting Compose do the arithmetic also means this tracks the user's font scale,
                // which a hardcoded reservation would not.
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                ) {
                    val density = LocalDensity.current
                    // The vertical label Box below is 15.dp wide. This used to reserve 28.dp, so
                    // the grid was measured 13dp narrower than it was actually drawn, making it
                    // ~3.5% taller than the height reserved for it even when it nominally fit.
                    val labelWidthPx = with(density) { VERTICAL_LABEL_WIDTH.toPx() }
                    val rowSpacingPx = with(density) { ROW_SPACING.toPx() }
                    val columns = 52
                    val rows = metrics.lifeExpectancy
                    val availableWidthPx = constraints.maxWidth - labelWidthPx
                    val cellFromWidth = (availableWidthPx - COL_SPACING * (columns - 1)) / columns
                    // rows is user-controlled (50..120) and independent of the screen, so the
                    // height budget has to be able to win.
                    val cellFromHeight = (constraints.maxHeight - rowSpacingPx * (rows - 1)) / rows
                    val cellSizePx = minOf(cellFromWidth, cellFromHeight).coerceAtLeast(1f)
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
                                .width(VERTICAL_LABEL_WIDTH),
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
                            emptyColor = gridEmpty,
                            rowSpacing = rowSpacingPx,
                            // Passed in rather than re-derived from the Canvas width. The draw code
                            // used to recompute it independently, so the geometry it painted could
                            // disagree with the geometry that was measured and reserved.
                            cellSize = cellSizePx,
                            animatePulse = isActive,
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
                            color = labelColor,
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
    /** Edge length of one week cell, in px, already fitted to both axes by the caller. */
    cellSize: Float,
    rowSpacing: Float = 1f,
    animatePulse: Boolean = true,
    modifier: Modifier = Modifier
) {
    val columns = 52
    val rows = metrics.lifeExpectancy
    val weeksLived = metrics.weeksLived
    // -1 means "no cell to highlight": either nothing lived yet, or the user has outlived their
    // life expectancy, in which case the index falls past the last row and the marker would be
    // drawn outside the grid.
    val currentWeekIndex = if (weeksLived in 1..(rows * columns)) weeksLived - 1 else -1

    Box(modifier = modifier) {
        // Static dots. This Canvas deliberately reads NO animated state, and graphicsLayer puts
        // it in its own RenderNode so its display list is reused rather than re-recorded every
        // time the pulse above invalidates. Without that isolation the thousands of circles
        // below would be re-rasterised on every animation frame.
        // clipToBounds so a future measure/draw mismatch is contained instead of painting over
        // whatever is outside this node, which is how the overflow above went unnoticed.
        Canvas(modifier = Modifier.fillMaxSize().clipToBounds().graphicsLayer()) {
            val radius = cellSize * 0.4f
            // When the height budget set the cell size, the grid is narrower than the canvas;
            // centre it instead of letting it hug the left edge with dead space on the right.
            val gridWidth = columns * cellSize + (columns - 1) * COL_SPACING
            val startX = ((size.width - gridWidth) / 2f).coerceAtLeast(0f)

            var weekIndex = 0
            for (row in 0 until rows) {
                for (col in 0 until columns) {
                    if (weekIndex == currentWeekIndex) { weekIndex++; continue }
                    val cx = startX + col * (cellSize + COL_SPACING) + cellSize / 2f
                    val cy = row * (cellSize + rowSpacing) + cellSize / 2f
                    if (weekIndex < weeksLived) {
                        drawCircle(color = filledColor, radius = radius, center = Offset(cx, cy))
                    } else {
                        drawCircle(color = emptyColor, radius = radius * 0.6f, center = Offset(cx, cy))
                    }
                    weekIndex++
                }
            }
        }

        // The pulsing current-week marker, in its own Canvas so that the per-frame animation
        // invalidates only this layer instead of the entire grid.
        if (currentWeekIndex >= 0) {
            CurrentWeekPulse(
                currentWeekIndex = currentWeekIndex,
                columns = columns,
                cellSize = cellSize,
                rowSpacing = rowSpacing,
                animate = animatePulse,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

private const val COL_SPACING = 1f

/** Width of the rotated "YEAR OF YOUR LIFE" label column, reserved by the height maths. */
private val VERTICAL_LABEL_WIDTH = 15.dp

/**
 * Vertical gap between year rows. Expressed in dp, not raw pixels: as a bare `5f` the gap was a
 * fixed number of *physical* pixels, so the grid's total height silently changed with screen
 * density instead of staying visually constant.
 */
private val ROW_SPACING = 2.dp

/**
 * The pulsing "you are here" dot.
 *
 * @param animate when false, no infinite transition is created at all. An infiniteRepeatable
 *   never reaches a finished state, so while one exists it requests a frame every frame for as
 *   long as it is composed. The pager keeps this page composed while the user is on the home
 *   page ([HorizontalPager] with beyondViewportPageCount = 1), so an unconditional animation
 *   here kept the launcher rendering ~26fps while completely idle — measured at 258 frames over
 *   10 idle seconds, against 0 for a system app on the same device.
 */
@Composable
private fun CurrentWeekPulse(
    currentWeekIndex: Int,
    columns: Int,
    cellSize: Float,
    rowSpacing: Float,
    animate: Boolean,
    modifier: Modifier = Modifier
) {
    val pulseScale: Float
    val pulseAlpha: Float
    if (animate) {
        val transition = rememberInfiniteTransition(label = "currentWeekPulse")
        val scale by transition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.8f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseScale"
        )
        val alpha by transition.animateFloat(
            initialValue = 0.6f,
            targetValue = 0.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseAlpha"
        )
        pulseScale = scale
        pulseAlpha = alpha
    } else {
        // Resting state: solid dot, no halo.
        pulseScale = 1.0f
        pulseAlpha = 0f
    }

    Canvas(modifier = modifier.clipToBounds()) {
        val radius = cellSize * 0.4f
        val gridWidth = columns * cellSize + (columns - 1) * COL_SPACING
        val startX = ((size.width - gridWidth) / 2f).coerceAtLeast(0f)
        val cx = startX + (currentWeekIndex % columns) * (cellSize + COL_SPACING) + cellSize / 2f
        val cy = (currentWeekIndex / columns) * (cellSize + rowSpacing) + cellSize / 2f
        val currentWeekColor = Color(0xFF228B22)

        drawCircle(color = currentWeekColor, radius = radius, center = Offset(cx, cy))
        if (pulseAlpha > 0f) {
            drawCircle(
                color = currentWeekColor.copy(alpha = pulseAlpha),
                radius = radius * pulseScale,
                center = Offset(cx, cy),
                style = Stroke(width = 1.5f)
            )
        }
    }
}
