package com.qhathia.mobile.discovery.feature.settings

import app.cash.turbine.test
import com.qhathia.mobile.discovery.core.domain.model.DeviceSettings
import com.qhathia.mobile.discovery.core.domain.model.StoreSession
import com.qhathia.mobile.discovery.core.domain.usecase.ExitKioskModeUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ObserveKioskLockUseCase
import com.qhathia.mobile.discovery.core.testing.FakeDeviceSettingsRepository
import com.qhathia.mobile.discovery.core.testing.FakeKioskSessionRepository
import com.qhathia.mobile.discovery.core.testing.FakeStoreSessionRepository
import com.qhathia.mobile.discovery.core.testing.MainDispatcherExtension
import com.qhathia.mobile.discovery.feature.settings.model.KioskExitUiEvent
import com.qhathia.mobile.discovery.feature.settings.model.KioskExitUiState
import com.qhathia.mobile.discovery.feature.settings.ui.KioskExitViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class KioskExitViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val settings = FakeDeviceSettingsRepository()
    private val kiosk = FakeKioskSessionRepository()

    private fun viewModel() = KioskExitViewModel(
        ObserveKioskLockUseCase(FakeStoreSessionRepository(StoreSession(deviceAccessToken = "jwt")), settings, kiosk),
        ExitKioskModeUseCase(kiosk),
    )

    @Test
    fun exit_item_shown_while_locked() = runTest(mainDispatcher.dispatcher) {
        viewModel().uiState.test {
            assertEquals(KioskExitUiState(isLockActive = true), expectMostRecentItem())
        }
    }

    @Test
    fun exit_item_hidden_when_kiosk_mode_off() = runTest(mainDispatcher.dispatcher) {
        settings.store(DeviceSettings.DEFAULT.copy(kioskMode = false))
        viewModel().uiState.test {
            assertEquals(KioskExitUiState(isLockActive = false), expectMostRecentItem())
        }
    }

    @Test
    fun exit_releases_the_lock_and_shows_the_message() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel()
        vm.uiState.test {
            assertEquals(KioskExitUiState(isLockActive = true), expectMostRecentItem())
            vm.onEvent(KioskExitUiEvent.ExitKioskMode)
            assertEquals(KioskExitUiState(isLockActive = false, hasExited = true), expectMostRecentItem())
        }
        assertTrue(kiosk.exitedFromSettings.value)
    }
}
