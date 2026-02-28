package com.optimistswe.mementolauncher.ui.components.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.optimistswe.mementolauncher.data.AppInfo
import com.optimistswe.mementolauncher.ui.components.DotText

/**
 * Settings section for managing app visibility.
 *
 * Displays the full list of installed apps with toggle switches. Toggled apps are added to 
 * [hiddenPackages] and filtered out of the [AppDrawerScreen] and search results.
 *
 * @param allApps The full list of apps installed on the device.
 * @param hiddenPackages The set of package names currently hidden from the user.
 * @param onToggleVisibility Callback to hide/show an app.
 */
@Composable
fun HiddenAppsSettings(
    allApps: List<AppInfo>,
    hiddenPackages: Set<String>,
    onToggleVisibility: (String) -> Unit,
    onBg: Color,
    bg: Color,
    dimmed: Color,
    surface: Color,
    cardBg: Color
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        SettingsSectionHeader(
            title = "HIDDEN APPS",
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
                if (allApps.isEmpty()) {
                    DotText(
                        text = "LOADING APPS...",
                        color = dimmed,
                        dotSize = 1.dp,
                        spacing = 0.4.dp
                    )
                } else {
                    val hiddenApps = remember(allApps, hiddenPackages) {
                        allApps.filter { hiddenPackages.contains(it.packageName) }
                            .sortedBy { it.label.lowercase() }
                    }

                    if (hiddenApps.isEmpty()) {
                        DotText(
                            text = "NO HIDDEN APPS",
                            color = dimmed,
                            dotSize = 1.2.dp,
                            spacing = 0.4.dp
                        )
                    } else {
                        hiddenApps.forEach { app ->
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
                                    checked = true, // They are definitely hidden at this point
                                    onCheckedChange = { onToggleVisibility(app.packageName) },
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
}
