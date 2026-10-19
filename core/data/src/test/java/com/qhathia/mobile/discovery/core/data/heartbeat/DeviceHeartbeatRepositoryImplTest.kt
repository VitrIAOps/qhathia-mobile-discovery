package com.qhathia.mobile.discovery.core.data.heartbeat

import app.cash.turbine.test
import com.qhathia.mobile.discovery.core.domain.model.StoreSession
import com.qhathia.mobile.discovery.core.network.NetworkErrorKind
import com.qhathia.mobile.discovery.core.network.NetworkResult
import com.qhathia.mobile.discovery.core.network.dto.HeartbeatResponseDto
import com.qhathia.mobile.discovery.core.network.dto.PendingCommandDto
import com.qhathia.mobile.discovery.core.network.service.DeviceHeartbeatApiService
import com.qhathia.mobile.discovery.core.testing.DeviceHeartbeatFixtures
import com.qhathia.mobile.discovery.core.testing.FakeStoreSessionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException

class DeviceHeartbeatRepositoryImplTest {

    private val api = mockk<DeviceHeartbeatApiService>()
    private val pairedSession = FakeStoreSessionRepository(StoreSession(deviceAccessToken = "device-jwt"))
    private val telemetry = DeviceHeartbeatFixtures.telemetry()

    private fun respond(dto: HeartbeatResponseDto) {
        coEvery { api.sendHeartbeat(any(), any()) } returns NetworkResult.Success(dto)
    }

    @Test
    fun given_unpaired_then_fails_without_request() = runTest {
        val repository = DeviceHeartbeatRepositoryImpl(api, FakeStoreSessionRepository())

        val result = repository.sendHeartbeat(telemetry)

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { api.sendHeartbeat(any(), any()) }
    }

    @Test
    fun given_paired_then_sends_device_token_and_mapped_telemetry() = runTest {
        respond(HeartbeatResponseDto(settingsVersion = 4))
        val repository = DeviceHeartbeatRepositoryImpl(api, pairedSession)

        val result = repository.sendHeartbeat(telemetry)

        assertEquals(4L, result.getOrNull()?.settingsVersion)
        coVerify { api.sendHeartbeat("device-jwt", telemetry.toRequestDto()) }
    }

    @Test
    fun success_is_published_and_replayed_to_late_subscriber() = runTest {
        respond(HeartbeatResponseDto(settingsVersion = 5))
        val repository = DeviceHeartbeatRepositoryImpl(api, pairedSession)

        repository.sendHeartbeat(telemetry)

        repository.observeHeartbeats().test {
            assertEquals(5L, awaitItem().settingsVersion)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun command_is_received_once_and_not_replayed() = runTest {
        respond(HeartbeatResponseDto(pendingCommands = listOf(PendingCommandDto("c1", "REFRESH_CATALOG"))))
        val repository = DeviceHeartbeatRepositoryImpl(api, pairedSession)

        repository.sendHeartbeat(telemetry)

        repository.observePendingCommands().test {
            assertEquals("c1", awaitItem().id)
            cancelAndIgnoreRemainingEvents()
        }
        repository.observePendingCommands().test {
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun http_error_fails_and_publishes_nothing() = runTest {
        coEvery { api.sendHeartbeat(any(), any()) } returns NetworkResult.HttpError(500, "Internal Server Error")
        val repository = DeviceHeartbeatRepositoryImpl(api, pairedSession)

        val result = repository.sendHeartbeat(telemetry)

        assertTrue(result.isFailure)
        repository.observeHeartbeats().test {
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun network_error_fails_with_cause() = runTest {
        val cause = IOException("offline")
        coEvery { api.sendHeartbeat(any(), any()) } returns NetworkResult.NetworkError(cause, NetworkErrorKind.Connectivity)
        val repository = DeviceHeartbeatRepositoryImpl(api, pairedSession)

        val result = repository.sendHeartbeat(telemetry)

        assertEquals(cause, result.exceptionOrNull())
    }
}
