package com.qhathia.mobile.discovery.core.data.kiosk

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class KioskSessionRepositoryImplTest {

    @Test
    fun starts_not_exited() {
        assertFalse(KioskSessionRepositoryImpl().exitedFromSettings.value)
    }

    @Test
    fun exit_and_resume_round_trip() {
        val repository = KioskSessionRepositoryImpl()
        repository.setExitedFromSettings(true)
        assertTrue(repository.exitedFromSettings.value)
        repository.setExitedFromSettings(false)
        assertFalse(repository.exitedFromSettings.value)
    }

    @Test
    fun a_new_process_starts_locked_again() {
        KioskSessionRepositoryImpl().setExitedFromSettings(true)
        assertFalse(KioskSessionRepositoryImpl().exitedFromSettings.value)
    }
}
