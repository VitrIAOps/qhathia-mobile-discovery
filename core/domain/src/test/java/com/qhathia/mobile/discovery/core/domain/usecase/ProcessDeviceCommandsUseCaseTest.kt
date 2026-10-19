package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.model.DeviceCommand
import com.qhathia.mobile.discovery.core.domain.model.DeviceCommandType
import com.qhathia.mobile.discovery.core.domain.repository.CatalogRefreshSignal
import com.qhathia.mobile.discovery.core.testing.DeviceHeartbeatFixtures
import com.qhathia.mobile.discovery.core.testing.FakeAppRestarter
import com.qhathia.mobile.discovery.core.testing.FakeCatalogRefreshSignal
import com.qhathia.mobile.discovery.core.testing.FakeDeviceCommandRepository
import com.qhathia.mobile.discovery.core.testing.FakeDeviceHeartbeatRepository
import com.qhathia.mobile.discovery.core.testing.FakeDeviceCommandRepository.Ack
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
class ProcessDeviceCommandsUseCaseTest {

    private val heartbeat = FakeDeviceHeartbeatRepository()
    private val commands = FakeDeviceCommandRepository()
    private val refresh = FakeCatalogRefreshSignal()
    private val restarter = FakeAppRestarter()
    private val retryDelay = 2.seconds

    private fun TestScope.start(signal: CatalogRefreshSignal = refresh) {
        val useCase = ProcessDeviceCommandsUseCase(heartbeat, commands, signal, restarter, retryDelay)
        backgroundScope.launch { useCase() }
        runCurrent()
    }

    private suspend fun TestScope.deliver(vararg delivered: DeviceCommand) {
        heartbeat.nextResult = Result.success(DeviceHeartbeatFixtures.result(pendingCommands = delivered.toList()))
        heartbeat.sendHeartbeat(DeviceHeartbeatFixtures.telemetry())
        runCurrent()
    }

    @Test
    fun refresh_catalog_requests_refresh_and_acks_success() = runTest {
        start()

        deliver(DeviceCommand("c1", DeviceCommandType.RefreshCatalog))

        assertEquals(1, refresh.requestCount)
        assertEquals(listOf(Ack("c1", true, null)), commands.acks)
    }

    @Test
    fun restart_acks_before_restarting() = runTest {
        var acksAtRestart = -1
        restarter.onRestart = { acksAtRestart = commands.acks.size }
        start()

        deliver(DeviceCommand("r1", DeviceCommandType.RestartApp))

        assertEquals(1, restarter.restartCount)
        assertEquals(1, acksAtRestart)
        assertEquals(listOf(Ack("r1", true, null)), commands.acks)
    }

    @Test
    fun restart_waits_for_ack_retries() = runTest {
        commands.failuresBeforeSuccess = 3
        start()

        deliver(DeviceCommand("r1", DeviceCommandType.RestartApp))
        assertEquals(0, restarter.restartCount)

        advanceTimeBy(retryDelay.inWholeMilliseconds * 2)
        runCurrent()

        assertEquals(3, commands.acks.size)
        assertEquals(1, restarter.restartCount)
    }

    @Test
    fun restart_unavailable_acks_failure_without_restarting() = runTest {
        restarter.available = false
        start()

        deliver(DeviceCommand("r1", DeviceCommandType.RestartApp))

        assertEquals(0, restarter.restartCount)
        assertEquals(listOf(Ack("r1", false, "No se pudo reiniciar la app")), commands.acks)
    }

    @Test
    fun unknown_command_acks_unsupported() = runTest {
        start()

        deliver(DeviceCommand("u1", DeviceCommandType.Unknown("WIPE_DEVICE")))

        assertEquals(listOf(Ack("u1", false, "Comando no soportado: WIPE_DEVICE")), commands.acks)
    }

    @Test
    fun failing_execution_acks_failure_and_next_command_still_runs() = runTest {
        val throwing = object : CatalogRefreshSignal {
            override fun request() = throw IllegalStateException("catalog unavailable")
            override fun observe(): Flow<Unit> = emptyFlow()
        }
        start(throwing)

        deliver(
            DeviceCommand("c1", DeviceCommandType.RefreshCatalog),
            DeviceCommand("u1", DeviceCommandType.Unknown("X")),
        )

        assertEquals(
            listOf(Ack("c1", false, "catalog unavailable"), Ack("u1", false, "Comando no soportado: X")),
            commands.acks,
        )
    }

    @Test
    fun ack_is_retried_two_seconds_apart_up_to_three_attempts() = runTest {
        commands.failuresBeforeSuccess = 10
        start()

        deliver(DeviceCommand("c1", DeviceCommandType.RefreshCatalog))
        assertEquals(1, commands.acks.size)

        advanceTimeBy(retryDelay.inWholeMilliseconds)
        runCurrent()
        assertEquals(2, commands.acks.size)

        advanceTimeBy(retryDelay.inWholeMilliseconds * 10)
        runCurrent()
        assertEquals(3, commands.acks.size)
    }

    @Test
    fun ack_stops_retrying_after_success() = runTest {
        commands.failuresBeforeSuccess = 1
        start()

        deliver(DeviceCommand("c1", DeviceCommandType.RefreshCatalog))
        advanceTimeBy(retryDelay.inWholeMilliseconds * 10)
        runCurrent()

        assertEquals(2, commands.acks.size)
    }

    @Test
    fun long_detail_is_truncated_to_300_chars() = runTest {
        start()

        deliver(DeviceCommand("u1", DeviceCommandType.Unknown("X".repeat(400))))

        assertEquals(300, commands.acks.single().detail!!.length)
    }

    @Test
    fun commands_are_processed_in_order() = runTest {
        start()

        deliver(
            DeviceCommand("c1", DeviceCommandType.RefreshCatalog),
            DeviceCommand("r1", DeviceCommandType.RestartApp),
        )

        assertEquals(listOf("c1", "r1"), commands.acks.map { it.commandId })
        assertEquals(1, restarter.restartCount)
    }
}
