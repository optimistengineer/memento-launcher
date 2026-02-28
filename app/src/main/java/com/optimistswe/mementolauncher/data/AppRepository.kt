package com.optimistswe.mementolauncher.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
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
     * Observes app install/uninstall events and emits the updated app list.
     *
     * The Flow re-queries PackageManager whenever an app is added, removed,
     * or replaced on the device.
     */
    fun observeApps(): Flow<List<AppInfo>> = callbackFlow {
        // Emit initial list
        send(getInstalledApps())

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                launch {
                    send(getInstalledApps())
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }

        context.registerReceiver(receiver, filter)

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
