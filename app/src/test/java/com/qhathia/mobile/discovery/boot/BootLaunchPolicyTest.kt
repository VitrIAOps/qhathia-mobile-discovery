package com.qhathia.mobile.discovery.boot

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BootLaunchPolicyTest {

    @Test
    fun paired_tablet_not_home_is_opened() {
        assertTrue(BootLaunchPolicy.shouldOpen(isPaired = true, isDefaultHome = false))
    }

    @Test
    fun discovery_as_home_is_not_opened_twice() {
        assertFalse(BootLaunchPolicy.shouldOpen(isPaired = true, isDefaultHome = true))
    }

    @Test
    fun unpaired_tablet_is_not_opened() {
        assertFalse(BootLaunchPolicy.shouldOpen(isPaired = false, isDefaultHome = false))
        assertFalse(BootLaunchPolicy.shouldOpen(isPaired = false, isDefaultHome = true))
    }
}
