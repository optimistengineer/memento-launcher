package com.optimistswe.mementolauncher

import android.app.Activity
import android.content.pm.ActivityInfo

/**
 * Locks the activity to portrait on handsets, leaving large screens free to rotate.
 *
 * The life calendar is the app's headline page: a 52-column grid with one row per year of life
 * (50..120 of them) drawn as square cells, so its natural aspect is tall. On a rotated phone the
 * page has roughly 218dp of height to spend on 80 rows, which it can only satisfy by shrinking
 * each week to a few pixels — measured on a Pixel 8 Pro, the grid rendered as a ~151px strip in
 * the middle of a 2244px-wide screen. Nothing was clipped or missing, it was simply not worth
 * looking at, and a minimalist home screen has no landscape use case that justifies shipping it.
 *
 * Large screens are excluded: a tablet or unfolded foldable in landscape still has the height to
 * draw the grid properly, its content is already capped and centred, and Play's large-screen
 * guidance is explicitly against apps that lock orientation there.
 *
 * This lives in code rather than `android:screenOrientation` in the manifest because that
 * attribute takes a literal enum value and cannot reference a resource, so it cannot vary by
 * `sw600dp` — which is the entire distinction being drawn here.
 */
fun Activity.applyOrientationLock() {
    requestedOrientation = if (resources.getBoolean(R.bool.lock_portrait)) {
        // USER_PORTRAIT rather than PORTRAIT: still portrait-only, but honours a reverse-portrait
        // preference for anyone who holds their phone the other way up.
        ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
    } else {
        ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }
}
