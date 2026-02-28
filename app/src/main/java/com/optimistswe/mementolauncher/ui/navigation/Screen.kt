package com.optimistswe.mementolauncher.ui.navigation

/**
 * Navigation destinations for the Memento app.
 *
 * Each destination is represented by its route string, used by NavController
 * to navigate between screens.
 */
sealed class Screen(val route: String) {
    /**
     * Onboarding screen for new users to enter their birth date.
     */
    data object Onboarding : Screen("onboarding")

    /**
     * Main screen showing the calendar preview and actions.
     */
    data object Home : Screen("home")

    /**
     * Settings screen for adjusting preferences.
     */
    data object Settings : Screen("settings")

    /**
     * Launcher home screen (the default home replacement).
     */
    data object LauncherHome : Screen("launcher_home")

    /**
     * Full app drawer with search and alphabetical listing.
     */
    data object AppDrawer : Screen("app_drawer")
}
