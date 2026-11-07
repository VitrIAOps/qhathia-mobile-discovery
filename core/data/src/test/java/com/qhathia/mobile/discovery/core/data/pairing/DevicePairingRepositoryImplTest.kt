package com.qhathia.mobile.discovery.core.data.pairing

import com.qhathia.mobile.discovery.core.data.network.ConnectivityDataSource
import com.qhathia.mobile.discovery.core.data.network.ConnectivitySnapshot
import com.qhathia.mobile.discovery.core.domain.model.PairingExchangeError
import com.qhathia.mobile.discovery.core.domain.model.PairingExchangeException
import com.qhathia.mobile.discovery.core.domain.model.PairingStatus
import com.qhathia.mobile.discovery.core.network.NetworkErrorKind
import com.qhathia.mobile.discovery.core.network.NetworkResult
import com.qhathia.mobile.discovery.core.network.dto.DeviceInfoResponseDto
import com.qhathia.mobile.discovery.core.network.dto.DeviceTokenResponseDto
import com.qhathia.mobile.discovery.core.network.dto.PairingStatusMessageDto
import com.qhathia.mobile.discovery.core.network.dto.PairingTokenResponseDto
import com.qhathia.mobile.discovery.core.network.service.DevicePairingApiService
import com.qhathia.mobile.discovery.core.network.service.PairingExchangeResponse
import com.qhathia.mobile.discovery.core.network.service.PairingHandshakeRejectedException
import com.qhathia.mobile.discovery.core.network.service.PairingSocketClient
import com.qhathia.mobile.discovery.core.testing.FakeStoreSessionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import java.io.IOException
import java.time.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DevicePairingRepositoryImplTest {

    private val api = mockk<DevicePairingApiService>()
    private val socket = mockk<PairingSocketClient>()
    private val session = FakeStoreSessionRepository()
    private val connectivity = FakeConnectivityDataSource()
    private val pending = FakePendingPairingSessionStore()

    private fun repository() = DevicePairingRepositoryImpl(api, socket, session, connectivity, pending)

    private fun codeFor(sessionId: String) = NetworkResult.Success(
        PairingTokenResponseDto("2Y567", sessionId, "2026-06-09T18:40:00Z", expiresInSeconds = 300),
    )

    private val tokenDto = DeviceTokenResponseDto(
        deviceId = "dev-1",
        deviceAccessToken = "device-jwt",
        storeId = "store-1",
        deviceName = "Tablet Mostrador 1",
        platform = "ANDROID",
        status = "ACTIVE",
    )

    private fun messages(vararg statuses: String): Flow<PairingStatusMessageDto> =
        flowOf(*statuses.map { PairingStatusMessageDto(it, "sess-1") }.toTypedArray())

    private val dropped: Flow<PairingStatusMessageDto> = flow { throw IOException("socket closed") }
    private val handshakeRejected: Flow<PairingStatusMessageDto> =
        flow { throw PairingHandshakeRejectedException("expected 101 but was 404") }

    // --- generate ---

    @Test
    fun given_generate_succeeds_then_maps_code() = runTest {
        coEvery { api.generatePairingToken(any()) } returns NetworkResult.Success(
            PairingTokenResponseDto("2Y567", "sess-1", "2026-06-09T18:40:00Z", expiresInSeconds = 300),
        )

        val result = repository().generateCode()

        assertTrue(result.isSuccess)
        assertEquals("2Y567", result.getOrNull()?.code)
        assertEquals("sess-1", result.getOrNull()?.sessionId)
        assertEquals(300L, result.getOrNull()?.ttlSeconds)
    }

    @Test
    fun given_generate_without_ttl_then_falls_back_to_expiry() = runTest {
        // Older server omits expiresInSeconds: ttl is derived from expiresAt against the local clock.
        coEvery { api.generatePairingToken(any()) } returns NetworkResult.Success(
            PairingTokenResponseDto("2Y567", "sess-1", Instant.now().plusSeconds(180).toString()),
        )

        val ttl = repository().generateCode().getOrNull()?.ttlSeconds

        assertTrue(ttl != null && ttl in 175..180)
    }

    @Test
    fun given_generate_http_error_then_failure() = runTest {
        coEvery { api.generatePairingToken(any()) } returns NetworkResult.HttpError(503, "Unavailable")

        assertTrue(repository().generateCode().isFailure)
    }

    @Test
    fun given_generate_network_error_then_failure() = runTest {
        coEvery { api.generatePairingToken(any()) } returns
            NetworkResult.NetworkError(RuntimeException("offline"), NetworkErrorKind.Connectivity)

        assertTrue(repository().generateCode().isFailure)
    }

    // --- exchange ---

    @Test
    fun given_exchange_succeeds_then_persists_token_and_returns_device() = runTest {
        coEvery { api.exchangeToken("sess-1") } returns PairingExchangeResponse.Success(tokenDto)

        val result = repository().exchangeForDeviceToken("sess-1")

        assertTrue(result.isSuccess)
        assertEquals("dev-1", result.getOrNull()?.deviceId)
        assertEquals("device-jwt", session.deviceAccessToken())
    }

    @Test
    fun given_exchange_succeeded_when_called_again_then_no_second_request() = runTest {
        coEvery { api.exchangeToken("sess-1") } returns PairingExchangeResponse.Success(tokenDto)
        val repository = repository()

        repository.exchangeForDeviceToken("sess-1")
        val second = repository.exchangeForDeviceToken("sess-1")

        assertEquals("dev-1", second.getOrNull()?.deviceId)
        coVerify(exactly = 1) { api.exchangeToken("sess-1") }
    }

    @Test
    fun given_exchange_connectivity_error_then_retried() = runTest {
        coEvery { api.exchangeToken("sess-1") } returnsMany listOf(
            PairingExchangeResponse.NetworkError(IOException("offline"), NetworkErrorKind.Connectivity),
            PairingExchangeResponse.NetworkError(IOException("timeout"), NetworkErrorKind.Timeout),
            PairingExchangeResponse.Success(tokenDto),
        )

        val result = repository().exchangeForDeviceToken("sess-1")

        assertTrue(result.isSuccess)
        coVerify(exactly = 3) { api.exchangeToken("sess-1") }
    }

    @Test
    fun given_exchange_keeps_failing_offline_then_gives_up_with_connectivity() = runTest {
        coEvery { api.exchangeToken("sess-1") } returns
            PairingExchangeResponse.NetworkError(IOException("offline"), NetworkErrorKind.Connectivity)

        val result = repository().exchangeForDeviceToken("sess-1")

        assertEquals(PairingExchangeError.Connectivity, result.exchangeError())
        coVerify(exactly = DevicePairingRepositoryImpl.MAX_EXCHANGE_RETRIES + 1) { api.exchangeToken("sess-1") }
    }

    @Test
    fun given_exchange_already_used_then_not_retried() = runTest {
        coEvery { api.exchangeToken("sess-1") } returns
            PairingExchangeResponse.HttpError(410, "PAIRING_TOKEN_ALREADY_USED")

        val result = repository().exchangeForDeviceToken("sess-1")

        assertEquals(PairingExchangeError.AlreadyUsed, result.exchangeError())
        coVerify(exactly = 1) { api.exchangeToken("sess-1") }
    }

    @Test
    fun exchange_http_errors_map_by_error_code() {
        val cases = mapOf(
            PairingExchangeResponse.HttpError(409, "PAIRING_NOT_APPROVED") to PairingExchangeError.NotApproved,
            PairingExchangeResponse.HttpError(409, "PAIRING_REJECTED") to PairingExchangeError.Rejected,
            PairingExchangeResponse.HttpError(409, null) to PairingExchangeError.NotApproved,
            PairingExchangeResponse.HttpError(410, "PAIRING_TOKEN_EXPIRED") to PairingExchangeError.Expired,
            PairingExchangeResponse.HttpError(410, "PAIRING_TOKEN_ALREADY_USED") to PairingExchangeError.AlreadyUsed,
            PairingExchangeResponse.HttpError(410, null) to PairingExchangeError.Expired,
            PairingExchangeResponse.HttpError(404, "NOT_FOUND") to PairingExchangeError.NotFound,
            PairingExchangeResponse.HttpError(500, null) to PairingExchangeError.Other,
        )
        cases.forEach { (response, expected) -> assertEquals(expected, response.toExchangeError(), "$response") }
    }

    // --- status ---

    @Test
    fun given_status_messages_then_maps_and_filters_unknown() = runTest {
        every { socket.observe("sess-1") } returns messages("WAITING", "BOGUS", "APPROVED")

        val statuses = repository().observeStatus("sess-1").toList()

        assertEquals(listOf(PairingStatus.Waiting, PairingStatus.Approved), statuses)
    }

    @Test
    fun given_socket_completes_without_final_status_then_reconnects_after_backoff() = runTest {
        every { socket.observe("sess-1") } returnsMany listOf(
            messages("WAITING"),
            messages("WAITING", "APPROVED"),
        )

        val statuses = repository().observeStatus("sess-1").toList()

        assertEquals(listOf(PairingStatus.Waiting, PairingStatus.Waiting, PairingStatus.Approved), statuses)
        assertEquals(1_000L, currentTime)
        verify(exactly = 2) { socket.observe("sess-1") }
        coVerify(exactly = 0) { api.exchangeToken(any()) }
    }

    @Test
    fun given_repeated_drops_then_backoff_is_1_2_4_8_8_seconds() = runTest {
        every { socket.observe("sess-1") } returnsMany
            List(5) { dropped } + listOf(messages("APPROVED"))

        repository().observeStatus("sess-1").toList()

        assertEquals(1_000L + 2_000 + 4_000 + 8_000 + 8_000, currentTime)
    }

    @Test
    fun given_back_online_then_reconnects_without_waiting_for_backoff() = runTest {
        every { socket.observe("sess-1") } returnsMany List(3) { dropped } + listOf(messages("APPROVED"))
        val statuses = mutableListOf<PairingStatus>()
        val job = launch { repository().observeStatus("sess-1").collect { statuses += it } }
        advanceTimeBy(3_100) // drops at 0 s, 1 s and 3 s; now waiting 4 s
        connectivity.state.value = connectivity.state.value.copy(hasInternet = false)
        runCurrent()
        advanceTimeBy(500)

        connectivity.state.value = connectivity.state.value.copy(hasInternet = true)
        runCurrent()

        assertEquals(listOf(PairingStatus.Approved), statuses)
        assertEquals(3_600L, currentTime)
        job.cancel()
    }

    @Test
    fun given_waiting_for_minutes_then_never_calls_exchange() = runTest {
        every { socket.observe("sess-1") } returnsMany listOf(
            dropped,
            flow {
                emit(PairingStatusMessageDto("WAITING", "sess-1"))
                awaitCancellation()
            },
        )
        val job = launch { repository().observeStatus("sess-1").collect {} }

        advanceTimeBy(5 * 60_000L)

        coVerify(exactly = 0) { api.exchangeToken(any()) }
        job.cancel()
    }

    @Test
    fun given_handshake_rejected_and_fallback_200_then_approved_with_single_exchange() = runTest {
        every { socket.observe("sess-1") } returns handshakeRejected
        coEvery { api.exchangeToken("sess-1") } returns PairingExchangeResponse.Success(tokenDto)
        val repository = repository()

        val statuses = repository.observeStatus("sess-1").toList()
        // The ViewModel then exchanges on APPROVED: served from the cache.
        val device = repository.exchangeForDeviceToken("sess-1")

        assertEquals(listOf(PairingStatus.Approved), statuses)
        assertEquals("dev-1", device.getOrNull()?.deviceId)
        coVerify(exactly = 1) { api.exchangeToken("sess-1") }
    }

    @Test
    fun given_handshake_rejected_and_not_approved_then_keeps_reconnecting() = runTest {
        every { socket.observe("sess-1") } returnsMany listOf(handshakeRejected, messages("APPROVED"))
        coEvery { api.exchangeToken("sess-1") } returns
            PairingExchangeResponse.HttpError(409, "PAIRING_NOT_APPROVED")

        val statuses = repository().observeStatus("sess-1").toList()

        assertEquals(listOf(PairingStatus.Approved), statuses)
        verify(exactly = 2) { socket.observe("sess-1") }
    }

    @Test
    fun given_handshake_rejected_and_rejected_then_rejected() = runTest {
        every { socket.observe("sess-1") } returns handshakeRejected
        coEvery { api.exchangeToken("sess-1") } returns PairingExchangeResponse.HttpError(409, "PAIRING_REJECTED")

        assertEquals(listOf(PairingStatus.Rejected), repository().observeStatus("sess-1").toList())
    }

    @Test
    fun given_handshake_rejected_and_expired_or_missing_then_expired() = runTest {
        every { socket.observe("sess-1") } returns handshakeRejected
        coEvery { api.exchangeToken("sess-1") } returnsMany listOf(
            PairingExchangeResponse.HttpError(410, "PAIRING_TOKEN_EXPIRED"),
            PairingExchangeResponse.HttpError(404, "NOT_FOUND"),
        )

        assertEquals(listOf(PairingStatus.Expired), repository().observeStatus("sess-1").toList())
        assertEquals(listOf(PairingStatus.Expired), repository().observeStatus("sess-1").toList())
    }

    @Test
    fun given_handshake_rejected_and_already_used_then_flow_fails() = runTest {
        every { socket.observe("sess-1") } returns handshakeRejected
        coEvery { api.exchangeToken("sess-1") } returns
            PairingExchangeResponse.HttpError(410, "PAIRING_TOKEN_ALREADY_USED")

        val failure = runCatching { repository().observeStatus("sess-1").toList() }.exceptionOrNull()

        assertEquals(PairingExchangeError.AlreadyUsed, (failure as PairingExchangeException).error)
    }

    // --- paired device / unpair ---

    @Test
    fun given_no_token_when_get_paired_then_failure() = runTest {
        assertTrue(repository().getPairedDevice().isFailure)
    }

    @Test
    fun given_token_when_get_paired_then_success() = runTest {
        session.saveDeviceSession("device-jwt", "Tablet Mostrador 1")
        coEvery { api.getPairedDevice("device-jwt") } returns NetworkResult.Success(
            DeviceInfoResponseDto(
                deviceId = "dev-1",
                storeId = "store-1",
                deviceName = "Tablet Mostrador 1",
                platform = "ANDROID",
                status = "ACTIVE",
                lastSyncAt = "2026-06-09T18:40:00Z",
            ),
        )

        val result = repository().getPairedDevice()

        assertTrue(result.isSuccess)
        assertEquals("dev-1", result.getOrNull()?.deviceId)
    }

    private fun deviceInfo(storeName: String? = null) = DeviceInfoResponseDto(
        deviceId = "dev-1",
        storeId = "store-1",
        deviceName = "Tablet Mostrador 1",
        platform = "ANDROID",
        status = "ACTIVE",
        lastSeenAt = "2026-10-30T15:00:00Z",
        storeName = storeName,
    )

    @Test
    fun given_exchange_succeeds_then_code_survives_a_restart() = runTest {
        coEvery { api.generatePairingToken(any()) } returns NetworkResult.Success(
            PairingTokenResponseDto("2Y567", "sess-1", "2026-06-09T18:40:00Z", expiresInSeconds = 300),
        )
        coEvery { api.exchangeToken("sess-1") } returns PairingExchangeResponse.Success(tokenDto)
        coEvery { api.getPairedDevice("device-jwt") } returns NetworkResult.Success(deviceInfo())
        val pairing = repository()
        pairing.generateCode()
        pairing.exchangeForDeviceToken("sess-1")

        // A new repository instance stands for the app restarted: nothing is left in memory.
        val device = repository().getPairedDevice().getOrNull()

        assertEquals("2Y567", device?.pairingCode)
    }

    @Test
    fun given_device_paired_before_the_code_was_saved_then_code_is_null() = runTest {
        session.saveDeviceSession("device-jwt", "Tablet Mostrador 1")
        coEvery { api.getPairedDevice("device-jwt") } returns NetworkResult.Success(deviceInfo())

        assertEquals(null, repository().getPairedDevice().getOrNull()?.pairingCode)
    }

    @Test
    fun given_clear_session_then_code_is_removed() = runTest {
        session.saveDeviceSession("device-jwt", "Tablet Mostrador 1", pairingCode = "2Y567")

        session.clearDeviceSession()

        assertEquals(null, session.observe().first().pairingCode)
    }

    @Test
    fun given_last_seen_then_it_is_mapped() = runTest {
        session.saveDeviceSession("device-jwt", "Tablet Mostrador 1")
        coEvery { api.getPairedDevice("device-jwt") } returns NetworkResult.Success(deviceInfo())

        val device = repository().getPairedDevice().getOrNull()

        assertEquals(Instant.parse("2026-10-30T15:00:00Z"), device?.lastSeenAt)
        assertEquals(null, device?.lastSyncAt)
    }

    @Test
    fun given_store_name_sent_then_it_is_saved_and_returned() = runTest {
        session.saveDeviceSession("device-jwt", "Tablet Mostrador 1")
        coEvery { api.getPairedDevice("device-jwt") } returns
            NetworkResult.Success(deviceInfo(storeName = "Ferretería Central"))

        val device = repository().getPairedDevice().getOrNull()

        assertEquals("Ferretería Central", device?.storeName)
        assertEquals("Ferretería Central", session.observe().first().storeName)
    }

    @Test
    fun given_blank_store_name_then_saved_name_is_kept() = runTest {
        session.saveDeviceSession("device-jwt", "Tablet Mostrador 1")
        session.updateStoreName("Ferretería Central")
        coEvery { api.getPairedDevice("device-jwt") } returns NetworkResult.Success(deviceInfo(storeName = " "))

        val device = repository().getPairedDevice().getOrNull()

        assertEquals("Ferretería Central", device?.storeName)
        assertEquals("Ferretería Central", session.observe().first().storeName)
    }

    @Test
    fun given_unpair_when_no_api_then_failure() = runTest {
        assertTrue(repository().unpair().isFailure)
    }

    private fun Result<*>.exchangeError(): PairingExchangeError? =
        (exceptionOrNull() as? PairingExchangeException)?.error

    // --- store name from the exchange (F-05) ---

    @Test
    fun exchange_with_store_name_saves_it_for_home() = runTest {
        coEvery { api.exchangeToken("sess-1") } returns
            PairingExchangeResponse.Success(tokenDto.copy(storeName = "QA Discovery — Ferretería"))

        val device = repository().exchangeForDeviceToken("sess-1").getOrNull()

        assertEquals("QA Discovery — Ferretería", session.observe().first().storeName)
        assertEquals("QA Discovery — Ferretería", device?.storeName)
    }

    @Test
    fun exchange_without_store_name_keeps_the_session_name() = runTest {
        session.updateStoreName("Ferretería")
        coEvery { api.exchangeToken("sess-1") } returns PairingExchangeResponse.Success(tokenDto.copy(storeName = " "))

        repository().exchangeForDeviceToken("sess-1")

        assertEquals("Ferretería", session.observe().first().storeName)
    }

    // --- abandoned codes (F-02) ---

    @Test
    fun first_code_replaces_nothing_and_is_kept() = runTest {
        coEvery { api.generatePairingToken(any()) } returns codeFor("sess-1")

        repository().generateCode()

        coVerify { api.generatePairingToken(null) }
        assertEquals("sess-1", pending.value)
    }

    @Test
    fun second_code_replaces_the_first_even_after_a_restart() = runTest {
        coEvery { api.generatePairingToken(any()) } returnsMany listOf(codeFor("sess-1"), codeFor("sess-2"))
        repository().generateCode()

        repository().generateCode() // a new instance stands for the restarted app

        coVerify { api.generatePairingToken("sess-1") }
        assertEquals("sess-2", pending.value)
    }

    @Test
    fun successful_exchange_forgets_the_code() = runTest {
        pending.value = "sess-1"
        coEvery { api.exchangeToken("sess-1") } returns PairingExchangeResponse.Success(tokenDto)

        repository().exchangeForDeviceToken("sess-1")

        assertNull(pending.value)
    }

    @Test
    fun abandoned_code_is_cancelled_and_forgotten() = runTest {
        pending.value = "sess-1"
        coEvery { api.cancelPairingSession("sess-1") } returns NetworkResult.Success(Unit)

        assertTrue(repository().cancelAbandonedSession().isSuccess)

        assertNull(pending.value)
    }

    @Test
    fun abandoned_code_unknown_to_the_backend_is_forgotten() = runTest {
        pending.value = "sess-1"
        coEvery { api.cancelPairingSession("sess-1") } returns NetworkResult.HttpError(404, "Not Found")

        assertTrue(repository().cancelAbandonedSession().isSuccess)
        assertNull(pending.value)
    }

    @Test
    fun failed_cancel_keeps_the_code_for_the_next_request() = runTest {
        pending.value = "sess-1"
        coEvery { api.cancelPairingSession("sess-1") } returns
            NetworkResult.NetworkError(IOException("offline"), NetworkErrorKind.Connectivity)

        assertTrue(repository().cancelAbandonedSession().isFailure)
        assertEquals("sess-1", pending.value)
    }

    @Test
    fun nothing_kept_means_no_request() = runTest {
        assertTrue(repository().cancelAbandonedSession().isSuccess)

        coVerify(exactly = 0) { api.cancelPairingSession(any()) }
    }

    @Test
    fun paired_tablet_forgets_the_code_without_cancelling() = runTest {
        pending.value = "sess-1"
        session.saveDeviceSession("device-jwt", "Tablet Mostrador 1")

        repository().cancelAbandonedSession()

        coVerify(exactly = 0) { api.cancelPairingSession(any()) }
        assertNull(pending.value)
    }

    @Test
    fun late_cancel_does_not_forget_a_newer_code() = runTest {
        pending.value = "sess-1"
        coEvery { api.cancelPairingSession("sess-1") } coAnswers {
            pending.value = "sess-2" // "Continuar" got a new code meanwhile
            NetworkResult.Success(Unit)
        }

        repository().cancelAbandonedSession()

        assertEquals("sess-2", pending.value)
    }

    @Test
    fun cancelled_status_reads_as_expired() {
        assertEquals(PairingStatus.Expired, PairingStatusMessageDto("CANCELLED", "sess-1").toDomainOrNull())
    }

    private class FakePendingPairingSessionStore : PendingPairingSessionStore {
        var value: String? = null

        override suspend fun get(): String? = value

        override suspend fun set(sessionId: String) {
            value = sessionId
        }

        override suspend fun clearIf(sessionId: String) {
            if (value == sessionId) value = null
        }
    }

    private class FakeConnectivityDataSource : ConnectivityDataSource {
        val state = MutableStateFlow(
            ConnectivitySnapshot(
                wifiEnabled = true,
                mobileDataActive = false,
                airplaneModeActive = false,
                connectedSsid = null,
                wifiActive = true,
                hasInternet = true,
            ),
        )

        override fun observe(): Flow<ConnectivitySnapshot> = state
    }
}
