package com.qhathia.mobile.discovery.feature.pairing

import app.cash.turbine.test
import com.qhathia.mobile.discovery.core.domain.model.PairingCode
import com.qhathia.mobile.discovery.core.domain.model.PairingExchangeError
import com.qhathia.mobile.discovery.core.domain.model.PairingExchangeException
import com.qhathia.mobile.discovery.core.domain.model.PairingStatus
import com.qhathia.mobile.discovery.core.domain.usecase.ExchangeDeviceTokenUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.GeneratePairingCodeUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.GetHeldPairingCodeUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.GetPairedDeviceUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.HoldPairingCodeUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ObservePairingStatusUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ObserveStoreSessionUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.UnpairDeviceUseCase
import com.qhathia.mobile.discovery.core.testing.FakeDevicePairingRepository
import com.qhathia.mobile.discovery.core.testing.FakeHeldPairingCodeRepository
import com.qhathia.mobile.discovery.core.testing.FakeStoreSessionRepository
import com.qhathia.mobile.discovery.core.testing.MainDispatcherExtension
import com.qhathia.mobile.discovery.feature.pairing.model.PairingError
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

    /** Outlives each ViewModel, like the app singleton. */
    private val heldCodes = FakeHeldPairingCodeRepository()

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
        getHeldPairingCode = GetHeldPairingCodeUseCase(heldCodes),
        holdPairingCode = HoldPairingCodeUseCase(heldCodes),
        clock = { mainDispatcher.dispatcher.scheduler.currentTime },
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
    fun given_status_stream_fails_then_shows_link_error_without_crashing() =
        runTest(mainDispatcher.dispatcher) {
            // The repository reconnects drops itself; its flow fails only when the session is unusable.
            val pairing = FakeDevicePairingRepository().apply {
                statusStreamError = PairingExchangeException(PairingExchangeError.AlreadyUsed)
            }
            val vm = viewModel(pairing)

            vm.start()
            runCurrent()

            assertEquals(PairingPhase.Error, vm.uiState.value.phase)
            assertEquals(PairingError.Linking, vm.uiState.value.error)
            vm.stop()
        }

    @Test
    fun given_exchange_already_used_then_link_error_offers_new_code() =
        runTest(mainDispatcher.dispatcher) {
            val pairing = FakeDevicePairingRepository(
                exchangeResult = Result.failure(PairingExchangeException(PairingExchangeError.AlreadyUsed)),
            )
            val vm = viewModel(pairing)
            vm.start()

            pairing.emitStatus(PairingStatus.Approved)
            runCurrent()
            assertEquals(PairingError.Linking, vm.uiState.value.error)

            vm.onEvent(PairingUiEvent.GenerateCode)
            runCurrent()
            assertEquals(PairingPhase.WaitingForApproval, vm.uiState.value.phase)
            assertEquals(2, pairing.generateCount)
            vm.stop()
        }

    @Test
    fun given_generate_fails_then_code_generation_error() = runTest(mainDispatcher.dispatcher) {
        val pairing = FakeDevicePairingRepository(generateResult = Result.failure(RuntimeException("503")))
        val vm = viewModel(pairing)

        vm.start()
        runCurrent()

        assertEquals(PairingPhase.Error, vm.uiState.value.phase)
        assertEquals(PairingError.CodeGeneration, vm.uiState.value.error)
    }

    @Test
    fun given_approved_twice_then_exchanges_once() = runTest(mainDispatcher.dispatcher) {
        val pairing = FakeDevicePairingRepository()
        val vm = viewModel(pairing)
        vm.start()

        pairing.emitStatus(PairingStatus.Approved)
        pairing.emitStatus(PairingStatus.Approved)
        runCurrent()

        assertEquals(1, pairing.exchangeCount)
        vm.stop()
    }

    @Test
    fun given_waiting_then_never_exchanges() = runTest(mainDispatcher.dispatcher) {
        val pairing = FakeDevicePairingRepository()
        val vm = viewModel(pairing)
        vm.start()

        pairing.emitStatus(PairingStatus.Waiting)
        advanceTimeBy(120_000)

        assertEquals(0, pairing.exchangeCount)
        assertEquals(PairingPhase.WaitingForApproval, vm.uiState.value.phase)
        vm.stop()
    }

    @Test
    fun given_stop_and_start_within_ttl_then_same_code_and_remaining_time() =
        runTest(mainDispatcher.dispatcher) {
            val pairing = FakeDevicePairingRepository()
            val vm = viewModel(pairing)
            vm.start()
            runCurrent()

            vm.stop() // screen off / leaving the section
            advanceTimeBy(100_000)
            vm.start()
            runCurrent()

            val state = vm.uiState.value
            assertEquals(1, pairing.generateCount)
            assertEquals("2Y567", state.code)
            assertEquals(PairingPhase.WaitingForApproval, state.phase)
            assertEquals(200L, state.remainingSeconds)
            assertEquals(300L, state.totalSeconds)

            // The status subscription is resumed too.
            pairing.emitStatus(PairingStatus.Approved)
            runCurrent()
            assertEquals(PairingPhase.Paired, vm.uiState.value.phase)
        }

    @Test
    fun given_section_left_and_viewmodel_destroyed_then_new_viewmodel_shows_same_code() =
        runTest(mainDispatcher.dispatcher) {
            // Onboarding "Volver" pops the pairing destination: its ViewModel is destroyed.
            val pairing = FakeDevicePairingRepository()
            val first = viewModel(pairing)
            first.start()
            runCurrent()
            first.stop()
            advanceTimeBy(30_000)

            val second = viewModel(pairing)
            second.start()
            runCurrent()

            val state = second.uiState.value
            assertEquals(1, pairing.generateCount)
            assertEquals(PairingPhase.WaitingForApproval, state.phase)
            assertEquals("2Y567", state.code)
            assertEquals(270L, state.remainingSeconds)
            assertEquals(300L, state.totalSeconds)
            second.stop()
        }

    @Test
    fun given_rejected_then_next_visit_generates_a_new_code() = runTest(mainDispatcher.dispatcher) {
        val pairing = FakeDevicePairingRepository()
        val first = viewModel(pairing)
        first.start()
        pairing.emitStatus(PairingStatus.Rejected)
        runCurrent()

        val second = viewModel(pairing)
        second.start()
        runCurrent()

        assertEquals(2, pairing.generateCount)
        second.stop()
    }

    @Test
    fun given_stop_and_start_after_ttl_then_new_code() = runTest(mainDispatcher.dispatcher) {
        val pairing = FakeDevicePairingRepository()
        val vm = viewModel(pairing)
        vm.start()
        runCurrent()

        vm.stop()
        advanceTimeBy(301_000)
        vm.start()
        runCurrent()

        assertEquals(2, pairing.generateCount)
        assertEquals(PairingPhase.WaitingForApproval, vm.uiState.value.phase)
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
