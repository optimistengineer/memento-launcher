package com.optimistswe.mementolauncher.ui.components.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.optimistswe.mementolauncher.data.SearchBarPosition

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
