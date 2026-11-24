package com.qhathia.mobile.discovery.core.device.kiosk

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log

/**
 * Kiosk setup that only a device owner can do. Without device owner it does nothing and the
 * app keeps screen pinning with the "Pin app?" dialog.
 *
 * - Allows the app in lock task mode, so `startLockTask()` pins at once, without the dialog, and
 *   `stopLockTask()` still serves "Salir del modo kiosko".
 * - Hides the Home, Recents and notification features while pinned (API 28+).
 * - Makes the given activity the home screen, so nobody has to pick "Siempre" by hand.
 */
object DeviceOwnerPolicy {

    fun configure(activity: Activity, admin: ComponentName, homeActivity: ComponentName) {
        val dpm = activity.getSystemService(DevicePolicyManager::class.java) ?: return
        if (!dpm.isDeviceOwnerApp(activity.packageName)) return
        try {
            dpm.setLockTaskPackages(admin, arrayOf(activity.packageName))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                dpm.setLockTaskFeatures(admin, DevicePolicyManager.LOCK_TASK_FEATURE_NONE)
            }
            val home = IntentFilter(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                addCategory(Intent.CATEGORY_DEFAULT)
            }
            dpm.addPersistentPreferredActivity(admin, home, homeActivity)
        } catch (e: SecurityException) {
            Log.w(TAG, "Device owner kiosk setup rejected", e)
        }
    }

    private const val TAG = "DeviceOwnerKiosk"
}
