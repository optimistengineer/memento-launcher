package com.optimistswe.mementolauncher.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
    onLaunchApp: (String) -> Unit,
    onRemoveFavorite: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onExpandNotifications: () -> Unit = {}
) {
    val onBg = MaterialTheme.colorScheme.onBackground
    val bg = MaterialTheme.colorScheme.background
    val dimmed = onBg.copy(alpha = 0.35f)
    val faint = onBg.copy(alpha = 0.15f)

    // Pulsing alpha for birthday message
    val birthdayTransition = rememberInfiniteTransition(label = "birthday")
    val birthdayAlpha by birthdayTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "birthdayAlpha"
    )

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
            DotText(
                text = currentTime,
                color = onBg,
                dotSize = 8.dp,
                spacing = 2.dp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // DATE
            DotText(
                text = currentDate,
                color = dimmed,
                dotSize = 3.dp,
                spacing = 1.dp
            )

            Spacer(modifier = Modifier.height(4.dp))

            // BIRTHDAY
            if (isBirthday) {
                DotText(
                    text = "HAPPY BIRTHDAY",
                    color = onBg.copy(alpha = birthdayAlpha),
                    dotSize = 2.dp,
                    spacing = 0.7.dp
                )
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
                                onLongClick = { onRemoveFavorite(app.packageName) }
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
                    text = "<  CALENDAR  |  APPS  >",
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
}
