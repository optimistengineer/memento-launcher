package com.optimistswe.mementolauncher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import com.optimistswe.mementolauncher.data.AppFolder
import com.optimistswe.mementolauncher.data.AppInfo
import com.optimistswe.mementolauncher.data.SearchBarPosition
import com.optimistswe.mementolauncher.ui.components.AppDrawerDialogs
import com.optimistswe.mementolauncher.ui.components.AppDrawerList
import com.optimistswe.mementolauncher.ui.components.AppDrawerSearchBar
import com.optimistswe.mementolauncher.ui.components.DotText

/**
 * Represents an item that can be displayed within the [AppDrawerScreen].
 *
 * This diskriminant union handles both individual apps and custom grouping folders.
 */
sealed class AppDrawerItem {
    /** The text label used for alphabetical sorting and display. */
    abstract val displayName: String

    /** A standard installed application. */
    data class App(val info: AppInfo) : AppDrawerItem() {
        override val displayName = info.label
    }

    /** A user-defined folder containing one or more apps. */
    data class Folder(val folder: AppFolder, val resolvedApps: List<AppInfo>) : AppDrawerItem() {
        override val displayName = folder.name
    }
}

/**
 * The application drawer screen, providing a searchable list of all apps and folders.
 *
 * This screen supports:
 * 1. Categorized alphabetical listing of apps and custom folders.
 * 2. Real-time search/filtering.
 * 3. Configurable search bar position (Top or Bottom) via [searchBarPosition].
 * 4. Context menus (long-press) for both apps (rename, hide, favorite, add to folder)
 *    and folders (rename, delete).
 * 5. Automatic keyboard management via [autoOpenKeyboard].
 *
 * @param groupedItems Map of first characters to the list of items starting with that character.
 * @param folders List of all custom folders for the add-to-folder dialog.
 * @param searchBarPosition Whether the search bar is at the top or bottom of the screen.
 * @param searchQuery Current search text.
 * @param onSearchQueryChange Callback for search text updates.
 * @param onClearSearch Callback to reset search.
 * @param onLaunchApp Callback to start an app.
 * @param onToggleFavorite Callback to pin/unpin an app.
 * @param isFavorite Check if an app is currently pinned.
 * @param onRenameApp Callback to set a custom alias for an app.
 * @param onDeleteFolder Callback to remove a folder.
 * @param onRenameFolder Callback to rename a folder.
 * @param onAddAppToFolder Callback to put an app inside a folder.
 * @param onRemoveAppFromFolder Callback to take an app out of a folder.
 * @param onHideApp Callback to hide an app from the drawer.
 * @param onOpenSettings Callback to open the settings overlay.
 * @param isVisible Whether this screen is currently the selected page in the root pager.
 * @param autoOpenKeyboard If true, the search field requests focus when the screen becomes visible.
 */
@Composable
fun AppDrawerScreen(
    groupedItems: Map<Char, List<AppDrawerItem>>,
    folders: List<AppFolder>,
    searchBarPosition: SearchBarPosition,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onLaunchApp: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    isFavorite: (String) -> Boolean,
    onRenameApp: (String, String) -> Unit,
    onDeleteFolder: (String) -> Unit,
    onRenameFolder: (String, String) -> Unit,
    onAddAppToFolder: (String, String) -> Unit,
    onRemoveAppFromFolder: (String, String) -> Unit,
    onHideApp: (String) -> Unit,
    onOpenSettings: () -> Unit,
    isVisible: Boolean,
    autoOpenKeyboard: Boolean = false
) {
    // Dialog State
    var selectedAppForMenu by remember { mutableStateOf<AppInfo?>(null) }
    var selectedFolderForMenu by remember { mutableStateOf<AppFolder?>(null) }
    var appToRename by remember { mutableStateOf<AppInfo?>(null) }
    var folderToRename by remember { mutableStateOf<AppFolder?>(null) }
    var addToFolderApp by remember { mutableStateOf<AppInfo?>(null) }
    var renameText by remember { mutableStateOf("") }
    
    val context = LocalContext.current
    val faint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f)

    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Handle auto-focus and keyboard (moved here so position toggle doesn't re-trigger it)
    LaunchedEffect(isVisible) {
        if (isVisible) {
            if (autoOpenKeyboard) {
                focusRequester.requestFocus()
                keyboardController?.show()
            }
        } else {
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .imePadding()
            .padding(top = 24.dp, bottom = 16.dp, start = 28.dp, end = 28.dp)
    ) {
        val searchBar = remember {
            movableContentOf<String> { query ->
                AppDrawerSearchBar(
                    searchQuery = query,
                    onSearchQueryChange = onSearchQueryChange,
                    onOpenSettings = onOpenSettings,
                    focusRequester = focusRequester,
                    focusManager = focusManager
                )
            }
        }

        if (searchBarPosition == SearchBarPosition.TOP) {
            searchBar(searchQuery)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Hint for favorites
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            DotText(
                text = "LONG PRESS FOR OPTIONS",
                color = faint,
                dotSize = 1.5.dp,
                spacing = 0.5.dp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // All combined dialogs
        AppDrawerDialogs(
            selectedAppForMenu = selectedAppForMenu,
            onCloseAppMenu = { selectedAppForMenu = null },
            isFavorite = isFavorite,
            onToggleFavorite = onToggleFavorite,
            onHideApp = onHideApp,
            folders = folders,
            onRemoveAppFromFolder = onRemoveAppFromFolder,
            onOpenAddToFolder = { addToFolderApp = it },
            onOpenRenameApp = { 
                appToRename = it
                renameText = it.label 
            },
            selectedFolderForMenu = selectedFolderForMenu,
            onCloseFolderMenu = { selectedFolderForMenu = null },
            onOpenRenameFolder = { 
                folderToRename = it
                renameText = it.name 
            },
            onDeleteFolder = onDeleteFolder,
            addToFolderApp = addToFolderApp,
            onCloseAddToFolder = { addToFolderApp = null },
            onAddAppToFolder = onAddAppToFolder,
            appToRename = appToRename,
            onCloseRenameApp = { appToRename = null },
            onRenameApp = onRenameApp,
            folderToRename = folderToRename,
            onCloseRenameFolder = { folderToRename = null },
            onRenameFolder = onRenameFolder,
            renameText = renameText,
            onRenameTextChange = { renameText = it },
            context = context
        )

        // Alphabetical List
        AppDrawerList(
            groupedItems = groupedItems,
            searchQuery = searchQuery,
            isFavorite = isFavorite,
            onLaunchApp = { pkg ->
                keyboardController?.hide()
                focusManager.clearFocus()
                onLaunchApp(pkg)
            },
            onAppLongClick = { selectedAppForMenu = it },
            onFolderLongClick = { selectedFolderForMenu = it },
            modifier = Modifier.weight(1f)
        )

        if (searchBarPosition == SearchBarPosition.BOTTOM) {
            Spacer(modifier = Modifier.height(16.dp))
            searchBar(searchQuery)
        }
    }
}
