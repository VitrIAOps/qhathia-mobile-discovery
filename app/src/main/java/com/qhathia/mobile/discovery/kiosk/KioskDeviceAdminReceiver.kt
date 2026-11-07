package com.qhathia.mobile.discovery.kiosk

import android.app.admin.DeviceAdminReceiver

/**
 * Device admin component, so operation can make the app the device owner (F-10):
 * `adb shell dpm set-device-owner com.qhathia.mobile.discovery/.kiosk.KioskDeviceAdminReceiver`.
 * It uses no admin policies; being device owner is what lets the app pin without the system dialog.
 */
class KioskDeviceAdminReceiver : DeviceAdminReceiver()
