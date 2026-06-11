package com.qhathia.mobile.discovery.core.data.pairing

import com.qhathia.mobile.discovery.core.domain.model.PairingStatus
import com.qhathia.mobile.discovery.core.network.NetworkErrorKind
import com.qhathia.mobile.discovery.core.network.NetworkResult
import com.qhathia.mobile.discovery.core.network.dto.DeviceInfoResponseDto
import com.qhathia.mobile.discovery.core.network.dto.DeviceTokenResponseDto
import com.qhathia.mobile.discovery.core.network.dto.PairingStatusMessageDto
import com.qhathia.mobile.discovery.core.network.dto.PairingTokenResponseDto
import com.qhathia.mobile.discovery.core.network.service.DevicePairingApiService
import com.qhathia.mobile.discovery.core.network.service.PairingSocketClient
import com.qhathia.mobile.discovery.core.testing.FakeStoreSessionRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import java.time.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DevicePairingRepositoryImplTest {

    private val api = mockk<DevicePairingApiService>()
    private val socket = mockk<PairingSocketClient>()
    private val session = FakeStoreSessionRepository()

    private fun repository() = DevicePairingRepositoryImpl(api, socket, session)

    @Test
    fun given_generate_succeeds_then_maps_code() = runTest {
        coEvery { api.generatePairingToken() } returns NetworkResult.Success(
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
        coEvery { api.generatePairingToken() } returns NetworkResult.Success(
            PairingTokenResponseDto("2Y567", "sess-1", Instant.now().plusSeconds(180).toString()),
        )

        val ttl = repository().generateCode().getOrNull()?.ttlSeconds

        assertTrue(ttl != null && ttl in 175..180)
    }

    @Test
    fun given_generate_http_error_then_failure() = runTest {
        coEvery { api.generatePairingToken() } returns NetworkResult.HttpError(503, "Unavailable")

        assertTrue(repository().generateCode().isFailure)
    }

    @Test
    fun given_generate_network_error_then_failure() = runTest {
        coEvery { api.generatePairingToken() } returns
            NetworkResult.NetworkError(RuntimeException("offline"), NetworkErrorKind.Connectivity)

        assertTrue(repository().generateCode().isFailure)
    }

    @Test
    fun given_exchange_succeeds_then_persists_token_and_returns_device() = runTest {
        coEvery { api.exchangeToken("sess-1") } returns NetworkResult.Success(
            DeviceTokenResponseDto(
                deviceId = "dev-1",
                deviceAccessToken = "device-jwt",
                storeId = "store-1",
                deviceName = "Tablet Mostrador 1",
                platform = "ANDROID",
                status = "ACTIVE",
            ),
        )

        val result = repository().exchangeForDeviceToken("sess-1")

        assertTrue(result.isSuccess)
        assertEquals("dev-1", result.getOrNull()?.deviceId)
        assertEquals("device-jwt", session.deviceAccessToken())
    }

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

    @Test
    fun given_unpair_when_no_api_then_failure() = runTest {
        assertTrue(repository().unpair().isFailure)
    }

    @Test
    fun given_status_messages_then_maps_and_filters_unknown() = runTest {
        every { socket.observe("sess-1") } returns flowOf(
            PairingStatusMessageDto("WAITING", "sess-1"),
            PairingStatusMessageDto("BOGUS", "sess-1"),
            PairingStatusMessageDto("APPROVED", "sess-1"),
        )

        val statuses = repository().observeStatus("sess-1").toList()

        assertEquals(listOf(PairingStatus.Waiting, PairingStatus.Approved), statuses)
    }
}
