package com.optimistswe.mementolauncher.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

/**
 * Repository for querying and observing installed launchable apps.
 *
 * Uses [PackageManager] to retrieve the list of apps that have a launcher
 * intent, and a [BroadcastReceiver] to observe app installs/uninstalls
 * in real-time.
 *
 * @param context Application context for accessing PackageManager
 */
class AppRepository(private val context: Context) {

    /**
     * Returns a snapshot of all launchable apps, sorted alphabetically.
     */
    /**
     * Returns a snapshot of all launchable apps, sorted alphabetically.
     * This method correctly switches to [Dispatchers.IO] for potentially heavy
     * PackageManager operations.
     */
    suspend fun getInstalledApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos: List<ResolveInfo> = context.packageManager.queryIntentActivities(
            intent, PackageManager.MATCH_ALL
        )

        resolveInfos
            .filter { it.activityInfo.packageName != context.packageName } // Exclude self
            .map { resolveInfo ->
                AppInfo(
                    label = resolveInfo.loadLabel(context.packageManager).toString(),
                    packageName = resolveInfo.activityInfo.packageName,
                    activityName = resolveInfo.activityInfo.name
                )
            }
            .sortedBy { it.label.lowercase() }
            .distinctBy { it.packageName }
    }

    /**
     * Observes package events and emits the updated app list, plus — when the trigger was a
     * genuine uninstall — which package was uninstalled.
     *
     * The removed package is taken from the ACTION_PACKAGE_REMOVED broadcast itself, not
     * inferred by diffing app lists. The difference matters: an app also *disappears from the
     * query* when it is disabled in system settings, mid-update, or on SD/adoptable storage
     * that unmounts — and all of those are temporary. Only the broadcast distinguishes "gone
     * for now" from "uninstalled", so only the broadcast may authorise deleting the user's
     * favourites/dock/folder placements for that package. EXTRA_REPLACING filters out the
     * REMOVED half of an app update.
     */
    fun observeApps(): Flow<AppListUpdate> = callbackFlow {
        // Emit initial list
        send(AppListUpdate(getInstalledApps()))

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val uninstalled = if (
                    intent?.action == Intent.ACTION_PACKAGE_REMOVED &&
                    !intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
                ) intent.data?.schemeSpecificPart else null
                launch {
                    send(AppListUpdate(getInstalledApps(), uninstalled))
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            // Fired when an app (or its launcher activity) is enabled or disabled — e.g. the user
            // disables a preinstalled app in system settings, or an app toggles its own alias
            // components. Without it, a disabled app stayed in the drawer for the life of the
            // process (its row tapped into nothing) and a re-enabled one never appeared. For a
            // HOME app "the life of the process" is weeks, and there is no other re-query path.
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        // These arrive without a package: data scheme, so they need their own filter:
        // apps on shared/adoptable storage appearing and disappearing as media mounts.
        val storageFilter = IntentFilter().apply {
            addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE)
            addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
            context.registerReceiver(receiver, storageFilter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
            context.registerReceiver(receiver, storageFilter)
        }

        awaitClose {
            context.unregisterReceiver(receiver)
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Creates a launch intent for the specified app.
     *
     * @param packageName The package name of the app to launch
     * @return Launch intent, or null if the app is not found
     */
    fun getLaunchIntent(packageName: String): Intent? {
        return context.packageManager.getLaunchIntentForPackage(packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}

/**
 * One emission of the observed app list.
 *
 * @property apps every launchable app currently visible to the launcher.
 * @property removedPackage set only when this emission was triggered by a genuine uninstall
 *   (ACTION_PACKAGE_REMOVED without EXTRA_REPLACING). Consumers may delete stored placements
 *   for this package and no other — packages merely absent from [apps] can be disabled,
 *   mid-update, on unmounted storage, or part of a restored backup, and must be kept.
 */
data class AppListUpdate(
    val apps: List<AppInfo>,
    val removedPackage: String? = null
)
