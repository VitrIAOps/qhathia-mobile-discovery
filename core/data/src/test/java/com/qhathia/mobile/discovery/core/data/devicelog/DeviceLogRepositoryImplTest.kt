package com.qhathia.mobile.discovery.core.data.devicelog

import com.qhathia.mobile.discovery.core.domain.model.StoreSession
import com.qhathia.mobile.discovery.core.network.NetworkErrorKind
import com.qhathia.mobile.discovery.core.network.NetworkResult
import com.qhathia.mobile.discovery.core.network.dto.DeviceLogEntryDto
import com.qhathia.mobile.discovery.core.network.dto.DeviceLogsRequestDto
import com.qhathia.mobile.discovery.core.network.service.DeviceLogApiService
import com.qhathia.mobile.discovery.core.testing.FakeStoreSessionRepository
import com.qhathia.mobile.discovery.core.testing.MutableClock
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException
import java.time.Instant

class DeviceLogRepositoryImplTest {

    private val api = mockk<DeviceLogApiService>()
    private val clock = MutableClock(Instant.parse("2026-11-06T12:00:00.250Z"))
    private val paired = FakeStoreSessionRepository(StoreSession(deviceAccessToken = "device-jwt"))

    @Test
    fun given_unpaired_then_fails_without_request() = runTest {
        val repository = DeviceLogRepositoryImpl(api, FakeStoreSessionRepository(), clock)

        assertTrue(repository.reportPinLockout(5, 30).isFailure)
        coVerify(exactly = 0) { api.send(any(), any()) }
    }

    @Test
    fun sends_a_warn_lockout_entry_with_the_device_token() = runTest {
        val body = slot<DeviceLogsRequestDto>()
        coEvery { api.send("device-jwt", capture(body)) } returns NetworkResult.Success(Unit)

        val result = DeviceLogRepositoryImpl(api, paired, clock).reportPinLockout(5, 60)

        assertTrue(result.isSuccess)
        val entry: DeviceLogEntryDto = body.captured.logs.single()
        assertEquals("WARN", entry.level)
        assertEquals("SETTINGS_PIN_LOCKOUT", entry.code)
        assertEquals("2026-11-06T12:00:00Z", entry.timestamp)
        assertEquals(mapOf("failedAttempts" to 5L, "lockoutSeconds" to 60L), entry.context)
    }

    @Test
    fun http_and_network_errors_are_failures() = runTest {
        val repository = DeviceLogRepositoryImpl(api, paired, clock)

        coEvery { api.send(any(), any()) } returns NetworkResult.HttpError(422, "Unprocessable")
        assertTrue(repository.reportPinLockout(5, 30).isFailure)

        coEvery { api.send(any(), any()) } returns NetworkResult.NetworkError(IOException(), NetworkErrorKind.Connectivity)
        assertTrue(repository.reportPinLockout(5, 30).isFailure)
    }
}
