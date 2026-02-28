package com.optimistswe.mementolauncher.ui.components.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.optimistswe.mementolauncher.data.AppInfo
import com.optimistswe.mementolauncher.ui.components.DotText

/**
 * Settings section for managing the Quick Access Dock corners.
 *
 * Users can pick one app for the bottom-left corner and one for the bottom-right corner.
 */
@Composable
fun DockAppsSettings(
    allApps: List<AppInfo>,
    dockLeftPkg: String?,
    dockRightPkg: String?,
    onSetLeftApp: (String?) -> Unit,
    onSetRightApp: (String?) -> Unit,
    onBg: Color,
    bg: Color,
    dimmed: Color,
    surface: Color,
    cardBg: Color
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        SettingsSectionHeader(
            title = "QUICK ACCESS",
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
                DotText(
                    text = "CHOOSE APPS FOR BOTTOM CORNERS",
                    color = dimmed,
                    dotSize = 1.dp,
                    spacing = 0.4.dp
                )

                // LEFT CORNER
                DockCornerPicker(
                    label = "LEFT CORNER",
                    selectedPkg = dockLeftPkg,
                    allApps = allApps,
                    onSelect = onSetLeftApp,
                    onBg = onBg,
                    dimmed = dimmed,
                    cardBg = cardBg
                )

                // RIGHT CORNER
                DockCornerPicker(
                    label = "RIGHT CORNER",
                    selectedPkg = dockRightPkg,
                    allApps = allApps,
                    onSelect = onSetRightApp,
                    onBg = onBg,
                    dimmed = dimmed,
                    cardBg = cardBg
                )
            }
        }
    }
}

@Composable
private fun DockCornerPicker(
    label: String,
    selectedPkg: String?,
    allApps: List<AppInfo>,
    onSelect: (String?) -> Unit,
    onBg: Color,
    dimmed: Color,
    cardBg: Color
) {
    var pickerOpen by remember { mutableStateOf(false) }
    val selectedApp = allApps.find { it.packageName == selectedPkg }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DotText(
                text = label,
                color = onBg,
                dotSize = 1.5.dp,
                spacing = 0.5.dp
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DotText(
                    text = selectedApp?.label?.uppercase() ?: "NONE",
                    color = if (selectedApp != null) onBg else dimmed,
                    dotSize = 1.2.dp,
                    spacing = 0.4.dp,
                    modifier = Modifier.clickable { pickerOpen = !pickerOpen }
                )

                if (selectedPkg != null) {
                    DotText(
                        text = "X",
                        color = dimmed,
                        dotSize = 1.2.dp,
                        spacing = 0.4.dp,
                        modifier = Modifier.clickable { onSelect(null) }
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = pickerOpen,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cardBg, RoundedCornerShape(8.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val dockEligible = remember(allApps) {
                    allApps.filter { app ->
                        val pkg = app.packageName.lowercase()
                        pkg.contains("dialer") || pkg.contains("phone") ||
                        pkg.contains("messaging") || pkg.contains("mms") ||
                        pkg.contains("camera") ||
                        pkg.contains("calculator") ||
                        pkg.contains("maps") ||
                        pkg.contains("photos") || pkg.contains("gallery")
                    }.sortedBy { it.label.lowercase() }
                }
                dockEligible.forEach { app ->
                    val isSelected = app.packageName == selectedPkg
                    DotText(
                        text = app.label.uppercase(),
                        color = if (isSelected) onBg else dimmed,
                        dotSize = 1.2.dp,
                        spacing = 0.4.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(app.packageName)
                                pickerOpen = false
                            }
                            .padding(vertical = 6.dp)
                    )
                }
            }
        }
    }
}
