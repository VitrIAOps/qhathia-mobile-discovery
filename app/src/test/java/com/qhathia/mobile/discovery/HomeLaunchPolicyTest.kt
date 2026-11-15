package com.qhathia.mobile.discovery

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class HomeLaunchPolicyTest {

    @Test
    fun launcher_or_explicit_intent_always_proceeds() {
        // Launcher icon, RESTART_APP trampoline (F-09): never dropped, whatever the task looks like.
        assertEquals(HomeLaunchAction.Proceed, homeLaunchAction(isHomeIntent = false, isTaskRoot = false, otherShellTaskExists = true))
        assertEquals(HomeLaunchAction.Proceed, homeLaunchAction(isHomeIntent = false, isTaskRoot = true, otherShellTaskExists = true))
    }

    @Test
    fun home_intent_stacked_on_the_open_showcase_finishes() {
        assertEquals(HomeLaunchAction.FinishOnTop, homeLaunchAction(isHomeIntent = true, isTaskRoot = false, otherShellTaskExists = false))
        assertEquals(HomeLaunchAction.FinishOnTop, homeLaunchAction(isHomeIntent = true, isTaskRoot = false, otherShellTaskExists = true))
    }

    @Test
    fun home_intent_in_its_own_task_brings_the_open_showcase_back() {
        assertEquals(
            HomeLaunchAction.BringExistingToFront,
            homeLaunchAction(isHomeIntent = true, isTaskRoot = true, otherShellTaskExists = true),
        )
    }

    @Test
    fun home_intent_with_no_showcase_open_proceeds() {
        // After a reboot, Discovery as the home screen (kiosk-boot-start).
        assertEquals(HomeLaunchAction.Proceed, homeLaunchAction(isHomeIntent = true, isTaskRoot = true, otherShellTaskExists = false))
    }
}
