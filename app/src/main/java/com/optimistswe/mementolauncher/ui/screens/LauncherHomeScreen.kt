package com.optimistswe.mementolauncher.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.optimistswe.mementolauncher.data.AppInfo
import com.optimistswe.mementolauncher.ui.components.DotIcon
import com.optimistswe.mementolauncher.ui.components.DotIconType
import com.optimistswe.mementolauncher.ui.components.AutoScaledDotText
import com.optimistswe.mementolauncher.ui.components.DotText

/**
 * The default home screen of the Memento launcher.
 *
 * Displays clock, date, life progress, favorite apps, and two dock corner icons.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LauncherHomeScreen(
    currentTime: String,
    currentDate: String,
    favoriteApps: List<AppInfo>,
    dockLeftApp: AppInfo? = null,
    dockRightApp: AppInfo? = null,
    nextAlarm: String?,
    screenTime: String? = null,
    hasUsagePermission: Boolean = false,
    isBirthday: Boolean = false,
    /** Whether the life calendar page exists, so the swipe hint matches reality. */
    showCalendar: Boolean = true,
    onLaunchApp: (String) -> Unit,
    onRemoveFavorite: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onExpandNotifications: () -> Unit = {}
) {
    val onBg = MaterialTheme.colorScheme.onBackground
    val bg = MaterialTheme.colorScheme.background
    // 0.15 alpha on black composites to #262626 — 1.39:1, far under the 4.5:1 WCAG floor.
    // Everything using this on this screen is text, including the only first-run instruction
    // ("SWIPE RIGHT TO ADD APPS") and the only signpost that other pages exist.
    // 0.46 gives #757575 = 4.56:1; 0.45 still misses at 4.43:1.
    val faint = onBg.copy(alpha = 0.46f)
    // The date is the clock's companion line, not a hint — 0.35 on black sits under ~4:1
    // contrast, which is below WCAG AA even for large text.
    val dateColor = onBg.copy(alpha = 0.55f)

    // Long-press used to remove a favourite instantly, with no confirmation, no undo and no
    // indication the gesture even existed — an accidental long-press while reaching for the app
    // silently unpinned it, and the user had to go find it in the drawer to put it back.
    var pendingRemoval by remember { mutableStateOf<AppInfo?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                var totalDrag = 0f
                detectVerticalDragGestures(
                    onDragStart = { totalDrag = 0f },
                    onVerticalDrag = { _, dragAmount ->
                        totalDrag += dragAmount
                        if (totalDrag > 100f) {
                            onExpandNotifications()
                            totalDrag = 0f
                        }
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 48.dp, bottom = 24.dp, start = 32.dp, end = 32.dp)
        ) {
            // CLOCK
            // AutoScaledDotText, not DotText: at dotSize 8.dp the 12-hour ("04:22 PM", 378dp)
            // and with-seconds ("04:22:33", 368dp) styles both exceed the 329dp of usable width
            // on a 393dp-wide phone, and DotText neither wraps nor truncates — it just drew past
            // the edge and got clipped. This scales down only when it has to, so the common
            // 24-hour case renders identically.
            AutoScaledDotText(
                text = currentTime,
                color = onBg,
                baseDotSize = 8.dp,
                baseSpacing = 2.dp,
                alignment = Alignment.Start
            )

            // The clock stands ~126px tall, so a 12dp gap left the date crowded against it —
            // it read as an orphaned fragment rather than a second line. Roughly a third of
            // the clock's height gives the pair room to read as one block.
            Spacer(modifier = Modifier.height(22.dp))

            // DATE
            DotText(
                text = currentDate,
                color = dateColor,
                dotSize = 3.2.dp,
                spacing = 1.1.dp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // BIRTHDAY
            if (isBirthday) {
                BirthdayGreeting(color = onBg)
            }

            // SCREEN TIME (only shown when permission is granted)
            if (hasUsagePermission && screenTime != null) {
                Spacer(modifier = Modifier.height(4.dp))
                DotText(
                    text = screenTime,
                    color = faint,
                    dotSize = 1.5.dp,
                    spacing = 0.5.dp
                )
            }

            // Alarm
            nextAlarm?.let {
                Spacer(modifier = Modifier.height(4.dp))
                DotText(
                    text = it,
                    color = faint,
                    dotSize = 1.5.dp,
                    spacing = 0.5.dp
                )
            }

            // Push favorites toward center
            Spacer(modifier = Modifier.weight(1f))

            // FAVORITES
            if (favoriteApps.isNotEmpty()) {
                favoriteApps.forEach { app ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { onLaunchApp(app.packageName) },
                                onLongClick = { pendingRemoval = app }
                            )
                            .padding(vertical = 14.dp)
                    ) {
                        DotText(
                            text = app.label.uppercase(),
                            color = onBg,
                            dotSize = 2.5.dp,
                            spacing = 0.8.dp
                        )
                    }
                }
            } else {
                DotText(
                    text = "SWIPE RIGHT TO ADD APPS",
                    color = faint,
                    dotSize = 2.dp,
                    spacing = 0.7.dp
                )
            }

            // Push bottom content down
            Spacer(modifier = Modifier.weight(1f))

            // SWIPE HINT
            Box(modifier = Modifier.fillMaxWidth()) {
                DotText(
                    // Must match the pages that actually exist — the calendar page is optional.
                    text = if (showCalendar) "<  CALENDAR    APPS  >" else "APPS  >",
                    color = faint,
                    dotSize = 1.5.dp,
                    spacing = 0.5.dp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        // ═══════════════════════════════════════════
        // DOCK — Bottom-left corner
        // ═══════════════════════════════════════════
        dockLeftApp?.let { app ->
            com.optimistswe.mementolauncher.ui.components.DockCornerIcon(
                app = app,
                onLaunchApp = onLaunchApp,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 24.dp, bottom = 56.dp)
            )
        }

        // ═══════════════════════════════════════════
        // DOCK — Bottom-right corner
        // ═══════════════════════════════════════════
        dockRightApp?.let { app ->
            com.optimistswe.mementolauncher.ui.components.DockCornerIcon(
                app = app,
                onLaunchApp = onLaunchApp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 24.dp, bottom = 56.dp)
            )
        }
    }

    pendingRemoval?.let { app ->
        ConfirmRemoveFavourite(
            appLabel = app.label,
            onConfirm = {
                onRemoveFavorite(app.packageName)
                pendingRemoval = null
            },
            onDismiss = { pendingRemoval = null }
        )
    }
}

