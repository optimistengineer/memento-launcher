package com.betteruniverse.mementolauncher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.betteruniverse.mementolauncher.data.AppLabelRepository
import com.betteruniverse.mementolauncher.data.AppRepository
import com.betteruniverse.mementolauncher.data.FavoritesRepository
import com.betteruniverse.mementolauncher.data.FolderRepository
import com.betteruniverse.mementolauncher.data.PreferencesRepository
import com.betteruniverse.mementolauncher.ui.LauncherViewModel
import com.betteruniverse.mementolauncher.ui.screens.LauncherRootScreen

import dagger.hilt.android.AndroidEntryPoint

/**
 * Launcher Activity — registered as a HOME replacement.
 *
 * This activity is a thin entry point that initializes dependencies
 * and delegates UI rendering to [LauncherRootScreen].
 */
@AndroidEntryPoint
class LauncherActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Portrait-only on handsets, free rotation on large screens. Set here rather than in the
        // manifest because android:screenOrientation takes a literal enum and cannot be varied by
        // resource qualifier, and the phone/tablet split is the whole point — see res/values/
        // bools.xml. A side benefit: with rotation gone on phones, the Activity is no longer
        // recreated by turning the device, so nothing transient is lost that way either.
        applyOrientationLock()
        // Prevent Android from taking a visual snapshot of the launcher for Recents
        // window.setFlags(
        //     android.view.WindowManager.LayoutParams.FLAG_SECURE,
        //     android.view.WindowManager.LayoutParams.FLAG_SECURE
        // )

        enableEdgeToEdge()

        handleIntent(intent)

        setContent {
            LauncherRootScreen(
                viewModel = viewModel,
                onLaunchApp = { packageName -> launchApp(packageName) }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return

        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) {
            viewModel.onHomeIntentReceived()
        }

        if (intent.getBooleanExtra("EXTRA_MINDFUL_INTERRUPTION", false)) {
            val pkg = intent.getStringExtra("EXTRA_INTERRUPTED_PACKAGE")
            if (pkg != null) {
                viewModel.triggerMindfulInterruption(pkg)
            }
            // Clear extras so we don't accidentally re-trigger on screen rotation
            intent.removeExtra("EXTRA_MINDFUL_INTERRUPTION")
            intent.removeExtra("EXTRA_INTERRUPTED_PACKAGE")
        }
    }

    // NOTE: deliberately no onBackPressed() override.
    //
    // ComponentActivity.onBackPressed() is what drives onBackPressedDispatcher below API 33.
    // Overriding it with an empty body (as this class used to) swallowed back entirely on
    // API 26-32, so the BackHandler in LauncherRootScreen never ran there: the settings panel
    // could not be dismissed with back, and back would not return from the app drawer to home.
    // On API 33+ enableOnBackInvokedCallback routes around onBackPressed(), which is why the
    // two paths behaved differently.
    //
    // "Back does nothing on the home page" is now enforced by that BackHandler staying enabled
    // and consuming the event, which works identically on every supported API level.

    /**
     * Launches an app by its package name.
     */
    private fun launchApp(packageName: String) {
        try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
        } catch (_: Exception) {
            // App may have been uninstalled between tap and launch
        }
    }
}
