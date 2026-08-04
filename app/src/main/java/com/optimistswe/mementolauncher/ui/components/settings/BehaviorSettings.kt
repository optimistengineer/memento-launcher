package com.optimistswe.mementolauncher.ui.components.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.optimistswe.mementolauncher.data.SearchBarPosition
import com.optimistswe.mementolauncher.ui.components.DotText

/**
 * Settings section for home screen and app drawer behavior.
 *
 * Allows the user to configure:
 * - Auto-open keyboard when opening the app drawer.
 * - [SearchBarPosition]: Top or bottom placement of the search bar.
 */
@Composable
fun BehaviorSettings(
    autoOpenKeyboard: Boolean,
    onAutoOpenKeyboardChange: (Boolean) -> Unit,
    searchBarPosition: SearchBarPosition,
    onSearchBarPositionChange: (SearchBarPosition) -> Unit,
    /** Whether usage-access has been granted, so screen time can be shown. */
    hasUsageAccess: Boolean = false,
    onBg: Color,
    bg: Color,
    dimmed: Color,
    faint: Color,
    surface: Color,
    cardBg: Color
) {
    var behaviorExpanded by remember { mutableStateOf(true) }

    Column {
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
                // Screen time needs usage access, which is granted in system settings rather
                // than by a runtime prompt. Nothing in the app used to open that screen, so the
                // feature was unreachable for anyone who had not found it manually — and a
                // declared sensitive permission with no user-facing route to enabling it is
                // exactly what store review flags.
                val context = LocalContext.current
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !hasUsageAccess) {
                            runCatching {
                                context.startActivity(
                                    Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                )
                            }.onFailure {
                                // Some OEM builds do not expose this screen at all.
                                runCatching {
                                    context.startActivity(Intent(Settings.ACTION_SETTINGS))
                                }
                            }
                        }
                ) {
                    DotText(
                        text = "SCREEN TIME",
                        color = onBg,
                        dotSize = 1.5.dp,
                        spacing = 0.5.dp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    DotText(
                        text = if (hasUsageAccess) "SHOWN ON HOME SCREEN"
                               else "TAP TO ALLOW USAGE ACCESS",
                        color = dimmed,
                        dotSize = 1.dp,
                        spacing = 0.4.dp
                    )
                }

                Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(faint))

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
            }
        }
    }
}