/** Confirmation for unpinning a home screen favourite. */
@Composable
private fun ConfirmRemoveFavourite(
    appLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val onBg = MaterialTheme.colorScheme.onBackground
    val bg = MaterialTheme.colorScheme.background
    val dimmed = onBg.copy(alpha = 0.55f)

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                .padding(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                DotText(
                    text = "REMOVE FROM HOME?",
                    color = onBg,
                    dotSize = 2.2.dp,
                    spacing = 0.7.dp
                )
                AutoScaledDotText(
                    text = appLabel.uppercase(),
                    color = dimmed,
                    baseDotSize = 1.6.dp,
                    baseSpacing = 0.55.dp,
                    alignment = Alignment.Start
                )
                DotText(
                    text = "IT STAYS IN THE APP DRAWER",
                    color = onBg.copy(alpha = 0.4f),
                    dotSize = 1.2.dp,
                    spacing = 0.4.dp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .background(onBg.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        DotText(text = "KEEP", color = dimmed, dotSize = 1.6.dp, spacing = 0.55.dp)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .background(onBg, RoundedCornerShape(12.dp))
                            .clickable(onClick = onConfirm),
                        contentAlignment = Alignment.Center
                    ) {
                        DotText(text = "REMOVE", color = bg, dotSize = 1.6.dp, spacing = 0.55.dp)
                    }
                }
            }
        }
    }
}

/**
 * Pulsing "HAPPY BIRTHDAY" greeting.
 *
 * The infinite transition lives here rather than in [LauncherHomeScreen] so it is only created
 * on the user's birthday. Previously it was created unconditionally: an infiniteRepeatable never
 * reaches a finished state, so it kept requesting a frame every frame for as long as the home
 * page was composed — which, for a HOME app, is indefinitely. Nothing read the value on other
 * days, so it was invisible; it just stopped the UI thread ever going idle.
 */
@Composable
private fun BirthdayGreeting(color: androidx.compose.ui.graphics.Color) {
    val transition = rememberInfiniteTransition(label = "birthday")
    val alpha by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "birthdayAlpha"
    )

    DotText(
        text = "HAPPY BIRTHDAY",
        color = color.copy(alpha = alpha),
        dotSize = 2.dp,
        spacing = 0.7.dp
    )
}
