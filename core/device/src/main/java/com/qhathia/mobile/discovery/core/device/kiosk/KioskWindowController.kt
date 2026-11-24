package com.qhathia.mobile.discovery.core.device.kiosk

import android.app.Activity
import android.app.ActivityManager
import android.os.SystemClock
import android.util.Log
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Keeps [activity] in kiosk: screen pinning (lock task), hidden system bars and screen on. It only
 * carries the lock out; whether the kiosk should be active is the host app's decision.
 *
 * @param pinningMinIntervalMillis least time between throttled pinning requests, so a refused
 *   "Pin app?" is not shown in a loop.
 */
class KioskWindowController(
    private val activity: Activity,
    pinningMinIntervalMillis: Long,
) {
    private var immersive = false

    /** Stops re-hiding the bars after the keyboard (Android 10 and older); set while locked. */
    private var stopKeepingBarsHidden: (() -> Unit)? = null

    private val pinningThrottle = PinningRequestThrottle(pinningMinIntervalMillis)

    /** The keyboard closing or a system dialog (e.g. "Pin app?") going away shows the bars: hide them. */
    fun onWindowFocusChanged(hasFocus: Boolean) {
        if (hasFocus && immersive) activity.window.hideSystemBars()
    }

    /**
     * Applies the kiosk lock: screen pinning without Device Owner, hidden system bars (a swipe shows
     * them transiently) and screen kept on. Pinning is always asked here (resume).
     */
    fun applyLock(active: Boolean) {
        immersive = active
        val window = activity.window
        if (active) {
            requestPinning(throttled = false)
            window.hideSystemBars()
            if (stopKeepingBarsHidden == null) stopKeepingBarsHidden = window.keepSystemBarsHidden()
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            if (isPinned()) runCatching { activity.stopLockTask() }
            stopKeepingBarsHidden?.invoke()
            stopKeepingBarsHidden = null
            WindowCompat.getInsetsController(window, window.decorView)
                .show(WindowInsetsCompat.Type.systemBars())
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    /**
     * Asks for screen pinning when the task is not pinned yet. [throttled] requests are dropped when
     * one was made less than the minimum interval ago.
     */
    fun requestPinning(throttled: Boolean) {
        if (isPinned()) return
        val now = SystemClock.elapsedRealtime()
        if (throttled) {
            if (!pinningThrottle.tryAcquire(now)) return
        } else {
            pinningThrottle.record(now)
        }
        try {
            activity.startLockTask()
        } catch (e: Exception) {
            // API 26 rejects it while the task is not in the foreground; keep it visible in logcat.
            Log.w(TAG, "startLockTask rejected", e)
        }
    }

    private fun isPinned(): Boolean =
        activity.getSystemService(ActivityManager::class.java).lockTaskModeState !=
            ActivityManager.LOCK_TASK_MODE_NONE

    private companion object {
        const val TAG = "KioskWindowController"
    }
}
