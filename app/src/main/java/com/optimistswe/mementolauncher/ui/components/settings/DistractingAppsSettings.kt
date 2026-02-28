package com.optimistswe.mementolauncher.ui.components.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.border
import com.optimistswe.mementolauncher.data.AppInfo
import com.optimistswe.mementolauncher.ui.components.DotText

private val WHITESPACE_REGEX = Regex("\\s+")

/**
 * Settings section for managing apps that trigger the Mindful Launch Delay.
 *
 * Displays the full list of installed apps. Toggled apps are added to 
 * [distractingPackages] and will trigger a delay overlay when launched.
 *
 * @param allApps The full list of apps installed on the device.
 * @param distractingPackages The set of package names currently flagged as distracting.
 * @param onToggleDistracting Callback to flag/unflag an app.
 */
@Composable
fun DistractingAppsSettings(
    allApps: List<AppInfo>,
    distractingPackages: Set<String>,
    onToggleDistracting: (String) -> Unit,
    mindfulMessage: String,
    onMindfulMessageChange: (String) -> Unit,
    onBg: Color,
    bg: Color,
    dimmed: Color,
    surface: Color,
    cardBg: Color
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        SettingsSectionHeader(
            title = "MINDFUL DELAY APPS",
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Custom Message Input
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DotText(
                        text = "CUSTOM MESSAGE (10 WORDS MAX)",
                        color = dimmed,
                        dotSize = 1.dp,
                        spacing = 0.4.dp
                    )
                    
                    var localMessage by remember { mutableStateOf(TextFieldValue(mindfulMessage)) }

                    LaunchedEffect(mindfulMessage) {
                        if (mindfulMessage != localMessage.text) {
                            localMessage = localMessage.copy(text = mindfulMessage)
                        }
                    }

                    var showSavedStatus by remember { mutableStateOf(false) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BasicTextField(
                            value = localMessage,
                            onValueChange = { newValue ->
                                val words = newValue.text.trim().split(WHITESPACE_REGEX)
                                if (words.size <= 10 || newValue.text.length < localMessage.text.length) {
                                    localMessage = newValue
                                    showSavedStatus = false
                                }
                            },
                            textStyle = TextStyle(
                                color = onBg,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .background(surface, RoundedCornerShape(8.dp))
                                .border(1.dp, onBg.copy(alpha=0.15f), RoundedCornerShape(8.dp))
                                .padding(16.dp)
                        )
                        
                        val isEdited = localMessage.text != mindfulMessage
                        val isSaved = showSavedStatus || (!isEdited && mindfulMessage.isNotEmpty())

                        Box(
                            modifier = Modifier
                                .height(56.dp)
                                .background(if (isSaved) dimmed.copy(alpha=0.15f) else onBg, RoundedCornerShape(8.dp))
                                .clickable(enabled = isEdited) {
                                    if (isEdited) {
                                        onMindfulMessageChange(localMessage.text)
                                        showSavedStatus = true
                                    }
                                }
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            DotText(
                                text = if (isSaved) "SAVED" else "SAVE",
                                color = if (isSaved) dimmed else bg,
                                dotSize = 1.8.dp,
                                spacing = 0.5.dp
                            )
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(onBg.copy(alpha=0.15f)))

                if (allApps.isEmpty()) {
                    DotText(
                        text = "LOADING APPS...",
                        color = dimmed,
                        dotSize = 1.dp,
                        spacing = 0.4.dp
                    )
                } else {
                    val sortedApps = remember(allApps) { allApps.sortedBy { it.label.lowercase() } }
                    sortedApps.forEach { app ->
                        val isDistracting = distractingPackages.contains(app.packageName)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                DotText(
                                    text = app.label.uppercase(),
                                    color = onBg,
                                    dotSize = 1.2.dp,
                                    spacing = 0.5.dp
                                )
                            }
                            
                            Switch(
                                checked = isDistracting,
                                onCheckedChange = { onToggleDistracting(app.packageName) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = bg,
                                    checkedTrackColor = onBg,
                                    uncheckedThumbColor = dimmed,
                                    uncheckedTrackColor = surface
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
