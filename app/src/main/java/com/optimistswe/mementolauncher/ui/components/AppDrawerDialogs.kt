package com.optimistswe.mementolauncher.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.optimistswe.mementolauncher.data.AppFolder
import com.optimistswe.mementolauncher.data.AppInfo

/**
 * Hosts all dialogs triggered from the app drawer: app options, folder actions,
 * add-to-folder picker, app rename, and folder rename.
 *
 * Only one dialog is visible at a time, controlled by the nullable state parameters.
 */
@Composable
fun AppDrawerDialogs(
    selectedAppForMenu: AppInfo?,
    onCloseAppMenu: () -> Unit,
    isFavorite: (String) -> Boolean,
    onToggleFavorite: (String) -> Unit,
    onHideApp: (String) -> Unit,
    folders: List<AppFolder>,
    onRemoveAppFromFolder: (String, String) -> Unit,
    onOpenAddToFolder: (AppInfo) -> Unit,
    onOpenRenameApp: (AppInfo) -> Unit,
    
    selectedFolderForMenu: AppFolder?,
    onCloseFolderMenu: () -> Unit,
    onOpenRenameFolder: (AppFolder) -> Unit,
    onDeleteFolder: (String) -> Unit,
    
    addToFolderApp: AppInfo?,
    onCloseAddToFolder: () -> Unit,
    onAddAppToFolder: (String, String) -> Unit,
    
    appToRename: AppInfo?,
    onCloseRenameApp: () -> Unit,
    onRenameApp: (String, String) -> Unit,
    
    folderToRename: AppFolder?,
    onCloseRenameFolder: () -> Unit,
    onRenameFolder: (String, String) -> Unit,
    
    renameText: String,
    onRenameTextChange: (String) -> Unit,
    
    context: Context
) {
    val onBg = MaterialTheme.colorScheme.onBackground
    val bg = MaterialTheme.colorScheme.background
    val dimmed = onBg.copy(alpha = 0.4f)
    val faint = onBg.copy(alpha = 0.15f)

    // ═══════════════════════════════════════════
    // APP OPTIONS DIALOG
    // ═══════════════════════════════════════════
    if (selectedAppForMenu != null) {
        val app = selectedAppForMenu
        val isFav = isFavorite(app.packageName)

        Dialog(onDismissRequest = onCloseAppMenu) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Title
                    DotText(
                        text = app.label.uppercase(),
                        color = onBg,
                        dotSize = 2.5.dp,
                        spacing = 0.8.dp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Favorite
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onToggleFavorite(app.packageName)
                                onCloseAppMenu()
                            }
                            .padding(vertical = 12.dp)
                    ) {
                        DotText(
                            text = if (isFav) "REMOVE FROM HOME" else "ADD TO HOME",
                            color = dimmed,
                            dotSize = 1.5.dp,
                            spacing = 0.5.dp
                        )
                    }

                    // App Info
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = android.net.Uri.parse("package:${app.packageName}")
                                }
                                context.startActivity(intent)
                                onCloseAppMenu()
                            }
                            .padding(vertical = 12.dp)
                    ) {
                        DotText(
                            text = "APP INFO",
                            color = dimmed,
                            dotSize = 1.5.dp,
                            spacing = 0.5.dp
                        )
                    }

                    // Hide App
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onHideApp(app.packageName)
                                onCloseAppMenu()
                            }
                            .padding(vertical = 12.dp)
                    ) {
                        DotText(
                            text = "HIDE",
                            color = dimmed,
                            dotSize = 1.5.dp,
                            spacing = 0.5.dp
                        )
                    }

                    // Folder assignment
                    val inFolderId = folders.find { it.packages.contains(app.packageName) }?.id
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (inFolderId != null) {
                                    onRemoveAppFromFolder(inFolderId, app.packageName)
                                } else {
                                    onOpenAddToFolder(app)
                                }
                                onCloseAppMenu()
                            }
                            .padding(vertical = 12.dp)
                    ) {
                        DotText(
                            text = if (inFolderId != null) "REMOVE FROM FOLDER" else "ADD TO FOLDER",
                            color = dimmed,
                            dotSize = 1.5.dp,
                            spacing = 0.5.dp
                        )
                    }

                    // Uninstall
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val intent = Intent(Intent.ACTION_DELETE).apply {
                                    data = android.net.Uri.fromParts("package", app.packageName, null)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                                }
                                context.startActivity(intent)
                                onCloseAppMenu()
                            }
                            .padding(vertical = 12.dp)
                    ) {
                        DotText(
                            text = "UNINSTALL",
                            color = dimmed,
                            dotSize = 1.5.dp,
                            spacing = 0.5.dp
                        )
                    }

                    // Rename
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onOpenRenameApp(app)
                                onCloseAppMenu()
                            }
                            .padding(vertical = 12.dp)
                    ) {
                        DotText(
                            text = "RENAME",
                            color = dimmed,
                            dotSize = 1.5.dp,
                            spacing = 0.5.dp
                        )
                    }
                }
            }
        }
    }

    // ═══════════════════════════════════════════
    // FOLDER ACTIONS DIALOG
    // ═══════════════════════════════════════════
    if (selectedFolderForMenu != null) {
        val folder = selectedFolderForMenu
        Dialog(onDismissRequest = onCloseFolderMenu) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                    .padding(24.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    DotText(
                        text = folder.name.uppercase(),
                        color = onBg,
                        dotSize = 2.5.dp,
                        spacing = 0.8.dp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onOpenRenameFolder(folder)
                                onCloseFolderMenu()
                            }
                            .padding(vertical = 12.dp)
                    ) {
                        DotText(
                            text = "RENAME FOLDER",
                            color = dimmed,
                            dotSize = 1.5.dp,
                            spacing = 0.5.dp
                        )
                    }
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onDeleteFolder(folder.id)
                                onCloseFolderMenu()
                            }
                            .padding(vertical = 12.dp)
                    ) {
                        DotText(
                            text = "DELETE FOLDER",
                            color = dimmed,
                            dotSize = 1.5.dp,
                            spacing = 0.5.dp
                        )
                    }
                }
            }
        }
    }

    // ═══════════════════════════════════════════
    // ADD APP TO FOLDER DIALOG
    // ═══════════════════════════════════════════
    if (addToFolderApp != null) {
        Dialog(onDismissRequest = onCloseAddToFolder) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                    .padding(24.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    DotText(
                        text = "ADD TO FOLDER",
                        color = onBg,
                        dotSize = 2.dp,
                        spacing = 0.6.dp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    if (folders.isEmpty()) {
                        DotText(
                            text = "NO FOLDERS YET",
                            color = faint,
                            dotSize = 1.dp,
                            spacing = 0.5.dp
                        )
                    } else {
                        LazyColumn {
                            items(folders) { folder ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onAddAppToFolder(folder.id, addToFolderApp.packageName)
                                            onCloseAddToFolder()
                                        }
                                        .padding(vertical = 12.dp)
                                ) {
                                    DotText(
                                        text = folder.name.uppercase(),
                                        color = dimmed,
                                        dotSize = 1.5.dp,
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

    // ═══════════════════════════════════════════
    // RENAME APP DIALOG
    // ═══════════════════════════════════════════
    appToRename?.let { app ->
        Dialog(
            onDismissRequest = onCloseRenameApp
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
                        text = "RENAME APP",
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
                        BasicTextField(
                            value = renameText,
                            onValueChange = onRenameTextChange,
                            textStyle = TextStyle(
                                color = onBg,
                                fontSize = 16.sp
                            ),
                            singleLine = true,
                            cursorBrush = SolidColor(onBg),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Reset to original
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .background(
                                    onBg.copy(alpha = 0.08f),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    onRenameApp(app.packageName, "")
                                    onCloseRenameApp()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            DotText(
                                text = "RESET",
                                color = dimmed,
                                dotSize = 1.dp,
                                spacing = 0.5.dp
                            )
                        }

                        // Save
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .background(
                                    onBg,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    if (renameText.isNotBlank()) {
                                        onRenameApp(app.packageName, renameText.trim())
                                    }
                                    onCloseRenameApp()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            DotText(
                                text = "SAVE",
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
    
    // ═══════════════════════════════════════════
    // RENAME FOLDER DIALOG
    // ═══════════════════════════════════════════
    folderToRename?.let { folder ->
        Dialog(
            onDismissRequest = onCloseRenameFolder
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
                        text = "RENAME FOLDER",
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
                        BasicTextField(
                            value = renameText,
                            onValueChange = { onRenameTextChange(it.uppercase()) }, // Auto uppercase folders
                            textStyle = TextStyle(
                                color = onBg,
                                fontSize = 16.sp
                            ),
                            singleLine = true,
                            cursorBrush = SolidColor(onBg),
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
                                    onCloseRenameFolder()
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

                        // Save
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .background(
                                    onBg,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    if (renameText.isNotBlank()) {
                                        onRenameFolder(folder.id, renameText.trim())
                                    }
                                    onCloseRenameFolder()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            DotText(
                                text = "SAVE",
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
