package com.qhathia.mobile.discovery

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Process

/**
 * Trampoline for the remote `RESTART_APP` (design D7). It runs in its own `:restart` process and is
 * started by [MainActivity] into the same task, so the pinned task always keeps a live activity and
 * screen pinning holds (no "Pin app?" dialog). It kills the main process, starts [MainActivity] again
 * in this task (a new main process), and then ends its own process.
 *
 * It must be opaque: on API 26 a translucent activity cannot fix its orientation and crashes (task
 * 1.1). It draws nothing; the app theme's background shows for a moment.
 */
class RestartActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mainPid = intent.getIntExtra(EXTRA_MAIN_PID, NO_PID)
        if (mainPid != NO_PID && mainPid != Process.myPid()) Process.killProcess(mainPid)
        startActivity(
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
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

        /** Intent to restart the app from [activity] (no `NEW_TASK`: it must join the current task). */
        fun intent(activity: Context, mainPid: Int): Intent =
            Intent(activity, RestartActivity::class.java).putExtra(EXTRA_MAIN_PID, mainPid)
    }
}
