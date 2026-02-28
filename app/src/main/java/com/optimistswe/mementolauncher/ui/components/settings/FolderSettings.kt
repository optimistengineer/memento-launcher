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
import androidx.compose.ui.unit.sp
import com.optimistswe.mementolauncher.ui.components.DotText

/**
 * Settings section for managing custom app folders.
 *
 * Provides a button to trigger a folder creation dialog. Folders allow users to group related
 * apps together to reduce clutter in the main app drawer list.
 *
 * @param onCreateFolder Callback to persist a new folder with the given name.
 */
@Composable
fun FolderSettings(
    onCreateFolder: (String) -> Unit,
    onBg: Color,
    bg: Color,
    dimmed: Color,
    cardBg: Color
) {
    var expanded by remember { mutableStateOf(true) }
    var isCreatingFolder by remember { mutableStateOf(false) }
    var folderNameText by remember { mutableStateOf("") }

    Column {
        SettingsSectionHeader(
            title = "FOLDERS",
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DotText(
                    text = "GROUP APPS INTO CUSTOM FOLDERS",
                    color = dimmed.copy(alpha = 0.8f),
                    dotSize = 1.dp,
                    spacing = 0.4.dp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(onBg, RoundedCornerShape(12.dp))
                        .clickable { isCreatingFolder = true },
                    contentAlignment = Alignment.Center
                ) {
                    DotText(
                        text = "+ CREATE NEW FOLDER",
                        color = bg,
                        dotSize = 1.dp,
                        spacing = 0.5.dp
                    )
                }
            }
        }

        if (isCreatingFolder) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { isCreatingFolder = false }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .background(
                            MaterialTheme.colorScheme.surface,
                            RoundedCornerShape(24.dp)
                        )
                        .padding(24.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        DotText(
                            text = "NEW FOLDER",
                            color = onBg,
                            dotSize = 2.dp,
                            spacing = 0.6.dp
                        )

                        // Text input
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    onBg.copy(alpha = 0.08f),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            androidx.compose.foundation.text.BasicTextField(
                                value = folderNameText,
                                onValueChange = { folderNameText = it.uppercase() },
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    color = onBg,
                                    fontSize = 16.sp
                                ),
                                singleLine = true,
                                cursorBrush = androidx.compose.ui.graphics.SolidColor(onBg),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Cancel
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .background(
                                        onBg.copy(alpha = 0.08f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        folderNameText = ""
                                        isCreatingFolder = false
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                DotText(
                                    text = "CANCEL",
                                    color = dimmed,
                                    dotSize = 1.dp,
                                    spacing = 0.5.dp
                                )
                            }

                            // Create
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .background(
                                        onBg,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        if (folderNameText.isNotBlank()) {
                                            onCreateFolder(folderNameText.trim())
                                        }
                                        folderNameText = ""
                                        isCreatingFolder = false
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                DotText(
                                    text = "CREATE",
                                    color = bg,
                                    dotSize = 1.dp,
                                    spacing = 0.5.dp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
