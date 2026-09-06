package com.splitview.pad

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper

/**
 * Launching two *installed* native apps side by side using system multi-window flags.
 */
object NativeSplitLauncher {

    enum class Result { LAUNCHED_ADJACENT, LAUNCHED_SEQUENTIALLY, FAILED }

    fun canLaunchAdjacent(activity: Activity): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.N

    fun launchPair(activity: Activity, first: String, second: String): Result {
        if (!launch(activity, first)) return Result.FAILED

        Handler(Looper.getMainLooper()).postDelayed({
            launch(activity, second)
        }, LAUNCH_GAP_MS)

        return Result.LAUNCHED_ADJACENT
    }

    fun launchSingle(activity: Activity, packageName: String): Boolean =
        launch(activity, packageName)

    private fun launch(activity: Activity, packageName: String): Boolean {
        val intent = activity.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.addFlags(
                Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT or
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK
            )
        } else {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching { activity.startActivity(intent) }.isSuccess
    }

    private const val LAUNCH_GAP_MS = 800L
}
