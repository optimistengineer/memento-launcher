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
import com.optimistswe.mementolauncher.data.BackgroundStyle
import com.optimistswe.mementolauncher.data.ClockStyle
import com.optimistswe.mementolauncher.data.FontSize

/**
 * Settings section for UI aesthetics and appearance.
 *
 * Allows the user to configure:
 * - [ClockStyle]: 12h, 24h, or 24h with seconds.
 * - [BackgroundStyle]: Solid black or Dot matrix grid.
 * - [FontSize]: Global typography and icon scaling.
 *
 * This component is expandable and uses a segmented control for multi-option settings.
 */
@Composable
fun AppearanceSettings(
    clockStyle: ClockStyle,
    onClockStyleChange: (ClockStyle) -> Unit,
    backgroundStyle: BackgroundStyle,
    onBackgroundStyleChange: (BackgroundStyle) -> Unit,
    fontSize: FontSize,
    onFontSizeChange: (FontSize) -> Unit,
    onBg: Color,
    bg: Color,
    dimmed: Color,
    faint: Color,
    cardBg: Color
) {
    var expanded by remember { mutableStateOf(true) }

    Column {
        SettingsSectionHeader(
            title = "APPEARANCE",
            expanded = expanded,
            onToggle = { expanded = !expanded },
            onBg = onBg,
            dimmed = dimmed
        )

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cardBg, RoundedCornerShape(16.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Clock Style
                SettingsSegmentedControl(
                    label = "CLOCK STYLE",
                    options = ClockStyle.entries.map { style ->
                        when (style) {
                            ClockStyle.H24 -> "24H"
                            ClockStyle.H12 -> "12H"
                            ClockStyle.H24_SEC -> "24H:SS"
                        }
                    },
                    selectedIndex = ClockStyle.entries.indexOf(clockStyle),
                    onSelect = { onClockStyleChange(ClockStyle.entries[it]) },
                    onBg = onBg, bg = bg, dimmed = dimmed
                )

                // Separator
                Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(faint))

                // Background Style
                SettingsSegmentedControl(
                    label = "BACKGROUND",
                    options = BackgroundStyle.entries.map { it.name.replace("_", " ") },
                    selectedIndex = BackgroundStyle.entries.indexOf(backgroundStyle),
                    onSelect = { onBackgroundStyleChange(BackgroundStyle.entries[it]) },
                    onBg = onBg, bg = bg, dimmed = dimmed
                )

                // Separator
                Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(faint))

                // Font Scale
                SettingsSegmentedControl(
                    label = "FONT SCALE",
                    options = FontSize.entries.map { it.name },
                    selectedIndex = FontSize.entries.indexOf(fontSize),
                    onSelect = { onFontSizeChange(FontSize.entries[it]) },
                    onBg = onBg, bg = bg, dimmed = dimmed
                )
            }
        }
    }
}
