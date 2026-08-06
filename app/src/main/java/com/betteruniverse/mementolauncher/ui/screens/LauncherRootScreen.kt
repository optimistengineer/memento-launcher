package com.betteruniverse.mementolauncher.ui.screens

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
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.betteruniverse.mementolauncher.MainActivity
import com.betteruniverse.mementolauncher.data.BackgroundStyle
import com.betteruniverse.mementolauncher.ui.screens.WallpaperScreen
import com.betteruniverse.mementolauncher.data.CalendarTheme
import com.betteruniverse.mementolauncher.data.FontSize
import com.betteruniverse.mementolauncher.data.SearchBarPosition
import com.betteruniverse.mementolauncher.ui.LauncherViewModel
import com.betteruniverse.mementolauncher.ui.components.LauncherSettingsPanel
import com.betteruniverse.mementolauncher.ui.components.LocalFontScale
import com.betteruniverse.mementolauncher.ui.components.MindfulDelayOverlay
import com.betteruniverse.mementolauncher.ui.theme.MementoTheme
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
        if (preferences?.clockStyle == com.betteruniverse.mementolauncher.data.ClockStyle.H24_SEC) 1_000L else 30_000L
    }

    // Both refresh loops are gated on the STARTED lifecycle state and aligned to the wall clock,
    // fixing three problems the bare `LaunchedEffect { while(true) { ...; delay(n) } }` form had:
    //
    // 1. STALENESS AFTER SLEEP — the largest thing on the home screen showed the wrong time.
    //    delay() counts elapsed-realtime-while-awake, so a night of deep sleep does not advance
    //    it; and a LaunchedEffect survives the activity being stopped, so nothing re-ran on
    //    resume either. Waking the phone at 07:30 against a 23:00 last-tick showed "23:00" until
    //    the loop's next tick happened to land. repeatOnLifecycle cancels the block on ON_STOP
    //    and restarts it from the top on ON_START, so the first thing that happens on every
    //    return to the launcher is an immediate refresh.
    //
    // 2. PHASE DRIFT — a 30s poll with arbitrary phase displays a time up to ~30s behind even
    //    while awake. Sleeping until just past the next interval boundary
    //    (interval - now % interval) makes the refresh land right after the minute/second rolls.
    //
    // 3. BACKGROUND WORK FOREVER — the HOME activity lives for weeks, and these loops kept
    //    re-formatting the clock and making UsageStats/AlarmManager/AppOps binder calls every
    //    30s/60s the whole time the user was inside other apps, invisible. Now they simply stop
    //    while the launcher is not visible.
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner, clockIntervalMs) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                viewModel.refreshClock()
                delay(clockIntervalMs - (System.currentTimeMillis() % clockIntervalMs))
            }
        }
    }

    // Refresh system widgets (alarm, screen time) at a slower interval since they change infrequently.
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                viewModel.refreshWidgets()
                delay(60_000L)
            }
        }
    }

    // The life calendar page is optional (see UserPreferences.showLifeCalendar), so page
    // indices are derived rather than hardcoded:
    //   with calendar:    0 = Life Calendar, 1 = Home, 2 = App Drawer
    //   without calendar:              0 = Home, 1 = App Drawer
    //
    // Gate on preferences having loaded before building the pager. rememberPagerState captures
    // initialPage on first composition only, so composing it against the null-preferences
    // default and then flipping pageCount would silently land the user on the wrong page.
    val loadedPreferences = preferences ?: run {
        // Consume back during the cold-start window too. LauncherActivity no longer overrides
        // onBackPressed(), so without this, back while DataStore does its first read reaches the
        // dispatcher's fallback and finishes the HOME activity — a visible flash and relaunch.
        BackHandler(enabled = true) {}
        Box(modifier = Modifier.fillMaxSize().background(Color.Black))
        return
    }
    // Onboarding is not finished — hand the user back to it.
    //
    // Granting the HOME role makes this the home app *immediately*, and the system then delivers
    // a HOME intent as the role dialog closes. That started LauncherActivity on top of the
    // half-finished onboarding, and because the only gate here was isDefaultLauncher (now true)
    // the launcher rendered happily and steps 2 and 3 never ran. Because completeOnboarding()
    // was therefore never called, the damage was permanent, not merely a skipped screen:
    // is_setup_complete stayed false so every later cold start came straight back here, the
    // birth date was never collected (the life calendar page reads "SET YOUR BIRTH DATE"
    // forever), and scheduleWorker() never ran so the weekly wallpaper update was never
    // scheduled at all.
    //
    // MainActivity performs the mirror-image check (setup complete -> come here), and the two
    // conditions are exclusive, so this cannot ping-pong. Reordering to front reuses the
    // onboarding instance still sitting in the background with its progress intact instead of
    // starting a second one from step one.
    if (!loadedPreferences.isSetupComplete) {
        BackHandler(enabled = true) {}
        val onboardingContext = LocalContext.current
        LaunchedEffect(Unit) {
            runCatching {
                onboardingContext.startActivity(
                    Intent(onboardingContext, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            }
        }
        Box(modifier = Modifier.fillMaxSize().background(Color.Black))
        return
    }

    val showCalendar = loadedPreferences.showLifeCalendar
    val calendarPage = 0
    val homePage = if (showCalendar) 1 else 0
    val drawerPage = homePage + 1

    // Keyed on showCalendar: rememberPagerState only consumes initialPage on creation, but adding
    // or removing the calendar page renumbers every index. Without re-creating the state, closing
    // settings after enabling the calendar silently moved the user from the app drawer to Home
    // (currentPage 1 meant "drawer" before and "home" after).
    val pagerState = key(showCalendar) {
        rememberPagerState(
            initialPage = homePage,
            pageCount = { if (showCalendar) 3 else 2 }
        )
    }

    // Ensures that search is cleared whenever the user navigates away from the app drawer.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            if (page != drawerPage) {
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
    // (lifecycleOwner is declared once, up by the refresh loops.)
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
            // Same reason as the cold-start branch: this returns before the main BackHandler.
            BackHandler(enabled = true) {}
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
                    // Must follow the theme. Hardcoding black here while MementoTheme is set to
                    // LIGHT makes onBackground near-black, i.e. near-black text on a black
                    // ground — the entire launcher rendered invisible.
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Decorative backgrounds are suppressed on the life calendar page: that page is
                // itself thousands of dots on black, and anything behind it competes with the
                // very grid the user is trying to read.
                val onCalendarPage = showCalendar && pagerState.currentPage == calendarPage
                if (!onCalendarPage) {
                    when (loadedPreferences.backgroundStyle) {
                        BackgroundStyle.MATRIX_GRID -> MatrixGridBackground()
                        BackgroundStyle.STARFIELD -> StarfieldBackground()
                        BackgroundStyle.SOLID_BLACK -> Unit
                    }
                }

                val keyboardController = LocalSoftwareKeyboardController.current
                val focusManager = LocalFocusManager.current

                // Keyed on homePage as well as viewModel. viewModel never changes for the life of
                // the activity, so a collector started once kept the homePage value from the FIRST
                // composition — after the calendar was toggled, the HOME button then scrolled to a
                // stale index (or, when the stale index happened to equal currentPage, did nothing
                // at all) for as long as the activity lived, which for a HOME app is days.
                LaunchedEffect(viewModel, homePage) {
                    viewModel.homeIntentEvents.collect {
                        if (showSettingsDialog) {
                            showSettingsDialog = false
                        }
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        if (pagerState.currentPage != homePage) {
                            pagerState.scrollToPage(homePage)
                        }
                    }
                }

                // Always enabled so this consumes back on every API level. Leaving it disabled
                // on the home page would let back fall through and finish the activity on
                // API 33+; this is what keeps "back does nothing on home" true now that
                // LauncherActivity no longer swallows onBackPressed().
                BackHandler(enabled = true) {
                    when {
                        showSettingsDialog -> showSettingsDialog = false
                        pagerState.currentPage != homePage -> {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(homePage)
                            }
                        }
                        // Already on the home page — this IS the home screen, so do nothing.
                        else -> Unit
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    beyondViewportPageCount = 1,
                    userScrollEnabled = !showSettingsDialog
                ) { page ->
                    when {
                        showCalendar && page == calendarPage -> {
                            WallpaperScreen(
                                metrics = lifeMetrics,
                                lifeProgressText = lifeProgress,
                                isActive = pagerState.currentPage == calendarPage,
                                onOpenSettings = {
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                    showSettingsDialog = true
                                }
                            )
                        }
                        page == homePage -> {
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
                                showCalendar = showCalendar,
                                onLaunchApp = { pkg -> viewModel.requestAppLaunch(pkg, onLaunchApp) },
                                onRemoveFavorite = { viewModel.toggleFavorite(it) },
                                onOpenSearch = {
                                    coroutineScope.launch { pagerState.animateScrollToPage(drawerPage) }
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
                        page == drawerPage -> {
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
                                isVisible = pagerState.currentPage == drawerPage,
                                autoOpenKeyboard = preferences?.autoOpenKeyboard == true
                            )
                        }
                    }
                }

                if (showSettingsDialog) {
                    LauncherSettingsPanel(
                        birthDate = preferences?.birthDate,
                        lifeExpectancy = preferences?.lifeExpectancy ?: 80,
                        showLifeCalendar = showCalendar,
                        hasUsageAccess = viewModel.hasUsagePermission(),
                        onShowLifeCalendarChange = { viewModel.updateShowLifeCalendar(it) },
                        onBirthDateChange = { viewModel.updateBirthDate(it) },
                        onLifeExpectancyChange = { viewModel.updateLifeExpectancy(it) },
                        backgroundStyle = preferences?.backgroundStyle ?: BackgroundStyle.SOLID_BLACK,
                        onBackgroundStyleChange = { style -> viewModel.updateBackgroundStyle(style) },
                        fontSize = preferences?.fontSize ?: FontSize.MEDIUM,
                        onFontSizeChange = { size -> viewModel.updateFontSize(size) },
                        autoOpenKeyboard = preferences?.autoOpenKeyboard == true,
                        onAutoOpenKeyboardChange = { auto -> viewModel.updateAutoOpenKeyboard(auto) },
                        clockStyle = preferences?.clockStyle ?: com.betteruniverse.mementolauncher.data.ClockStyle.H24,
                        onClockStyleChange = { style -> viewModel.updateClockStyle(style) },
                        searchBarPosition = preferences?.searchBarPosition ?: SearchBarPosition.BOTTOM,
                        onSearchBarPositionChange = { pos -> viewModel.updateSearchBarPosition(pos) },
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
                        message = loadedPreferences.mindfulMessage,
                        packageName = appPkg,
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
 * A still night sky.
 *
 * Deliberately NOT animated. Twinkling would mean an infinite transition requesting a frame every
 * frame for as long as the home screen is composed, which for a HOME app is days — the same
 * mistake the birthday greeting and the current-week pulse each made once. This draws once and
 * then costs nothing until something else invalidates.
 *
 * The layout is generated from a fixed seed, so the sky is identical on every recomposition,
 * rotation and relaunch rather than reshuffling itself each time the user glances at it. Star
 * count scales with screen area so a tablet gets the same density rather than the same sparse
 * handful, and brightness follows a power curve: mostly faint pinpricks with a few brighter
 * standouts, which is what stops it reading as evenly-scattered noise.
 */
@Composable
private fun StarfieldBackground() {
    val onBg = MaterialTheme.colorScheme.onBackground
    val surface = MaterialTheme.colorScheme.background

    Canvas(modifier = Modifier.fillMaxSize().background(surface)) {
        val areaDp = (size.width / density) * (size.height / density)
        val count = (areaDp / 2600f).toInt().coerceIn(60, 260)
        val rng = kotlin.random.Random(20260807)
        repeat(count) {
            val x = rng.nextFloat() * size.width
            val y = rng.nextFloat() * size.height
            // Power curve: most values land near 0, a few reach 1 — faint dust, occasional star.
            val t = rng.nextFloat().let { it * it * it }
            val radius = (0.6f + t * 1.5f).dp.toPx()
            val alpha = 0.14f + t * 0.62f
            drawCircle(color = onBg.copy(alpha = alpha), radius = radius, center = Offset(x, y))
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
    val surface = MaterialTheme.colorScheme.background
    Canvas(modifier = Modifier.fillMaxSize().background(surface)) {
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
