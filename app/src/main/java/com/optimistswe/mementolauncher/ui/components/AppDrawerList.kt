package com.optimistswe.mementolauncher.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.optimistswe.mementolauncher.data.AppFolder
import com.optimistswe.mementolauncher.data.AppInfo
import com.optimistswe.mementolauncher.ui.screens.AppDrawerItem
import kotlinx.coroutines.launch

/**
 * Scrollable list of apps and folders for the app drawer, grouped alphabetically.
 *
 * Displays a flat list with expandable/collapsible folders and an alphabet scrubber
 * bar on the right edge for quick section navigation via tap or drag.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppDrawerList(
    groupedItems: Map<Char, List<AppDrawerItem>>,
    searchQuery: String,
    isFavorite: (String) -> Boolean,
    onLaunchApp: (String) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
    onFolderLongClick: (AppFolder) -> Unit,
    modifier: Modifier = Modifier
) {
    val hapticFeedback = LocalHapticFeedback.current
    val onBg = MaterialTheme.colorScheme.onBackground
    val dimmed = onBg.copy(alpha = 0.4f)
    val faint = onBg.copy(alpha = 0.15f)

    var collapsedFolders by remember { mutableStateOf(setOf<String>()) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Scroll to top when search query changes
    LaunchedEffect(searchQuery) {
        listState.scrollToItem(0)
    }

    // Build letter→LazyColumn-index map accounting for current folder expansion state.
    // Recalculated whenever groupedItems or collapsedFolders changes.
    val letterToIndex = remember(groupedItems, collapsedFolders) {
        val map = mutableMapOf<Char, Int>()
        var idx = 0
        groupedItems.forEach { (letter, items) ->
            map[letter] = idx
            items.forEach { item ->
                idx++ // the folder/app row itself
                if (item is AppDrawerItem.Folder && !collapsedFolders.contains(item.folder.id)) {
                    idx += if (item.resolvedApps.isEmpty()) 1 else item.resolvedApps.size
                }
            }
        }
        map
    }

    val letters = remember(groupedItems) { groupedItems.keys.toList() }

    Box(modifier = modifier) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                // Reserve space on the right for the alphabet bar
                .padding(end = 20.dp)
        ) {
            if (groupedItems.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        DotText(
                            text = "NO APPS FOUND",
                            color = dimmed,
                            dotSize = 2.dp,
                            spacing = 0.8.dp
                        )
                    }
                }
            } else {
                groupedItems.forEach { (_, itemsInGroup) ->
                    // No section headers — flat continuous list.
                    // The right-side alphabet bar handles section navigation.
                    itemsInGroup.forEach { item ->
                        when (item) {
                            is AppDrawerItem.Folder -> {
                                val folder = item.folder
                                val appsInFolder = item.resolvedApps

                                item(key = "folder_${folder.id}", contentType = "folder") {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color.Black)
                                            .combinedClickable(
                                                onClick = {
                                                    collapsedFolders = if (collapsedFolders.contains(folder.id)) {
                                                        collapsedFolders - folder.id
                                                    } else {
                                                        collapsedFolders + folder.id
                                                    }
                                                },
                                                onLongClick = {
                                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    onFolderLongClick(folder)
                                                }
                                            )
                                            .padding(vertical = 18.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            DotText(
                                                text = folder.name,
                                                color = dimmed,
                                                dotSize = 3.dp,
                                                spacing = 1.dp
                                            )
                                            DotText(
                                                text = if (collapsedFolders.contains(folder.id)) "+" else "−",
                                                color = faint,
                                                dotSize = 2.dp,
                                                spacing = 0.5.dp
                                            )
                                        }
                                    }
                                }

                                if (!collapsedFolders.contains(folder.id)) {
                                    if (appsInFolder.isEmpty()) {
                                        item(key = "empty_${folder.id}", contentType = "empty_folder") {
                                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 8.dp)) {
                                                DotText(
                                                    text = "EMPTY",
                                                    color = faint,
                                                    dotSize = 1.5.dp,
                                                    spacing = 0.5.dp
                                                )
                                            }
                                        }
                                    } else {
                                        items(
                                            items = appsInFolder,
                                            key = { "${folder.id}_${it.packageName}" },
                                            contentType = { "app" }
                                        ) { app ->
                                            AppListItem(
                                                app = app,
                                                isFav = isFavorite(app.packageName),
                                                onLaunchApp = onLaunchApp,
                                                onAppLongClick = onAppLongClick
                                            )
                                        }
                                    }
                                }
                            }

                            is AppDrawerItem.App -> {
                                val app = item.info
                                item(key = "app_${app.packageName}", contentType = "app") {
                                    AppListItem(
                                        app = app,
                                        isFav = isFavorite(app.packageName),
                                        onLaunchApp = onLaunchApp,
                                        onAppLongClick = onAppLongClick
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Alphabet scrubber — right-aligned, tap or drag to jump to a section
        if (letters.isNotEmpty()) {
            AlphabetIndexBar(
                letters = letters,
                faint = faint,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(18.dp),
                onLetterSelected = { letter ->
                    letterToIndex[letter]?.let { index ->
                        coroutineScope.launch {
                            listState.animateScrollToItem(index)
                        }
                    }
                }
            )
        }
    }
}

/**
 * Vertical A–Z scrubber bar. Supports both tap and continuous drag to navigate sections.
 */
@Composable
private fun AlphabetIndexBar(
    letters: List<Char>,
    faint: Color,
    onLetterSelected: (Char) -> Unit,
    modifier: Modifier = Modifier
) {
    var barHeightPx by remember { mutableIntStateOf(1) }

    Column(
        modifier = modifier
            .onSizeChanged { barHeightPx = it.height.coerceAtLeast(1) }
            .pointerInput(letters) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    fun yToLetter(y: Float): Char {
                        val idx = ((y / barHeightPx) * letters.size)
                            .toInt()
                            .coerceIn(0, letters.size - 1)
                        return letters[idx]
                    }
                    onLetterSelected(yToLetter(down.position.y))
                    drag(down.id) { change ->
                        onLetterSelected(yToLetter(change.position.y))
                    }
                }
            },
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        letters.forEach { letter ->
            Text(
                text = letter.toString(),
                color = faint,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 10.sp
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppListItem(
    app: AppInfo,
    isFav: Boolean,
    onLaunchApp: (String) -> Unit,
    onAppLongClick: (AppInfo) -> Unit
) {
    val hapticFeedback = LocalHapticFeedback.current
    val onBg = MaterialTheme.colorScheme.onBackground

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "AppRowScale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(color = onBg.copy(alpha = 0.2f)),
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onLaunchApp(app.packageName)
                },
                onLongClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onAppLongClick(app)
                }
            )
            .padding(vertical = 18.dp, horizontal = 8.dp)
    ) {
        DotText(
            text = app.label.uppercase(),
            color = if (isFav) onBg else onBg.copy(alpha = 0.7f),
            dotSize = 2.5.dp,
            spacing = 0.8.dp
        )
    }
}
