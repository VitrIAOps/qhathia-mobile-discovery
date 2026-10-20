package com.qhathia.mobile.discovery.core.domain.usecase

import app.cash.turbine.test
import com.qhathia.mobile.discovery.core.domain.model.DeviceSettings
import com.qhathia.mobile.discovery.core.domain.model.KioskLockState
import com.qhathia.mobile.discovery.core.domain.model.StoreSession
import com.qhathia.mobile.discovery.core.testing.FakeDeviceSettingsRepository
import com.qhathia.mobile.discovery.core.testing.FakeKioskSessionRepository
import com.qhathia.mobile.discovery.core.testing.FakeStoreSessionRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class KioskLockUseCasesTest {

    private val paired = StoreSession(deviceAccessToken = "jwt")
    private val sessions = FakeStoreSessionRepository(paired)
    private val settings = FakeDeviceSettingsRepository()
    private val kiosk = FakeKioskSessionRepository()

    private val observe = ObserveKioskLockUseCase(sessions, settings, kiosk)
    private val exit = ExitKioskModeUseCase(kiosk)
    private val resume = ResumeKioskModeUseCase(kiosk)

    private val active = KioskLockState(active = true)
    private val inactive = KioskLockState.INACTIVE

    @Test
    fun unpaired_tablet_is_never_locked() = runTest {
        val observeUnpaired = ObserveKioskLockUseCase(FakeStoreSessionRepository(), settings, kiosk)
        observeUnpaired().test { assertEquals(inactive, awaitItem()) }
    }

    @Test
    fun paired_with_default_settings_is_locked() = runTest {
        observe().test { assertEquals(active, awaitItem()) }
    }

    @Test
    fun kiosk_mode_off_unlocks_and_on_locks_again() = runTest {
        observe().test {
            assertEquals(active, awaitItem())
            settings.store(DeviceSettings.DEFAULT.copy(kioskMode = false))
            assertEquals(inactive, awaitItem())
            settings.store(DeviceSettings.DEFAULT.copy(kioskMode = true, version = 2))
            assertEquals(active, awaitItem())
        }
    }

    @Test
    fun exit_unlocks_and_resume_locks_again() = runTest {
        observe().test {
            assertEquals(active, awaitItem())
            exit()
            assertEquals(inactive, awaitItem())
            resume()
            assertEquals(active, awaitItem())
        }
    }

    @Test
    fun unrelated_settings_change_does_not_emit() = runTest {
        observe().test {
            assertEquals(active, awaitItem())
            settings.store(DeviceSettings.DEFAULT.copy(voiceAssistantEnabled = false))
            expectNoEvents()
        }
    }
}
