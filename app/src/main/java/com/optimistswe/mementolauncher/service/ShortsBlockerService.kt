package com.optimistswe.mementolauncher.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.optimistswe.mementolauncher.LauncherActivity
import com.optimistswe.mementolauncher.data.PreferencesRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.annotation.VisibleForTesting

/**
 * An AccessibilityService responsible for detecting when the user is viewing short-form
 * content (YouTube Shorts, Instagram Reels) and redirecting them to the Memento Launcher
 * with a mindful delay interruption.
 *
 * This service uses Hilt to inject the user's preferences and actively listens for changes
 * to the `blockShortFormContent` toggle. If disabled, it does not intercept any content.
 */
@AndroidEntryPoint
class ShortsBlockerService : AccessibilityService() {

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isBlockingEnabled = false
    private var usageNudgeEnabled = false
    private var usageNudgeMs = 15 * 60_000L   // default 15 minutes in ms
    private var distractingPackages: Set<String> = emptySet()

    private var lastActivePackage = ""
    private var packageActiveStartTime = 0L

    /**
     * Called when the service is connected to the system.
     * We initialize our coroutine scope here to observe user preferences.
     */
    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceScope.launch {
            preferencesRepository.getUserPreferences().collect { prefs ->
                isBlockingEnabled = prefs.blockShortFormContent
                usageNudgeEnabled = prefs.usageNudgeEnabled
                usageNudgeMs = prefs.usageNudgeMinutes * 60_000L
                distractingPackages = prefs.distractingPackages
            }
        }
    }

    /**
     * Called when the system destroys the service. Cleans up background jobs.
     */
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    /**
     * Triggered automatically by the system when a targeted accessibility event occurs.
     * Parses the UI hierarchy to detect forbidden short-form content.
     */
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val packageName = event.packageName?.toString() ?: return

        // Track package transitions to determine if this is a fresh app launch/switch
        if (packageName != lastActivePackage && packageName != "com.android.systemui") {
            lastActivePackage = packageName
            packageActiveStartTime = System.currentTimeMillis()
        }

        // Short-circuit before expensive rootInActiveWindow IPC if nothing to do
        val hardcodedBlockingEnabled = false
        if (!hardcodedBlockingEnabled && !usageNudgeEnabled) return
        if (usageNudgeEnabled && packageName !in distractingPackages) return

        val rootNode = rootInActiveWindow ?: return

        val timeSinceForeground = System.currentTimeMillis() - packageActiveStartTime
        // Give a generous 4-second window to consider it an "app launch" where YouTube
        // aggressively defaults to the Shorts tab.
        val isAppLaunch = timeSinceForeground < 4000

        // ── Shorts/Reels blocking ──────────────────────────────────────────────
        // Force-disabled as per user request. Detection logic remains in file for future use.
        if (hardcodedBlockingEnabled) {
            when (packageName) {
                "com.google.android.youtube" -> {
                    if (isYouTubeShortsVisible(rootNode)) {
                        triggerMindfulInterruption(packageName, rootNode, isAppLaunch)
                    }
                }
                "com.instagram.android" -> {
                    if (isInstagramReelsVisible(rootNode)) {
                        triggerMindfulInterruption(packageName, rootNode, isAppLaunch)
                    }
                }
            }
        }

        // ── Usage nudge: fire after X minutes in any distracting app ──────────
        // Runs independently of the Shorts blocker so users can enable just this.
        if (usageNudgeEnabled
            && packageName in distractingPackages
            && timeSinceForeground >= usageNudgeMs) {
            // Reset the timer so the nudge fires every X minutes, not every event.
            packageActiveStartTime = System.currentTimeMillis()
            triggerMindfulInterruption(packageName, rootNode, false)
        }

        rootNode.recycle()
    }

    /**
     * Required override by AccessibilityService.
     */
    override fun onInterrupt() {
        // Required override, but no specific action needed for our use case.
    }

    private var lastInterruptionTime = 0L

    /**
     * Navigates the user away from the short-form content.
     * If the user just launched the app into Shorts, it attempts to click the 'Home' tab silently.
     * If the user actively tapped into Shorts, it triggers the Mindful Delay penalty overlay.
     *
     * @param sourcePackage The package name of the app being interrupted.
     * @param rootNode The root AccessibilityNodeInfo of the active window.
     * @param isAppLaunch True if the interruption is happening within 4 seconds of the app coming to the foreground.
     */
    private fun triggerMindfulInterruption(
        sourcePackage: String, 
        rootNode: AccessibilityNodeInfo, 
        isAppLaunch: Boolean
    ) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastInterruptionTime < 2500) {
            return // Debounce rapid triggers
        }
        lastInterruptionTime = currentTime

        var escapedToHome = false

        // Attempt to tap the "Home" tab to escape Shorts gracefully
        // Instagram and YouTube both use "Home" in the content description of their bottom nav.
        escapedToHome = attemptClickHomeTab(rootNode)

        // If we couldn't find the Home tab to click, fallback to system BACK
        // Avoid sending BACK on a fresh app launch, as that will simply crash/close the app entirely.
        if (!escapedToHome && !isAppLaunch) {
            performGlobalAction(GLOBAL_ACTION_BACK)
        }
        
        // If this was an involuntary drop into Shorts because they just opened the app, 
        // silently redirecting them to Home is enough. Kicking them to Memento is an abrupt crash.
        if (isAppLaunch) {
            return
        }
        
        // They were browsing the feed and actively tapped Shorts. 
        // We throw them to Memento for the 5-second mindful penalty!
        val intent = Intent(this, LauncherActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_MINDFUL_INTERRUPTION", true)
            putExtra("EXTRA_INTERRUPTED_PACKAGE", sourcePackage)
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("ShortsBlockerService", "Failed to launch Memento Launcher", e)
        }
    }

    /**
     * Recursively searches the accessibility node tree to find and click the "Home" navigation tab.
     * This is used as a graceful escape hatch from the Shorts feed.
     *
     * @param node The node to search within.
     * @return True if the Home tab was successfully found and clicked, false otherwise.
     */
    @VisibleForTesting
    internal fun attemptClickHomeTab(node: AccessibilityNodeInfo): Boolean {
        // Look through the node tree for the Home tab button
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                val desc = child.contentDescription?.toString()?.lowercase() ?: ""
                // YouTube's bottom nav Home button usually has "Home" in the description
                if (desc.contains("home") || desc.contains("inicio")) {
                    var clickableNode: AccessibilityNodeInfo? = child
                    while (clickableNode != null) {
                        if (clickableNode.isClickable) {
                            clickableNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                            child.recycle() // Normally shouldn't recycle while iterating parents but it's okay here since we return
                            return true
                        }
                        clickableNode = clickableNode.parent
                    }
                }
                
                val found = attemptClickHomeTab(child)
                child.recycle()
                if (found) return true
            }
        }
        return false
    }

    /**
     * Parses the View hierarchy of the YouTube app to determine if the Shorts player is active.
     * It specifically looks for the "Shorts" bottom navigation tab being in a "Selected" state
     * to avoid false positives on the regular Home feed's "Shorts Shelf".
     *
     * @param node The root node of the YouTube app.
     * @return True if the user is viewing Shorts.
     */
    @VisibleForTesting
    internal fun isYouTubeShortsVisible(node: AccessibilityNodeInfo): Boolean {
        // We look for the "Shorts" tab in the bottom navigation being in the "Selected" state.
        // We avoid looking for any text containing "Shorts" because the YouTube Home Feed
        // has a "Shorts Shelf" which triggers false positives!
        
        var foundShortsPlayer = false
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                val desc = child.contentDescription?.toString()?.lowercase() ?: ""
                
                // If it's a "Shorts" button that is "Selected" (e.g., bottom nav)
                if (desc.contains("shorts") && child.isSelected) {
                    foundShortsPlayer = true
                }
                
                if (!foundShortsPlayer) {
                    foundShortsPlayer = isYouTubeShortsVisible(child)
                }
                child.recycle()
                if (foundShortsPlayer) return true
            }
        }
        return false
    }

    /**
     * Parses the View hierarchy of the Instagram app to determine if the Reels player is active.
     * It relies on finding the bottom navigation tab labeled "Reels" being in a "Selected" state.
     *
     * @param node The root node of the Instagram app.
     * @return True if the user is viewing Reels.
     */
    @VisibleForTesting
    internal fun isInstagramReelsVisible(node: AccessibilityNodeInfo): Boolean {
        var foundReels = false
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                val desc = child.contentDescription?.toString()?.lowercase() ?: ""
                if (desc.contains("reels") && child.isSelected) {
                    foundReels = true
                }
                if (!foundReels) {
                    foundReels = isInstagramReelsVisible(child)
                }
                child.recycle()
                if (foundReels) return true
            }
        }
        return false
    }
}
