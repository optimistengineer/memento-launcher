package com.optimistswe.mementolauncher.ui.components.settings

import android.provider.Settings
import android.text.TextUtils
import android.content.Context
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import com.optimistswe.mementolauncher.data.SearchBarPosition
import com.optimistswe.mementolauncher.service.ShortsBlockerService
import com.optimistswe.mementolauncher.ui.components.AutoScaledDotText
import com.optimistswe.mementolauncher.ui.components.DotText


/**
 * Settings section for home screen and app drawer behavior.
 *
 * Allows the user to configure:
 * - Auto-open keyboard when opening the app drawer.
 * - [SearchBarPosition]: Top or bottom placement of the search bar.
 * - Usage nudge: Periodic reminders after prolonged use of distracting apps.
 *
 * This component is expandable and monitors the lifecycle to refresh
 * accessibility service state when the user returns from system settings.
 */
@Composable
fun BehaviorSettings(
    autoOpenKeyboard: Boolean,
    onAutoOpenKeyboardChange: (Boolean) -> Unit,
    searchBarPosition: SearchBarPosition,
    onSearchBarPositionChange: (SearchBarPosition) -> Unit,
    blockShortFormContent: Boolean,
    onBlockShortFormContentChange: (Boolean) -> Unit,
    usageNudgeEnabled: Boolean,
    onUsageNudgeEnabledChange: (Boolean) -> Unit,
    usageNudgeMinutes: Int,
    onUsageNudgeMinutesChange: (Int) -> Unit,
    onBg: Color,
    bg: Color,
    dimmed: Color,
    faint: Color,
    surface: Color,
    cardBg: Color
) {
    var behaviorExpanded by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Source of truth: what the system actually says about the accessibility service.
    // We track this as local state so the toggle always reflects reality, not a stale
    // DataStore value. Refreshed every time the user returns from another screen
    // (e.g. the Accessibility Settings page they were sent to when toggling ON).
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Accessibility check removed since service is disabled.
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column {
        // BEHAVIOR SECTION
        SettingsSectionHeader(
            title = "BEHAVIOR",
            expanded = behaviorExpanded,
            onToggle = { behaviorExpanded = !behaviorExpanded },
            onBg = onBg,
            dimmed = dimmed
        )

        AnimatedVisibility(
            visible = behaviorExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cardBg, RoundedCornerShape(16.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SettingsToggle(
                    label = "AUTO OPEN KEYBOARD",
                    description = "OPEN KEYBOARD IN APP DRAWER",
                    checked = autoOpenKeyboard,
                    onCheckedChange = onAutoOpenKeyboardChange,
                    onBg = onBg, bg = bg, dimmed = dimmed, surface = surface
                )

                Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(faint))

                SettingsSegmentedControl(
                    label = "SEARCH BAR POSITION",
                    options = SearchBarPosition.entries.map { it.name },
                    selectedIndex = SearchBarPosition.entries.indexOf(searchBarPosition),
                    onSelect = { onSearchBarPositionChange(SearchBarPosition.entries[it]) },
                    onBg = onBg, bg = bg, dimmed = dimmed
                )



                // ── Usage nudge ───────────────────────────────────────────────
                SettingsToggle(
                    label = "USAGE NUDGE",
                    description = "REMIND ME AFTER TIME IN DISTRACTING APP",
                    checked = usageNudgeEnabled,
                    onCheckedChange = onUsageNudgeEnabledChange,
                    onBg = onBg, bg = bg, dimmed = dimmed, surface = surface
                )

                // Show minute stepper only when nudge is on
                if (usageNudgeEnabled) {
                    var localMinutes by remember(usageNudgeMinutes) { mutableIntStateOf(usageNudgeMinutes) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(onBg.copy(alpha = 0.1f))
                                .clickable {
                                    if (localMinutes > 5) {
                                        localMinutes -= 5
                                        onUsageNudgeMinutesChange(localMinutes)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            DotText(text = "-", color = onBg, dotSize = 2.dp, spacing = 1.dp)
                        }
                        Spacer(modifier = Modifier.width(20.dp))
                        DotText(
                            text = "${localMinutes} MIN",
                            color = onBg,
                            dotSize = 2.5.dp,
                            spacing = 0.8.dp
                        )
                        Spacer(modifier = Modifier.width(20.dp))
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(onBg.copy(alpha = 0.1f))
                                .clickable {
                                    if (localMinutes < 120) {
                                        localMinutes += 5
                                        onUsageNudgeMinutesChange(localMinutes)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            DotText(text = "+", color = onBg, dotSize = 2.dp, spacing = 1.dp)
                        }
                    }
                }
            }
        }
    }
}
