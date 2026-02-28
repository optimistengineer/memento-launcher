package com.optimistswe.mementolauncher

import android.os.Bundle
import android.util.DisplayMetrics
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.optimistswe.mementolauncher.data.PreferencesRepository
import com.optimistswe.mementolauncher.ui.MainViewModel
import com.optimistswe.mementolauncher.ui.navigation.Screen
import com.optimistswe.mementolauncher.ui.screens.HomeScreen
import com.optimistswe.mementolauncher.ui.screens.OnboardingScreen
import com.optimistswe.mementolauncher.ui.screens.SettingsScreen
import com.optimistswe.mementolauncher.ui.theme.MementoTheme
import com.optimistswe.mementolauncher.wallpaper.WallpaperUpdater
import com.optimistswe.mementolauncher.worker.WallpaperUpdateWorker
import android.app.Activity
import android.content.Intent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import com.optimistswe.mementolauncher.data.CalendarTheme
import com.optimistswe.mementolauncher.ui.components.LocalFontScale

/**
 * Main entry point for the Memento app.
 *
 * Sets up:
 * - Edge-to-edge display
 * - Jetpack Compose theming
 * - Navigation between screens
 * - ViewModel with dependencies
 */
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main entry point for the Memento app.
 *
 * Sets up:
 * - Edge-to-edge display
 * - Jetpack Compose theming
 * - Navigation between screens
 * - ViewModel with dependencies
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Get screen dimensions
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(metrics)
        val screenWidth = metrics.widthPixels
        val screenHeight = metrics.heightPixels

        setContent {
            MementoApp(
                screenWidth = screenWidth,
                screenHeight = screenHeight
            )
        }
    }
}

/**
 * Main composable for the Memento app.
 *
 * Handles navigation and screen routing based on user setup status.
 *
 * @param screenWidth Device screen width for preview generation
 * @param screenHeight Device screen height for preview generation
 */
@Composable
fun MementoApp(
    screenWidth: Int,
    screenHeight: Int
) {
    val navController = rememberNavController()

    val viewModel: MainViewModel = androidx.hilt.navigation.compose.hiltViewModel()

    // Set screen dimensions for bitmap generation
    LaunchedEffect(Unit) {
        viewModel.setScreenDimensions(screenWidth, screenHeight)
    }

    val preferences by viewModel.preferences.collectAsState()
    val metrics by viewModel.metrics.collectAsState()

    // Wait for preferences to load before showing UI
    if (preferences == null) return

    val context = LocalContext.current

    // If setup is complete, launch the Launcher Activity directly and close MainActivity
    if (preferences?.isSetupComplete == true) {
        LaunchedEffect(Unit) {
            val intent = Intent(context, LauncherActivity::class.java)
            context.startActivity(intent)
            (context as? Activity)?.finish()
        }
        return // Do not render the onboarding nav host
    }

    val isDark = preferences?.theme != CalendarTheme.LIGHT
    val fontScale = preferences?.fontSize?.scale ?: 1.0f

    MementoTheme(darkTheme = isDark) {
        CompositionLocalProvider(LocalFontScale provides fontScale) {
            NavHost(
                navController = navController,
                startDestination = Screen.Onboarding.route
            ) {
                composable(Screen.Onboarding.route) {
                    OnboardingScreen(
                        onComplete = { birthDate, lifeExpectancy ->
                            // Only trigger the DataStore write here.
                            // DO NOT call startActivity immediately — completeOnboarding is
                            // async (DataStore write inside a coroutine). If we launch
                            // LauncherActivity right now, it starts before the birth date is
                            // persisted, so WallpaperScreen reads null and shows "SET BIRTH DATE".
                            //
                            // The preferences flow in MementoApp already watches isSetupComplete.
                            // Once the write finishes, it emits true and the LaunchedEffect above
                            // handles the LauncherActivity transition — guaranteed to run only
                            // after the data is on disk.
                            viewModel.completeOnboarding(birthDate, lifeExpectancy)
                        }
                    )
                }

                composable(Screen.Home.route) {
                    HomeScreen(
                        metrics = metrics,
                        previewBitmap = viewModel.previewBitmap,
                        isLoading = viewModel.isLoading,
                        wallpaperSet = viewModel.wallpaperSet,
                        onSetWallpaper = { viewModel.setWallpaper() },
                        onRefresh = { viewModel.refresh() },
                        onSettingsClick = { navController.navigate(Screen.Settings.route) }
                    )
                }

                composable(Screen.Settings.route) {
                    preferences?.let { prefs ->
                        SettingsScreen(
                            preferences = prefs,
                            onBack = { navController.popBackStack() },
                            onBirthDateChange = { viewModel.updateBirthDate(it) },
                            onLifeExpectancyChange = { viewModel.updateLifeExpectancy(it) },
                            onWallpaperTargetChange = { viewModel.updateWallpaperTarget(it) },
                            onThemeChange = { viewModel.updateTheme(it) },
                            onDotStyleChange = { viewModel.updateDotStyle(it) },
                            onAutoOpenKeyboardChange = { viewModel.updateAutoOpenKeyboard(it) },
                            onBackgroundStyleChange = { viewModel.updateBackgroundStyle(it) },
                            onFontSizeChange = { viewModel.updateFontSize(it) }
                        )
                    }
                }
            }
        }
    }
}
