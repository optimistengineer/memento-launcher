package com.optimistswe.mementolauncher.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.optimistswe.mementolauncher.MainActivity
import com.optimistswe.mementolauncher.data.BackgroundStyle
import com.optimistswe.mementolauncher.ui.screens.WallpaperScreen
import com.optimistswe.mementolauncher.data.CalendarTheme
import com.optimistswe.mementolauncher.data.FontSize
import com.optimistswe.mementolauncher.data.SearchBarPosition
import com.optimistswe.mementolauncher.ui.LauncherViewModel
import com.optimistswe.mementolauncher.ui.components.LauncherSettingsPanel
import com.optimistswe.mementolauncher.ui.components.LocalFontScale
import com.optimistswe.mementolauncher.ui.components.MindfulDelayOverlay
import com.optimistswe.mementolauncher.ui.theme.MementoTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The primary container for the Memento launcher's user interface.
 *
 * This screen manages:
 * 1. A three-page [HorizontalPager]:
 *    - Page 0: [WallpaperScreen] (Detailed life metrics and wallpaper settings)
 *    - Page 1: [LauncherHomeScreen] (Clock, life progress, and favorite apps)
 *    - Page 2: [AppDrawerScreen] (Searchable list of all installed apps and folders)
 * 2. Background rendering based on [BackgroundStyle] (Solid vs Matrix Grid).
 * 3. A [NestedScrollConnection] for detecting a "long right swipe" on the wallpaper page to
 *    trigger the settings overlay.
 * 4. System back button handling to return to the home page (Page 1) or close overlays.
 * 5. Lifecycle observation to ensure the app is the default launcher (Role Manager).
 *
 * @param viewModel The [LauncherViewModel] providing state and processing actions.
 * @param onLaunchApp Callback triggered when an app icon is clicked.
 */
