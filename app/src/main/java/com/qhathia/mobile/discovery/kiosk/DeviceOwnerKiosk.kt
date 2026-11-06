package com.qhathia.mobile.discovery.kiosk

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import com.qhathia.mobile.discovery.MainActivity

/**
 * Kiosk setup that only a device owner can do (F-10). Without device owner it does nothing and the
 * app keeps screen pinning with the "Pin app?" dialog.
 *
 * - Allows the app in lock task mode, so `startLockTask()` pins at once, without the dialog, and
 *   `stopLockTask()` still serves "Salir del modo kiosko".
 * - Hides the Home, Recents and notification features while pinned (API 28+).
 * - Makes `MainActivity` the home screen (F-12), so nobody has to pick "Siempre" by hand.
 */
internal object DeviceOwnerKiosk {

    fun configure(activity: Activity) {
        val dpm = activity.getSystemService(DevicePolicyManager::class.java) ?: return
        if (!dpm.isDeviceOwnerApp(activity.packageName)) return
        val admin = ComponentName(activity, KioskDeviceAdminReceiver::class.java)
        try {
            dpm.setLockTaskPackages(admin, arrayOf(activity.packageName))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                dpm.setLockTaskFeatures(admin, DevicePolicyManager.LOCK_TASK_FEATURE_NONE)
            }
            val home = IntentFilter(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                addCategory(Intent.CATEGORY_DEFAULT)
            }
            dpm.addPersistentPreferredActivity(admin, home, ComponentName(activity, MainActivity::class.java))
        } catch (e: SecurityException) {
            Log.w(TAG, "Device owner kiosk setup rejected", e)
        }
    }

    private const val TAG = "DeviceOwnerKiosk"
}
