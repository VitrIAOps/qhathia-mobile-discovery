package com.qhathia.mobile.discovery

import androidx.lifecycle.Lifecycle
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NavigationGuardTest {

    @Test
    fun resumed_screen_allows_navigation() {
        assertTrue(Lifecycle.State.RESUMED.allowsNavigation())
    }

    @Test
    fun screen_in_transition_drops_navigation() {
        assertFalse(Lifecycle.State.STARTED.allowsNavigation())
        assertFalse(Lifecycle.State.CREATED.allowsNavigation())
        assertFalse(Lifecycle.State.INITIALIZED.allowsNavigation())
        assertFalse(Lifecycle.State.DESTROYED.allowsNavigation())
    }

    @Test
    fun no_screen_drops_navigation() {
        assertFalse((null as Lifecycle.State?).allowsNavigation())
    }
}
