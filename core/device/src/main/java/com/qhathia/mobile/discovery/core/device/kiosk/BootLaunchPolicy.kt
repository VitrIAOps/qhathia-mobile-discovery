package com.qhathia.mobile.discovery.core.device.kiosk

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

/** When a boot receiver should open the app. */
object BootLaunchPolicy {

    /**
     * Opens only when the host allows it ([canOpen]) and the app is not the default home screen: as
     * home, Android already opens it after the boot, and opening it again would start a second screen.
     */
    fun shouldOpen(canOpen: Boolean, isDefaultHome: Boolean): Boolean = canOpen && !isDefaultHome
}

/** Whether this app is the device's default home screen. */
fun Context.isDefaultHomeApp(): Boolean {
    val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
    val resolved = packageManager.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)
    return resolved?.activityInfo?.packageName == packageName
}
