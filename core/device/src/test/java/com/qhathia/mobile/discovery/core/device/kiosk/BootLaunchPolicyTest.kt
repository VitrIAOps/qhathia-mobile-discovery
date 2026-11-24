package com.qhathia.mobile.discovery.core.device.kiosk

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BootLaunchPolicyTest {

    @Test
    fun paired_tablet_not_home_is_opened() {
        assertTrue(BootLaunchPolicy.shouldOpen(canOpen = true, isDefaultHome = false))
    }

    @Test
    fun discovery_as_home_is_not_opened_twice() {
        assertFalse(BootLaunchPolicy.shouldOpen(canOpen = true, isDefaultHome = true))
    }

    @Test
    fun unpaired_tablet_is_not_opened() {
        assertFalse(BootLaunchPolicy.shouldOpen(canOpen = false, isDefaultHome = false))
        assertFalse(BootLaunchPolicy.shouldOpen(canOpen = false, isDefaultHome = true))
    }
}
