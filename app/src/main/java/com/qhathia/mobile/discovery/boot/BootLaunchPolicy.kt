package com.qhathia.mobile.discovery.boot

/** When the boot receiver opens the showcase (boot-autostart D2). */
internal object BootLaunchPolicy {

    /**
     * Opens only a paired tablet, and only when Discovery is not the default home screen: as home,
     * Android already opens it after the boot, and opening it again would start a second screen.
     */
    fun shouldOpen(isPaired: Boolean, isDefaultHome: Boolean): Boolean = isPaired && !isDefaultHome
}
