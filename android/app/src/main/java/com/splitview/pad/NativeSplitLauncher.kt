package com.splitview.pad

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper

/**
 * Launching two *installed* native apps side by side using system multi-window flags.
 *
 * App 1 is launched into the primary split pane (adjacent = false),
 * then App 2 is launched into the secondary adjacent split pane (adjacent = true)
 * after a 1000ms delay with explicit launch bounds and windowing options.
 */
object NativeSplitLauncher {

    enum class Result { LAUNCHED_ADJACENT, LAUNCHED_SEQUENTIALLY, FAILED }

    fun canLaunchAdjacent(activity: Activity): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.N

    fun launchPair(activity: Activity, first: String, second: String): Result {
        // 1. Launch App 1 as primary task
        if (!launch(activity, first, adjacent = false)) return Result.FAILED

        // 2. Launch App 2 adjacent to App 1 after delay
        Handler(Looper.getMainLooper()).postDelayed({
            launch(activity, second, adjacent = true)
        }, LAUNCH_GAP_MS)

        return Result.LAUNCHED_ADJACENT
    }

    fun launchSingle(activity: Activity, packageName: String): Boolean =
        launch(activity, packageName, adjacent = false)

    private fun launch(activity: Activity, packageName: String, adjacent: Boolean): Boolean {
        val intent = activity.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        
        intent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_MULTIPLE_TASK or
            Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        )

        if (adjacent && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT)
        }

        var optionsBundle: android.os.Bundle? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                val options = ActivityOptions.makeBasic()
                val metrics = activity.resources.displayMetrics
                val w = metrics.widthPixels
                val h = metrics.heightPixels
                val isLandscape = w > h

                val bounds = if (!adjacent) {
                    if (isLandscape) Rect(0, 0, w / 2, h) else Rect(0, 0, w, h / 2)
                } else {
                    if (isLandscape) Rect(w / 2, 0, w, h) else Rect(0, h / 2, w, h)
                }
                options.setLaunchBounds(bounds)

                try {
                    val method = ActivityOptions::class.java.getMethod("setLaunchWindowingMode", Int::class.java)
                    val mode = if (!adjacent) 3 else 4
                    method.invoke(options, mode)
                } catch (_: Exception) {
                    // Method not available on this Android build; setLaunchBounds + FLAG_ACTIVITY_LAUNCH_ADJACENT still apply
                }

                optionsBundle = options.toBundle()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return runCatching {
            if (optionsBundle != null) {
                activity.startActivity(intent, optionsBundle)
            } else {
                activity.startActivity(intent)
            }
        }.isSuccess
    }

    private const val LAUNCH_GAP_MS = 1000L
}
