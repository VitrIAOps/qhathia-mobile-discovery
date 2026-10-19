package com.qhathia.mobile.discovery.core.data.heartbeat

import com.qhathia.mobile.discovery.core.data.network.ConnectivitySnapshot
import com.qhathia.mobile.discovery.core.domain.model.DeviceNetworkStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class DeviceTelemetryFormattingTest {

    private fun snapshot(wifiActive: Boolean = false, mobile: Boolean = false, ssid: String? = null) =
        ConnectivitySnapshot(
            wifiEnabled = true,
            mobileDataActive = mobile,
            airplaneModeActive = false,
            connectedSsid = ssid,
            wifiActive = wifiActive,
        )

    @Test
    fun model_joins_manufacturer_and_model() {
        assertEquals("Lenovo TB330FU", formatDeviceModel("lenovo", "TB330FU"))
    }

    @Test
    fun model_does_not_repeat_manufacturer() {
        assertEquals("Samsung SM-X200", formatDeviceModel("samsung", "Samsung SM-X200"))
    }

    @Test
    fun model_is_truncated_to_100_chars() {
        assertEquals(100, formatDeviceModel("Acme", "X".repeat(200))!!.length)
    }

    @Test
    fun blank_model_is_null() {
        assertNull(formatDeviceModel(" ", ""))
    }

    @Test
    fun battery_outside_range_is_dropped() {
        assertEquals(85, sanitizeBatteryLevel(85))
        assertEquals(0, sanitizeBatteryLevel(0))
        assertNull(sanitizeBatteryLevel(Int.MIN_VALUE))
        assertNull(sanitizeBatteryLevel(101))
        assertNull(sanitizeBatteryLevel(null))
    }

    @Test
    fun network_status_prefers_wifi_then_mobile() {
        assertEquals(DeviceNetworkStatus.WIFI, snapshot(wifiActive = true, mobile = true).toNetworkStatus())
        assertEquals(DeviceNetworkStatus.WIFI, snapshot(ssid = "Tienda").toNetworkStatus())
        assertEquals(DeviceNetworkStatus.MOBILE, snapshot(mobile = true).toNetworkStatus())
        assertEquals(DeviceNetworkStatus.OFFLINE, snapshot().toNetworkStatus())
    }
}
