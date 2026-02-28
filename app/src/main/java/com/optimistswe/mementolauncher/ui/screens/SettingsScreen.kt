package com.optimistswe.mementolauncher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.optimistswe.mementolauncher.ui.components.DotIcon
import com.optimistswe.mementolauncher.ui.components.DotIconType
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.optimistswe.mementolauncher.data.CalendarTheme
import com.optimistswe.mementolauncher.data.UserPreferences
import com.optimistswe.mementolauncher.domain.LifeCalendarCalculator
import com.optimistswe.mementolauncher.wallpaper.WallpaperTarget
import com.optimistswe.mementolauncher.ui.components.DotText
import com.optimistswe.mementolauncher.ui.components.DotStyleIcon
import com.optimistswe.mementolauncher.ui.components.DottedDatePickerDialog
import com.optimistswe.mementolauncher.data.DotStyle
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Full settings screen for configuring the Memento launcher.
 *
 * Provides controls for birth date, life expectancy, theme, font scale,
 * background style, dot style, and search keyboard behavior.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    preferences: UserPreferences,
    onBack: () -> Unit,
    onBirthDateChange: (LocalDate) -> Unit,
    onLifeExpectancyChange: (Int) -> Unit,
    onWallpaperTargetChange: (WallpaperTarget) -> Unit,
    onThemeChange: (CalendarTheme) -> Unit,
    onDotStyleChange: (com.optimistswe.mementolauncher.data.DotStyle) -> Unit,
    onAutoOpenKeyboardChange: (Boolean) -> Unit,
    onBackgroundStyleChange: (com.optimistswe.mementolauncher.data.BackgroundStyle) -> Unit,
    onFontSizeChange: (com.optimistswe.mementolauncher.data.FontSize) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var lifeExpectancy by remember(preferences.lifeExpectancy) {
        mutableStateOf(preferences.lifeExpectancy)
    }

    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM d yyyy") }
    val onBg = MaterialTheme.colorScheme.onBackground
    val bg = MaterialTheme.colorScheme.background

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .padding(top = 48.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clickable(onClick = onBack)
                    .padding(end = 24.dp, top = 8.dp, bottom = 8.dp)
            ) {
                DotText(text = "<", color = onBg, dotSize = 2.dp, spacing = 1.dp)
            }
            DotText(text = "SETTINGS", color = onBg, dotSize = 4.dp, spacing = 1.dp)
        }

        Spacer(modifier = Modifier.height(32.dp))

        Column(
            modifier = Modifier.padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Birth Date Section
            SettingsSection(title = "BIRTH DATE", onBg) {
                val dateText = preferences.birthDate?.format(dateFormatter)?.uppercase() ?: "NOT SET"
                SettingsCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 80.dp)
                            .clickable { showDatePicker = true }
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DotText(text = dateText, color = onBg, dotSize = 2.dp, spacing = 1.dp)
                        DotIcon(
                            type = DotIconType.EDIT,
                            color = onBg.copy(alpha = 0.7f),
                            dotSize = 2.dp,
                            spacing = 1.dp
                        )
                    }
                }
            }

            // Life Expectancy Section
            SettingsSection(title = "LIFE EXPECTANCY", onBg) {
                SettingsCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 80.dp)
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        DotText(text = "YEARS", color = onBg.copy(alpha = 0.7f), dotSize = 2.dp, spacing = 1.dp)

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RoundIconButton(
                                text = "-",
                                onClick = { 
                                    if (lifeExpectancy > LifeCalendarCalculator.MIN_LIFE_EXPECTANCY) {
                                        lifeExpectancy--
                                        onLifeExpectancyChange(lifeExpectancy)
                                    } 
                                },
                                color = onBg
                            )
                            
                            Spacer(modifier = Modifier.width(24.dp))
                            
                            DotText(
                                text = lifeExpectancy.toString(),
                                color = onBg,
                                dotSize = 4.dp,
                                spacing = 1.dp
                            )
                            
                            Spacer(modifier = Modifier.width(24.dp))

                            RoundIconButton(
                                text = "+",
                                onClick = { 
                                    if (lifeExpectancy < LifeCalendarCalculator.MAX_LIFE_EXPECTANCY) {
                                        lifeExpectancy++
                                        onLifeExpectancyChange(lifeExpectancy)
                                    }
                                },
                                color = onBg
                            )
                        }
                    }
                }
            }

            // Theme Section
            SettingsSection(title = "THEME", onBg) {
                SettingsCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 80.dp)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CalendarTheme.entries.forEach { theme ->
                            SegmentedControlButton(
                                selected = preferences.theme == theme,
                                onClick = { onThemeChange(theme) },
                                modifier = Modifier.weight(1f),
                                onBg = onBg
                            ) { textColor ->
                                DotText(
                                    text = theme.name,
                                    color = textColor,
                                    dotSize = 1.5.dp,
                                    spacing = 1.dp
                                )
                            }
                        }
                    }
                }
            }

            // Font Size Section
            SettingsSection(title = "UI FONT SCALE", onBg) {
                SettingsCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 80.dp)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        com.optimistswe.mementolauncher.data.FontSize.entries.forEach { size ->
                            SegmentedControlButton(
                                selected = preferences.fontSize == size,
                                onClick = { onFontSizeChange(size) },
                                modifier = Modifier.weight(1f),
                                onBg = onBg
                            ) { textColor ->
                                DotText(
                                    text = size.name,
                                    color = textColor,
                                    dotSize = 1.5.dp,
                                    spacing = 1.dp
                                )
                            }
                        }
                    }
                }
            }

            // Launcher Background Section
            SettingsSection(title = "LAUNCHER BACKGROUND", onBg) {
                SettingsCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 80.dp)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        com.optimistswe.mementolauncher.data.BackgroundStyle.entries.forEach { style ->
                            SegmentedControlButton(
                                selected = preferences.backgroundStyle == style,
                                onClick = { onBackgroundStyleChange(style) },
                                modifier = Modifier.weight(1f),
                                onBg = onBg
                            ) { textColor ->
                                DotText(
                                    text = style.name.replace("_", "\n"),
                                    color = textColor,
                                    dotSize = 1.dp,
                                    spacing = 1.dp
                                )
                            }
                        }
                    }
                }
            }

            // Dot Style Section
            SettingsSection(title = "DOT STYLE", onBg) {
                SettingsCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 80.dp)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DotStyle.entries.forEach { style ->
                            SegmentedControlButton(
                                selected = preferences.dotStyle == style,
                                onClick = { onDotStyleChange(style) },
                                modifier = Modifier.weight(1f),
                                onBg = onBg
                            ) { textColor ->
                                DotStyleIcon(
                                    style = style,
                                    color = textColor,
                                    dotSize = 2.dp
                                )
                            }
                        }
                    }
                }
            }

            // Keyboard Section
            SettingsSection(title = "SEARCH PREFERENCES", onBg) {
                SettingsCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 80.dp)
                            .clickable { onAutoOpenKeyboardChange(!preferences.autoOpenKeyboard) }
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        DotText(text = "AUTO OPEN KEYBOARD", color = onBg, dotSize = 1.5.dp, spacing = 1.dp)
                        
                        // Custom Dot Toggle
                        Box(
                            modifier = Modifier
                                .width(60.dp)
                                .height(32.dp)
                                .background(
                                    if (preferences.autoOpenKeyboard) onBg else Color.Transparent, 
                                    RoundedCornerShape(16.dp)
                                )
                                .border(
                                    2.dp, 
                                    if (preferences.autoOpenKeyboard) Color.Transparent else onBg.copy(alpha = 0.5f), 
                                    RoundedCornerShape(16.dp)
                                )
                                .padding(4.dp),
                            contentAlignment = if (preferences.autoOpenKeyboard) Alignment.CenterEnd else Alignment.CenterStart
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(
                                        if (preferences.autoOpenKeyboard) MaterialTheme.colorScheme.background else onBg.copy(alpha = 0.5f), 
                                        CircleShape
                                    )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(64.dp))
        }
    }

    if (showDatePicker) {
        DottedDatePickerDialog(
            initialDate = preferences.birthDate,
            onDateSelected = { newDate ->
                onBirthDateChange(newDate)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    onBg: Color,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        DotText(
            text = title,
            color = onBg.copy(alpha = 0.5f),
            dotSize = 2.dp,
            spacing = 1.dp,
            modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
        )
        content()
    }
}

@Composable
private fun SettingsCard(
    content: @Composable () -> Unit
) {
    androidx.compose.material3.Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = androidx.compose.material3.MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        content()
    }
}

@Composable
private fun RoundIconButton(
    text: String,
    onClick: () -> Unit,
    color: Color
) {
    androidx.compose.material3.Surface(
        modifier = Modifier
            .height(48.dp)
            .width(48.dp)
            .clickable { onClick() },
        shape = CircleShape,
        color = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Box(contentAlignment = Alignment.Center) {
            DotText(text = text, color = color, dotSize = 2.5.dp, spacing = 1.dp)
        }
    }
}

@Composable
private fun SegmentedControlButton(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onBg: Color,
    content: @Composable (Color) -> Unit
) {
    val bgColor = if (selected) onBg else Color.Transparent
    val textColor = if (selected) androidx.compose.material3.MaterialTheme.colorScheme.background else onBg.copy(alpha = 0.7f)
    
    androidx.compose.material3.Surface(
        modifier = modifier
            .height(56.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = bgColor
    ) {
        Box(contentAlignment = Alignment.Center) {
            content(textColor)
        }
    }
}