@Composable
fun LauncherRootScreen(
    viewModel: LauncherViewModel,
    onLaunchApp: (String) -> Unit
) {
    val preferences by viewModel.preferences.collectAsState()
    val isDark = preferences?.theme != CalendarTheme.LIGHT
    val fontScale = preferences?.fontSize?.scale ?: 1.0f
    
    val interceptedApp by viewModel.interceptedLaunchPackage.collectAsState()

    // Hoist shared state outside pager to avoid redundant collectors & reduce recomposition scope
    val currentTime by viewModel.currentTime.collectAsState()
    val currentDate by viewModel.currentDate.collectAsState()
    val lifeProgress by viewModel.lifeProgressText.collectAsState()
    val lifeMetrics by viewModel.lifeMetrics.collectAsState()
    val favoriteApps by viewModel.favoriteApps.collectAsState()
    val dockLeft by viewModel.dockLeftApp.collectAsState()
    val dockRight by viewModel.dockRightApp.collectAsState()
    val nextAlarm by viewModel.nextAlarm.collectAsState()
    val screenTime by viewModel.screenTime.collectAsState()
    val isBirthdayState by viewModel.isBirthday.collectAsState()
    val groupedItems by viewModel.groupedDrawerItems.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val allApps by viewModel.allApps.collectAsState()

    // Stable lambda reference to prevent app drawer recomposition cascade
    val isFavoriteCheck = remember(viewModel) { { pkg: String -> viewModel.isFavorite(pkg) } }

    // Adapt clock refresh rate: 1s for seconds display, 30s otherwise
    val clockIntervalMs = remember(preferences?.clockStyle) {
        if (preferences?.clockStyle == com.optimistswe.mementolauncher.data.ClockStyle.H24_SEC) 1_000L else 30_000L
    }

    // Periodically refreshes the clock and system widgets while the root screen is active.
    LaunchedEffect(clockIntervalMs) {
        while (true) {
            viewModel.refreshClock()
            viewModel.refreshWidgets()
            delay(clockIntervalMs)
        }
    }

    // 3-page pager: 0 = Life Calendar, 1 = Home, 2 = App Drawer
    // Starts on Page 1 (Home) so swiping left reveals the calendar, right reveals the drawer.
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 3 })

    // Ensures that search is cleared whenever the user navigates away from the app drawer.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            if (page != 2) {
                viewModel.clearSearch()
            }
        }
    }

    val coroutineScope = rememberCoroutineScope()
    var showSettingsDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    var isDefaultLauncher by remember { mutableStateOf(true) }

    // --- Backup & Restore via SAF ---
    var backupStatusText by remember { mutableStateOf("") }
    var pendingBackupJson by remember { mutableStateOf<String?>(null) }

    val backupExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null && pendingBackupJson != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(pendingBackupJson!!.toByteArray())
                }
                backupStatusText = "BACKUP SAVED"
            } catch (_: Exception) {
                backupStatusText = "BACKUP FAILED"
            }
            pendingBackupJson = null
        }
    }

    val backupImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val json = context.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.bufferedReader().readText()
                }
                if (json != null) {
                    viewModel.importBackup(json) { success ->
                        backupStatusText = if (success) "RESTORE COMPLETE" else "RESTORE FAILED"
                    }
                } else {
                    backupStatusText = "RESTORE FAILED"
                }
            } catch (_: Exception) {
                backupStatusText = "RESTORE FAILED"
            }
        }
    }

    // Observes ON_RESUME lifecycle events to check if the app currently holds the HOME role.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    val rm = context.getSystemService(android.app.role.RoleManager::class.java)
                    isDefaultLauncher = rm?.isRoleHeld(android.app.role.RoleManager.ROLE_HOME) == true
                } else {
                    isDefaultLauncher = true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    MementoTheme(darkTheme = isDark) {
        if (!isDefaultLauncher) {
            DefaultLauncherScreen(
                onDismiss = { isDefaultLauncher = true }
            )
            return@MementoTheme
        }

        CompositionLocalProvider(LocalFontScale provides fontScale) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .background(Color.Black)
            ) {
                // Only show the matrix grid background on Home (page 1) and App Drawer (page 2).
                // The calendar page (page 0) is pure content — dots on black — so the grid
                // background would visually conflict with the life calendar grid itself.
                if (preferences?.backgroundStyle == BackgroundStyle.MATRIX_GRID
                    && pagerState.currentPage != 0) {
                    MatrixGridBackground()
                }

                val keyboardController = LocalSoftwareKeyboardController.current
                val focusManager = LocalFocusManager.current

                LaunchedEffect(viewModel) {
                    viewModel.homeIntentEvents.collect {
                        if (showSettingsDialog) {
                            showSettingsDialog = false
                        }
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        if (pagerState.currentPage != 1) {
                            pagerState.scrollToPage(1)
                        }
                    }
                }

                BackHandler(enabled = showSettingsDialog || pagerState.currentPage != 1) {
                    if (showSettingsDialog) {
                        showSettingsDialog = false
                    } else {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(1)
                        }
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    beyondViewportPageCount = 1,
                    userScrollEnabled = !showSettingsDialog
                ) { page ->
                    when (page) {
                        0 -> {
                            WallpaperScreen(
                                metrics = lifeMetrics,
                                lifeProgressText = lifeProgress,
                                onOpenSettings = {
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                    showSettingsDialog = true
                                }
                            )
                        }
                        1 -> {
                            LauncherHomeScreen(
                                currentTime = currentTime,
                                currentDate = currentDate,
                                favoriteApps = favoriteApps,
                                dockLeftApp = dockLeft,
                                dockRightApp = dockRight,
                                nextAlarm = nextAlarm,
                                screenTime = screenTime,
                                hasUsagePermission = viewModel.hasUsagePermission(),
                                isBirthday = isBirthdayState,
                                onLaunchApp = { pkg -> viewModel.requestAppLaunch(pkg, onLaunchApp) },
                                onRemoveFavorite = { viewModel.toggleFavorite(it) },
                                onOpenSearch = {
                                    coroutineScope.launch { pagerState.animateScrollToPage(2) }
                                },
                                onExpandNotifications = {
                                    try {
                                        val statusBarService = context.getSystemService("statusbar")
                                        statusBarService?.let {
                                            val method = it.javaClass.getMethod("expandNotificationsPanel")
                                            method.invoke(it)
                                        }
                                    } catch (_: Exception) {
                                        // Silently fail if reflection is blocked on this device/API level
                                    }
                                }
                            )
                        }
                        2 -> {
                            val searchBarPosition = preferences?.searchBarPosition ?: SearchBarPosition.TOP

                            AppDrawerScreen(
                                groupedItems = groupedItems,
                                folders = folders,
                                searchBarPosition = searchBarPosition,
                                searchQuery = searchQuery,
                                onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                                onClearSearch = { viewModel.clearSearch() },
                                onLaunchApp = { pkg -> viewModel.requestAppLaunch(pkg, onLaunchApp) },
                                onToggleFavorite = { viewModel.toggleFavorite(it) },
                                isFavorite = isFavoriteCheck,
                                onRenameApp = { pkg, label -> viewModel.renameApp(pkg, label) },
                                onDeleteFolder = { id -> viewModel.deleteFolder(id) },
                                onRenameFolder = { id, name -> viewModel.renameFolder(id, name) },
                                onAddAppToFolder = { id, pkg -> viewModel.addAppToFolder(id, pkg) },
                                onRemoveAppFromFolder = { id, pkg -> viewModel.removeAppFromFolder(id, pkg) },
                                onHideApp = { viewModel.toggleAppVisibility(it) },
                                onOpenSettings = {
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                    showSettingsDialog = true
                                },
                                isVisible = pagerState.currentPage == 2,
                                autoOpenKeyboard = preferences?.autoOpenKeyboard == true
                            )
                        }
                    }
                }

                if (showSettingsDialog) {
                    LauncherSettingsPanel(
                        birthDate = preferences?.birthDate,
                        lifeExpectancy = preferences?.lifeExpectancy ?: 80,
                        onBirthDateChange = { viewModel.updateBirthDate(it) },
                        onLifeExpectancyChange = { viewModel.updateLifeExpectancy(it) },
                        backgroundStyle = preferences?.backgroundStyle ?: BackgroundStyle.SOLID_BLACK,
                        onBackgroundStyleChange = { style -> viewModel.updateBackgroundStyle(style) },
                        fontSize = preferences?.fontSize ?: FontSize.MEDIUM,
                        onFontSizeChange = { size -> viewModel.updateFontSize(size) },
                        autoOpenKeyboard = preferences?.autoOpenKeyboard == true,
                        onAutoOpenKeyboardChange = { auto -> viewModel.updateAutoOpenKeyboard(auto) },
                        clockStyle = preferences?.clockStyle ?: com.optimistswe.mementolauncher.data.ClockStyle.H24,
                        onClockStyleChange = { style -> viewModel.updateClockStyle(style) },
                        searchBarPosition = preferences?.searchBarPosition ?: SearchBarPosition.BOTTOM,
                        onSearchBarPositionChange = { pos -> viewModel.updateSearchBarPosition(pos) },
                        blockShortFormContent = preferences?.blockShortFormContent ?: false,
                        onBlockShortFormContentChange = { enabled -> viewModel.updateBlockShortFormContent(enabled) },
                        usageNudgeEnabled = preferences?.usageNudgeEnabled ?: false,
                        onUsageNudgeEnabledChange = { viewModel.updateUsageNudgeEnabled(it) },
                        usageNudgeMinutes = preferences?.usageNudgeMinutes ?: 15,
                        onUsageNudgeMinutesChange = { viewModel.updateUsageNudgeMinutes(it) },
                        onCreateFolder = { name -> viewModel.createFolder(name) },
                        allApps = allApps,
                        hiddenPackages = preferences?.hiddenPackages ?: emptySet(),
                        onSetHiddenPackages = { viewModel.setHiddenPackages(it) },
                        distractingPackages = preferences?.distractingPackages ?: emptySet(),
                        onSetDistractingPackages = { viewModel.setDistractingPackages(it) },
                        mindfulMessage = preferences?.mindfulMessage ?: "IS THIS\nINTENTIONAL?",
                        onMindfulMessageChange = { msg -> viewModel.updateMindfulMessage(msg) },
                        dockLeftPkg = dockLeft?.packageName,
                        dockRightPkg = dockRight?.packageName,
                        onSetDockLeft = { viewModel.setDockLeftApp(it) },
                        onSetDockRight = { viewModel.setDockRightApp(it) },
                        onBackup = {
                            viewModel.getBackupJson { json ->
                                if (json != null) {
                                    pendingBackupJson = json
                                    backupExportLauncher.launch("memento_backup.json")
                                } else {
                                    backupStatusText = "BACKUP FAILED"
                                }
                            }
                        },
                        onRestore = {
                            backupImportLauncher.launch(arrayOf("application/json"))
                        },
                        backupStatusText = backupStatusText,
                        onDismiss = { showSettingsDialog = false }
                    )
                }

                interceptedApp?.let { appPkg ->
                    val appName = viewModel.allApps.value.find { it.packageName == appPkg }?.label
                    MindfulDelayOverlay(
                        message = preferences?.mindfulMessage ?: "IS THIS\nINTENTIONAL?",
                        appName = appName,
                        onProceed = {
                            onLaunchApp(appPkg)
                            coroutineScope.launch {
                                delay(1000)
                                viewModel.clearInterceptedLaunch()
                            }
                        },
                        onCancel = {
                            viewModel.clearInterceptedLaunch()
                        }
                    )
                }
            }
        }
    }
}

/**
 * Renders a minimalist grid of subtle dots across the screen.
 * Used for the [BackgroundStyle.MATRIX_GRID] aesthetic.
 */
@Composable
private fun MatrixGridBackground() {
    val dotMatrixColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f)
    Canvas(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        val spacing = 48.dp.toPx()
        val radius = 2.dp.toPx()
        val rows = (size.height / spacing).toInt() + 1
        val cols = (size.width / spacing).toInt() + 1
        for (r in 0..rows) {
            for (c in 0..cols) {
                drawCircle(
                    color = dotMatrixColor,
                    radius = radius,
                    center = Offset(c * spacing, r * spacing)
                )
            }
        }
    }
}
