package com.splitview.pad

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper

/**
 * Launching two *installed* native apps side by side using system multi-window flags.
 *
 * App 1 is launched as a new task taking the screen (adjacent = false),
 * then App 2 is launched ADJACENT to App 1 (adjacent = true) after a short delay
 * so Android's task manager splits the screen between App 1 and App 2.
 */
object NativeSplitLauncher {

    enum class Result { LAUNCHED_ADJACENT, LAUNCHED_SEQUENTIALLY, FAILED }

    fun canLaunchAdjacent(activity: Activity): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false

        val manufacturer = Build.MANUFACTURER
        val brand = Build.BRAND
        val unsupportedOems = listOf("xiaomi", "poco", "redmi", "blackshark")

        val isUnsupported = unsupportedOems.any {
            manufacturer.contains(it, ignoreCase = true) || brand.contains(it, ignoreCase = true)
        }

        return !isUnsupported
    }

    fun launchPair(activity: Activity, first: String, second: String): Result {
        // 1. Launch App 1 as primary task
        if (!launch(activity, first, adjacent = false)) return Result.FAILED

        // 2. Launch App 2 adjacent to App 1 after short delay
        Handler(Looper.getMainLooper()).postDelayed({
            launch(activity, second, adjacent = true)
        }, LAUNCH_GAP_MS)

        return Result.LAUNCHED_ADJACENT
    }

    fun launchSingle(activity: Activity, packageName: String): Boolean =
        launch(activity, packageName, adjacent = false)

    private fun launch(activity: Activity, packageName: String, adjacent: Boolean): Boolean {
        val intent = activity.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        if (adjacent && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT)
        }

        return runCatching {
            activity.startActivity(intent)
        }.isSuccess
    }

    private const val LAUNCH_GAP_MS = 600L
}
