package com.optimistswe.mementolauncher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.optimistswe.mementolauncher.data.AppLabelRepository
import com.optimistswe.mementolauncher.data.AppRepository
import com.optimistswe.mementolauncher.data.FavoritesRepository
import com.optimistswe.mementolauncher.data.FolderRepository
import com.optimistswe.mementolauncher.data.PreferencesRepository
import com.optimistswe.mementolauncher.ui.LauncherViewModel
import com.optimistswe.mementolauncher.ui.screens.LauncherRootScreen

import dagger.hilt.android.AndroidEntryPoint

/**
 * Launcher Activity — registered as a HOME replacement.
 *
 * This activity is a thin entry point that initializes dependencies
 * and delegates UI rendering to [LauncherRootScreen].
 */
@AndroidEntryPoint
class LauncherActivity : ComponentActivity() {

    private lateinit var viewModel: LauncherViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Prevent Android from taking a visual snapshot of the launcher for Recents
        // window.setFlags(
        //     android.view.WindowManager.LayoutParams.FLAG_SECURE,
        //     android.view.WindowManager.LayoutParams.FLAG_SECURE
        // )
        
        enableEdgeToEdge()
        viewModel = androidx.lifecycle.ViewModelProvider(this)[LauncherViewModel::class.java]

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

    /**
     * Override back press to do nothing — standard launcher behavior.
     */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Do nothing — this IS the home screen
    }

    /**
     * Launches an app by its package name.
     */
    private fun launchApp(packageName: String) {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
    }
}
