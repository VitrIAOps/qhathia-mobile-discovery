package com.qhathia.mobile.discovery.core.device.kiosk

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Process

/**
 * Restart trampoline. The host declares a subclass in its own process and starts it from the main
 * activity into the same task, so the pinned task always keeps a live activity and screen pinning
 * holds (no "Pin app?" dialog). It kills the main process, starts [relaunchTarget] again in this task
 * (a new main process), and then ends its own process.
 *
 * It must be opaque: on API 26 a translucent activity cannot fix its orientation and crashes. It draws nothing; the app theme's background shows for a moment.
 */
abstract class ProcessRestartActivity : Activity() {

    /** The activity to start again once the main process is gone. */
    protected abstract val relaunchTarget: Class<out Activity>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mainPid = intent.getIntExtra(EXTRA_MAIN_PID, NO_PID)
        if (mainPid != NO_PID && mainPid != Process.myPid()) Process.killProcess(mainPid)
        startActivity(
            Intent(this, relaunchTarget).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
        )
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        Process.killProcess(Process.myPid())
    }

    companion object {
        private const val EXTRA_MAIN_PID = "main_pid"
        private const val NO_PID = -1

        /** The intent that restarts through [restartActivity], from the process [mainPid]. */
        fun intent(context: Context, restartActivity: Class<out ProcessRestartActivity>, mainPid: Int): Intent =
            Intent(context, restartActivity).putExtra(EXTRA_MAIN_PID, mainPid)
    }
}
