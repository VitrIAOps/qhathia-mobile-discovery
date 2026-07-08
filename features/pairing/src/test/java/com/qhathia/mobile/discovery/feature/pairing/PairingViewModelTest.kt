package com.qhathia.mobile.discovery.feature.pairing

import app.cash.turbine.test
import com.qhathia.mobile.discovery.core.domain.model.PairingCode
import com.qhathia.mobile.discovery.core.domain.model.PairingStatus
import com.qhathia.mobile.discovery.core.domain.usecase.ExchangeDeviceTokenUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.GeneratePairingCodeUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.GetPairedDeviceUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ObservePairingStatusUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ObserveStoreSessionUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.UnpairDeviceUseCase
import com.qhathia.mobile.discovery.core.testing.FakeDevicePairingRepository
import com.qhathia.mobile.discovery.core.testing.FakeStoreSessionRepository
import com.qhathia.mobile.discovery.core.testing.MainDispatcherExtension
import com.qhathia.mobile.discovery.feature.pairing.model.PairingPhase
import com.qhathia.mobile.discovery.feature.pairing.model.PairingUiEffect
import com.qhathia.mobile.discovery.feature.pairing.model.PairingUiEvent
import com.qhathia.mobile.discovery.feature.pairing.ui.PairingViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class PairingViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private fun viewModel(
        pairing: FakeDevicePairingRepository,
        session: FakeStoreSessionRepository = FakeStoreSessionRepository(),
    ) = PairingViewModel(
        observeStoreSession = ObserveStoreSessionUseCase(session),
        generatePairingCode = GeneratePairingCodeUseCase(pairing),
        observePairingStatus = ObservePairingStatusUseCase(pairing),
        exchangeDeviceToken = ExchangeDeviceTokenUseCase(pairing),
        getPairedDevice = GetPairedDeviceUseCase(pairing),
        unpairDevice = UnpairDeviceUseCase(pairing),
    )

    @Test
    fun given_not_paired_when_section_entered_then_generates_code_and_waits() =
        runTest(mainDispatcher.dispatcher) {
            val pairing = FakeDevicePairingRepository()
            val vm = viewModel(pairing)

            vm.start()

            val state = vm.uiState.value
            assertEquals(PairingPhase.WaitingForApproval, state.phase)
            assertEquals("2Y567", state.code)
            assertEquals(1, pairing.generateCount)
            vm.stop()
        }

    @Test
    fun given_code_generated_then_total_seconds_anchors_to_granted_ttl() =
        runTest(mainDispatcher.dispatcher) {
            // Server grants 145s (half the documented 5 min); the countdown is seeded from the relative
            // ttlSeconds, not expiresAt − now. A deliberately skewed expiresAt (10 min out) proves the
            // timer ignores the device clock and anchors to the TTL the server granted.
            val pairing = FakeDevicePairingRepository(
                generateResult = Result.success(
                    PairingCode("2Y567", "sess-1", Instant.now().plusSeconds(600), ttlSeconds = 145),
                ),
            )
            val vm = viewModel(pairing)

            vm.start()

            val state = vm.uiState.value
            assertEquals(state.remainingSeconds, state.totalSeconds)
            assertEquals(145L, state.totalSeconds)
            vm.stop()
        }

    @Test
    fun given_code_countdown_reaches_zero_then_requests_new_code() =
        runTest(mainDispatcher.dispatcher) {
            val pairing = FakeDevicePairingRepository(
                generateResult = Result.success(
                    PairingCode("2Y567", "sess-1", Instant.now().plusSeconds(1), ttlSeconds = 1),
                ),
            )
            val vm = viewModel(pairing)

            vm.start()
            advanceTimeBy(1_100)
            runCurrent()

            assertTrue(pairing.generateCount >= 2)
            vm.stop()
        }

    @Test
    fun given_approved_then_exchanges_token_and_shows_paired() =
        runTest(mainDispatcher.dispatcher) {
            val pairing = FakeDevicePairingRepository()
            val vm = viewModel(pairing)
            vm.start()

            pairing.emitStatus(PairingStatus.Approved)
            runCurrent()

            val state = vm.uiState.value
            assertEquals(PairingPhase.Paired, state.phase)
            assertEquals(FakeDevicePairingRepository.DEFAULT_DEVICE, state.device)
            vm.stop()
        }

    @Test
    fun given_approved_and_exchange_succeeds_then_emits_paired_effect() =
        runTest(mainDispatcher.dispatcher) {
            val pairing = FakeDevicePairingRepository()
            val vm = viewModel(pairing)
            vm.start()

            vm.uiEffect.test {
                pairing.emitStatus(PairingStatus.Approved)
                assertEquals(PairingUiEffect.Paired, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
            vm.stop()
        }

    @Test
    fun given_rejected_then_shows_rejected_phase() = runTest(mainDispatcher.dispatcher) {
        val pairing = FakeDevicePairingRepository()
        val vm = viewModel(pairing)
        vm.start()

        pairing.emitStatus(PairingStatus.Rejected)
        runCurrent()

        assertEquals(PairingPhase.Rejected, vm.uiState.value.phase)
        vm.stop()
    }

    @Test
    fun given_socket_expired_then_regenerates_code() = runTest(mainDispatcher.dispatcher) {
        val pairing = FakeDevicePairingRepository()
        val vm = viewModel(pairing)
        vm.start()

        pairing.emitStatus(PairingStatus.Expired)
        runCurrent()

        assertTrue(pairing.generateCount >= 2)
        vm.stop()
    }

    @Test
    fun given_status_stream_fails_then_shows_error_phase_without_crashing() =
        runTest(mainDispatcher.dispatcher) {
            val pairing = FakeDevicePairingRepository().apply {
                statusStreamError = IllegalStateException("Handshake exception, expected 101 but was 400")
            }
            val vm = viewModel(pairing)

            vm.start()
            runCurrent()

            assertEquals(PairingPhase.Error, vm.uiState.value.phase)
            vm.stop()
        }

    @Test
    fun given_unpair_confirmed_when_no_api_then_emits_unavailable_effect() =
        runTest(mainDispatcher.dispatcher) {
            val pairing = FakeDevicePairingRepository()
            val vm = viewModel(pairing)

            vm.uiEffect.test {
                vm.onEvent(PairingUiEvent.UnpairConfirmed)
                assertTrue(awaitItem() is PairingUiEffect.UnpairUnavailable)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun given_unpair_clicked_then_confirmation_shown_and_dismissable() =
        runTest(mainDispatcher.dispatcher) {
            val vm = viewModel(FakeDevicePairingRepository())

            vm.onEvent(PairingUiEvent.UnpairClicked)
            assertTrue(vm.uiState.value.showUnpairConfirmation)

            vm.onEvent(PairingUiEvent.UnpairDismissed)
            assertTrue(!vm.uiState.value.showUnpairConfirmation)
        }
}
