package com.splitview.pad

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper

/**
 * Launching two *installed* apps side by side.
 *
 * Android only honours [Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT] when the caller is
 * already in split-screen mode — there is no public API to force a device out of
 * full screen into a split. So rather than pretending, we do the part that works
 * and say plainly what the user has to do for the rest.
 */
object NativeSplitLauncher {

    enum class Result { LAUNCHED_ADJACENT, LAUNCHED_SEQUENTIALLY, FAILED }

    fun canLaunchAdjacent(activity: Activity): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && activity.isInMultiWindowMode

    fun launchPair(activity: Activity, first: String, second: String): Result {
        if (!launch(activity, first, adjacent = false)) return Result.FAILED

        val adjacent = canLaunchAdjacent(activity)
        Handler(Looper.getMainLooper()).postDelayed({
            launch(activity, second, adjacent = adjacent)
        }, LAUNCH_GAP_MS)

        return if (adjacent) Result.LAUNCHED_ADJACENT else Result.LAUNCHED_SEQUENTIALLY
    }

    fun launchSingle(activity: Activity, packageName: String): Boolean =
        launch(activity, packageName, adjacent = canLaunchAdjacent(activity))

    private fun launch(activity: Activity, packageName: String, adjacent: Boolean): Boolean {
        val intent = activity.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (adjacent) {
            intent.addFlags(
                Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK
            )
        }
        return runCatching { activity.startActivity(intent) }.isSuccess
    }

    private const val LAUNCH_GAP_MS = 600L
}
