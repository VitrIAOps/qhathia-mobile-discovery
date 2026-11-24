package com.qhathia.mobile.discovery.core.device.kiosk

import android.app.Activity
import android.app.ActivityManager
import android.content.Intent
import android.os.Build

/** Keeps a HOME intent from leaving a second copy of an activity that is already open. */
object HomeLaunchGuard {

    /**
     * Applies [homeLaunchAction] to the intent that created [activity]. Returns `true` when it is
     * finishing because another task of the same activity is already open.
     */
    fun yieldToOpenTask(activity: Activity): Boolean {
        val intent = activity.intent
        val isHomeIntent = intent?.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)
        if (!isHomeIntent) return false
        val otherTask = activity.getSystemService(ActivityManager::class.java).appTasks.firstOrNull { task ->
            val info = runCatching { task.taskInfo }.getOrNull() ?: return@firstOrNull false
            val id = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) info.taskId else @Suppress("DEPRECATION") info.id
            id != activity.taskId && info.baseActivity?.className == activity.javaClass.name
        }
        return when (homeLaunchAction(isHomeIntent, activity.isTaskRoot, otherShellTaskExists = otherTask != null)) {
            HomeLaunchAction.Proceed -> false
            HomeLaunchAction.FinishOnTop -> {
                activity.finish()
                true
            }
            HomeLaunchAction.BringExistingToFront -> {
                otherTask?.let { runCatching { it.moveToFront() } }
                activity.finishAndRemoveTask()
                true
            }
        }
    }
}
