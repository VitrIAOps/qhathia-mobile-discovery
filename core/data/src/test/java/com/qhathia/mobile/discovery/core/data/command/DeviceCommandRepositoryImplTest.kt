package com.qhathia.mobile.discovery.core.data.command

import app.cash.turbine.test
import com.qhathia.mobile.discovery.core.domain.model.StoreSession
import com.qhathia.mobile.discovery.core.network.NetworkResult
import com.qhathia.mobile.discovery.core.network.dto.DeviceCommandAckDto
import com.qhathia.mobile.discovery.core.network.service.DeviceCommandApiService
import com.qhathia.mobile.discovery.core.testing.FakeStoreSessionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DeviceCommandRepositoryImplTest {

    private val api = mockk<DeviceCommandApiService>()
    private val paired = FakeStoreSessionRepository(StoreSession(deviceAccessToken = "device-jwt"))

    @Test
    fun given_unpaired_then_fails_without_request() = runTest {
        val result = DeviceCommandRepositoryImpl(api, FakeStoreSessionRepository()).ack("c1", true)

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { api.ack(any(), any(), any()) }
    }

    @Test
    fun given_204_then_succeeds_with_token_and_body() = runTest {
        coEvery { api.ack(any(), any(), any()) } returns NetworkResult.Success(Unit)

        val result = DeviceCommandRepositoryImpl(api, paired).ack("c1", false, "sin conexión")

        assertTrue(result.isSuccess)
        coVerify { api.ack("device-jwt", "c1", DeviceCommandAckDto(false, "sin conexión")) }
    }

    @Test
    fun given_http_error_then_fails() = runTest {
        coEvery { api.ack(any(), any(), any()) } returns NetworkResult.HttpError(404, "Not Found")

        assertTrue(DeviceCommandRepositoryImpl(api, paired).ack("c1", true).isFailure)
    }

    @Test
    fun refresh_signal_reaches_current_collector_only() = runTest {
        val signal = CatalogRefreshSignalImpl()

        signal.request() // nobody listening: dropped, not replayed later
        signal.observe().test {
            signal.request()
            awaitItem()
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }
}
