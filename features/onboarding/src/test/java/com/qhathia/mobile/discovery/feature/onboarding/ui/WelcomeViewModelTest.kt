package com.qhathia.mobile.discovery.feature.onboarding.ui

import app.cash.turbine.test
import com.qhathia.mobile.discovery.core.domain.model.StoreSession
import com.qhathia.mobile.discovery.core.domain.usecase.DismissUnlinkedNoticeUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ObserveStoreSessionUseCase
import com.qhathia.mobile.discovery.core.testing.FakeStoreSessionRepository
import com.qhathia.mobile.discovery.core.testing.MainDispatcherExtension
import com.qhathia.mobile.discovery.feature.onboarding.model.WelcomeUiEffect
import com.qhathia.mobile.discovery.feature.onboarding.model.WelcomeUiEvent
import com.qhathia.mobile.discovery.feature.onboarding.model.WelcomeUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class WelcomeViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension(UnconfinedTestDispatcher())

    private fun viewModel(session: FakeStoreSessionRepository) = WelcomeViewModel(
        observeStoreSession = ObserveStoreSessionUseCase(session),
        dismissUnlinkedNotice = DismissUnlinkedNoticeUseCase(session),
    )

    @Test
    fun pending_notice_is_shown() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel(FakeStoreSessionRepository(StoreSession(unlinkedNotice = true)))

        vm.uiState.test {
            assertEquals(WelcomeUiState(showUnlinkedNotice = true), expectMostRecentItem())
        }
    }

    @Test
    fun no_notice_on_a_normal_first_run() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel(FakeStoreSessionRepository())

        vm.uiState.test {
            assertEquals(WelcomeUiState(showUnlinkedNotice = false), expectMostRecentItem())
        }
    }

    @Test
    fun continue_dismisses_the_notice_then_navigates() = runTest(mainDispatcher.dispatcher) {
        val session = FakeStoreSessionRepository(StoreSession(unlinkedNotice = true))
        val vm = viewModel(session)

        vm.uiEffect.test {
            vm.onEvent(WelcomeUiEvent.Continue)

            assertEquals(WelcomeUiEffect.NavigateToInitialConfiguration, awaitItem())
            // Dismissed before the effect was emitted.
            assertFalse(session.current.unlinkedNotice)
        }
    }
}
