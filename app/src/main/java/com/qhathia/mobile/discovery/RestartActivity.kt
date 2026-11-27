package com.qhathia.mobile.discovery

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.qhathia.mobile.discovery.core.device.kiosk.ProcessRestartActivity

/** RESTART_APP trampoline (design D7), declared in the manifest with its own `:restart` process. */
class RestartActivity : ProcessRestartActivity() {

    override val relaunchTarget: Class<out Activity> = MainActivity::class.java

    companion object {
        fun intent(activity: Context, mainPid: Int): Intent =
            ProcessRestartActivity.intent(activity, RestartActivity::class.java, mainPid)
    }
}
