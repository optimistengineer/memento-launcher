package com.optimistswe.mementolauncher.ui.components.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.optimistswe.mementolauncher.ui.components.DotText

/**
 * Collapsible section header with expand/collapse arrow.
 *
 * Displays the section [title] with a SHOW/HIDE toggle on the right side.
 * Used as the header for each expandable settings section.
 *
 * @param title The uppercase section label displayed on the left.
 * @param expanded Whether the section body is currently visible.
 * @param onToggle Callback invoked when the header is tapped to toggle visibility.
 */
@Composable
fun SettingsSectionHeader(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    onBg: Color,
    dimmed: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DotText(
            text = title,
            color = dimmed,
            dotSize = 1.5.dp,
            spacing = 0.5.dp
        )
        DotText(
            text = if (expanded) "HIDE" else "SHOW",
            color = onBg,
            dotSize = 2.dp,
            spacing = 0.5.dp
        )
    }
}

/**
 * Toggle row with label, description, and a Material 3 switch.
 *
 * Renders a two-line text block on the left (label in primary color,
 * description in dimmed color) with a toggle switch on the right.
 *
 * @param label Primary text describing the setting.
 * @param description Secondary helper text shown below the label.
 * @param checked Current toggle state.
 * @param onCheckedChange Callback invoked when the switch is toggled.
 */
@Composable
fun SettingsToggle(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onBg: Color,
    bg: Color,
    dimmed: Color,
    surface: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            DotText(
                text = label,
                color = onBg,
                dotSize = 1.5.dp,
                spacing = 0.5.dp
            )
            DotText(
                text = description,
                color = dimmed.copy(alpha = 0.6f),
                dotSize = 1.dp,
                spacing = 0.4.dp
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = bg,
                checkedTrackColor = onBg,
                uncheckedThumbColor = dimmed,
                uncheckedTrackColor = surface
            )
        )
    }
}

/**
 * Segmented control -- a horizontal row of mutually exclusive options.
 *
 * Each option is rendered as a rounded chip. The selected chip is highlighted
 * with the foreground color while unselected chips use a faint background.
 *
 * @param label Section label displayed above the control.
 * @param options Display strings for each segment.
 * @param selectedIndex Index of the currently selected option.
 * @param onSelect Callback invoked with the index of the tapped option.
 */
@Composable
fun SettingsSegmentedControl(
    label: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onBg: Color,
    bg: Color,
    dimmed: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        DotText(
            text = label,
            color = onBg,
            dotSize = 1.5.dp,
            spacing = 0.5.dp
        )
        Row(
            modifier = Modifier.fillMaxWidth().height(44.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEachIndexed { index, text ->
                val isSelected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(
                            if (isSelected) onBg else dimmed.copy(alpha = 0.1f),
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { onSelect(index) },
                    contentAlignment = Alignment.Center
                ) {
                    DotText(
                        text = text,
                        color = if (isSelected) bg else onBg,
                        dotSize = 1.dp,
                        spacing = 0.4.dp
                    )
                }
            }
        }
    }
}
