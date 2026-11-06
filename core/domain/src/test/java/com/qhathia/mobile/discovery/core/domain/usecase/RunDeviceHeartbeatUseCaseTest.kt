package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.model.DeviceCommand
import com.qhathia.mobile.discovery.core.domain.model.DeviceTelemetry
import com.qhathia.mobile.discovery.core.domain.model.HeartbeatResult
import com.qhathia.mobile.discovery.core.domain.model.NetworkConnectivityState
import com.qhathia.mobile.discovery.core.domain.model.StoreSession
import com.qhathia.mobile.discovery.core.domain.repository.DeviceHeartbeatRepository
import com.qhathia.mobile.discovery.core.testing.DeviceHeartbeatFixtures
import com.qhathia.mobile.discovery.core.testing.FakeDeviceHeartbeatRepository
import com.qhathia.mobile.discovery.core.testing.FakeDeviceTelemetryProvider
import com.qhathia.mobile.discovery.core.testing.FakeNetworkSettingsRepository
import com.qhathia.mobile.discovery.core.testing.FakeStoreSessionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class RunDeviceHeartbeatUseCaseTest {

    private val interval = 60.seconds
    private val paired = StoreSession(deviceAccessToken = "device-jwt", deviceName = "Tablet 1")

    private val repository = FakeDeviceHeartbeatRepository()
    private val telemetry = FakeDeviceTelemetryProvider()
    private val network = FakeNetworkSettingsRepository(NetworkConnectivityState(hasInternet = true))
    private val retryDelays = listOf(5.seconds, 10.seconds, 20.seconds, 40.seconds)

    private fun TestScope.start(session: FakeStoreSessionRepository, repo: DeviceHeartbeatRepository = repository) {
        val useCase = RunDeviceHeartbeatUseCase(
            session,
            SendHeartbeatUseCase(telemetry, repo),
            network,
            interval,
            retryDelays,
        )
        backgroundScope.launch { useCase() }
        runCurrent()
    }

    @Test
    fun given_paired_on_start_then_sends_immediately() = runTest {
        start(FakeStoreSessionRepository(paired))

        assertEquals(1, repository.sent.size)
        assertEquals(telemetry.telemetry, repository.sent.single())
    }

    @Test
    fun given_paired_then_sends_once_per_interval() = runTest {
        start(FakeStoreSessionRepository(paired))

        advanceTimeBy(interval.inWholeMilliseconds - 1)
        runCurrent()
        assertEquals(1, repository.sent.size)

        advanceTimeBy(1)
        runCurrent()
        assertEquals(2, repository.sent.size)

        advanceTimeBy(interval.inWholeMilliseconds * 3)
        runCurrent()
        assertEquals(5, repository.sent.size)
    }

    @Test
    fun given_unpaired_then_never_sends() = runTest {
        start(FakeStoreSessionRepository())

        advanceTimeBy(interval.inWholeMilliseconds * 5)
        runCurrent()

        assertEquals(0, repository.sent.size)
    }

    @Test
    fun given_pairing_completes_then_sends_right_away() = runTest {
        val session = FakeStoreSessionRepository()
        start(session)
        advanceTimeBy(30_000)

        session.saveDeviceSession(deviceAccessToken = "device-jwt", deviceName = "Tablet 1")
        runCurrent()

        assertEquals(1, repository.sent.size)
    }

    @Test
    fun given_session_cleared_then_loop_stops() = runTest {
        val session = FakeStoreSessionRepository(paired)
        start(session)

        session.clearDeviceSession()
        runCurrent()
        advanceTimeBy(interval.inWholeMilliseconds * 5)
        runCurrent()

        assertEquals(1, repository.sent.size)
    }

    @Test
    fun given_store_name_changes_then_no_extra_heartbeat() = runTest {
        val session = FakeStoreSessionRepository(paired)
        start(session)

        session.updateStoreName("El Martillo")
        runCurrent()

        assertEquals(1, repository.sent.size)
    }

    @Test
    fun given_failed_heartbeat_then_retried_after_5_10_20_40_s_then_every_interval() = runTest {
        repository.nextResult = Result.failure(IllegalStateException("HTTP 500"))
        start(FakeStoreSessionRepository(paired)) // t = 0

        advanceTimeBy(4_999); runCurrent()
        assertEquals(1, repository.sent.size)
        advanceTimeBy(1); runCurrent() // t = 5 s
        assertEquals(2, repository.sent.size)
        advanceTimeBy(10_000); runCurrent() // t = 15 s
        assertEquals(3, repository.sent.size)
        advanceTimeBy(20_000); runCurrent() // t = 35 s
        assertEquals(4, repository.sent.size)
        advanceTimeBy(40_000); runCurrent() // t = 75 s
        assertEquals(5, repository.sent.size)
        advanceTimeBy(interval.inWholeMilliseconds); runCurrent() // t = 135 s
        assertEquals(6, repository.sent.size)
    }

    @Test
    fun given_success_after_failures_then_back_to_the_interval() = runTest {
        repository.nextResult = Result.failure(IllegalStateException("HTTP 500"))
        start(FakeStoreSessionRepository(paired))
        repository.nextResult = Result.success(DeviceHeartbeatFixtures.result())

        advanceTimeBy(5_000); runCurrent() // the retry succeeds
        assertEquals(2, repository.sent.size)
        advanceTimeBy(interval.inWholeMilliseconds - 1); runCurrent()
        assertEquals(2, repository.sent.size)
        advanceTimeBy(1); runCurrent()
        assertEquals(3, repository.sent.size)
    }

    @Test
    fun given_internet_comes_back_then_heartbeat_sent_at_once() = runTest {
        network.connectivity.value = NetworkConnectivityState(hasInternet = false)
        repository.nextResult = Result.failure(IllegalStateException("offline"))
        start(FakeStoreSessionRepository(paired))

        advanceTimeBy(2_000)
        network.connectivity.value = NetworkConnectivityState(hasInternet = true)
        runCurrent()

        assertEquals(2, repository.sent.size) // at 2 s, not at the 5 s retry
    }

    @Test
    fun given_internet_comes_back_during_the_interval_then_heartbeat_sent_at_once() = runTest {
        start(FakeStoreSessionRepository(paired))
        network.connectivity.value = NetworkConnectivityState(hasInternet = false)
        runCurrent()

        advanceTimeBy(20_000)
        network.connectivity.value = NetworkConnectivityState(hasInternet = true)
        runCurrent()

        assertEquals(2, repository.sent.size)
    }

    @Test
    fun given_connectivity_changes_without_regaining_internet_then_no_extra_heartbeat() = runTest {
        start(FakeStoreSessionRepository(paired))

        network.connectivity.value = NetworkConnectivityState(hasInternet = true, connectedSsid = "Tienda")
        runCurrent()

        assertEquals(1, repository.sent.size)
    }

    @Test
    fun given_heartbeat_throws_then_loop_survives() = runTest {
        var attempts = 0
        val throwing = object : DeviceHeartbeatRepository {
            override suspend fun sendHeartbeat(telemetry: DeviceTelemetry): Result<HeartbeatResult> {
                attempts++
                throw IllegalStateException("boom")
            }
            override fun observeHeartbeats(): Flow<HeartbeatResult> = emptyFlow()
            override fun observePendingCommands(): Flow<DeviceCommand> = emptyFlow()
        }
        start(FakeStoreSessionRepository(paired), throwing)

        advanceTimeBy(5_000)
        runCurrent()

        assertEquals(2, attempts) // a throw counts as a failure: retried after 5 s
    }

    @Test
    fun send_heartbeat_use_case_forwards_current_telemetry() = runTest {
        telemetry.telemetry = DeviceHeartbeatFixtures.telemetry(batteryLevel = null)

        val result = SendHeartbeatUseCase(telemetry, repository)()

        assertEquals(null, repository.sent.single().batteryLevel)
        assertEquals(DeviceHeartbeatFixtures.result(), result.getOrNull())
    }
}
