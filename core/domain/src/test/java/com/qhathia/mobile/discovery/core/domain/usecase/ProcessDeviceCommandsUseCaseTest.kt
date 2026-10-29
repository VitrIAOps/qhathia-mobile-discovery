package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.model.DeviceCommand
import com.qhathia.mobile.discovery.core.domain.model.DeviceCommandType
import com.qhathia.mobile.discovery.core.domain.model.PendingRestartAck
import com.qhathia.mobile.discovery.core.domain.repository.CatalogRefreshSignal
import com.qhathia.mobile.discovery.core.testing.DeviceHeartbeatFixtures
import com.qhathia.mobile.discovery.core.testing.FakeAppRestarter
import com.qhathia.mobile.discovery.core.testing.FakeCatalogRefreshSignal
import com.qhathia.mobile.discovery.core.testing.FakeDeviceCommandRepository
import com.qhathia.mobile.discovery.core.testing.FakeDeviceHeartbeatRepository
import com.qhathia.mobile.discovery.core.testing.FakePendingRestartAckRepository
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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class ProcessDeviceCommandsUseCaseTest {

    private val heartbeat = FakeDeviceHeartbeatRepository()
    private val commands = FakeDeviceCommandRepository()
    private val refresh = FakeCatalogRefreshSignal()
    private val restarter = FakeAppRestarter()
    private val pending = FakePendingRestartAckRepository()
    private val now = Instant.parse("2026-10-29T12:00:00Z")
    private var clock: Clock = Clock.fixed(now, ZoneOffset.UTC)
    private val retryDelay = 2.seconds

    private fun TestScope.start(signal: CatalogRefreshSignal = refresh) {
        val useCase = ProcessDeviceCommandsUseCase(
            heartbeat, commands, signal, restarter, pending, clock, retryDelay,
        )
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
    fun restart_stores_the_id_and_sends_no_ack_before_restarting() = runTest {
        var storedAtRestart: PendingRestartAck? = null
        var acksAtRestart = -1
        restarter.onRestart = {
            storedAtRestart = pending.stored
            acksAtRestart = commands.acks.size
        }
        start()

        deliver(DeviceCommand("r1", DeviceCommandType.RestartApp))

        assertEquals(1, restarter.restartCount)
        assertEquals(PendingRestartAck("r1", now), storedAtRestart)
        assertEquals(0, acksAtRestart)
        assertEquals(emptyList<Ack>(), commands.acks)
    }

    @Test
    fun pending_restart_is_acked_after_the_first_heartbeat_of_the_new_process() = runTest {
        pending.stored = PendingRestartAck("r1", now.minusSeconds(20))
        start()
        assertEquals(emptyList<Ack>(), commands.acks)

        deliver()

        assertEquals(listOf(Ack("r1", true, null)), commands.acks)
        assertNull(pending.stored)
    }

    @Test
    fun stale_pending_restart_is_dropped_without_ack() = runTest {
        pending.stored = PendingRestartAck("r1", now.minus(Duration.ofMinutes(11)))
        start()

        deliver()

        assertEquals(emptyList<Ack>(), commands.acks)
        assertNull(pending.stored)
    }

    @Test
    fun pending_restart_from_the_future_is_dropped_without_ack() = runTest {
        // The clock moved back after the restart: the age cannot be trusted.
        pending.stored = PendingRestartAck("r1", now.plusSeconds(3_600))
        start()

        deliver()

        assertEquals(emptyList<Ack>(), commands.acks)
    }

    @Test
    fun same_restart_delivered_again_after_relaunch_does_not_restart_again() = runTest {
        pending.stored = PendingRestartAck("r1", now.minusSeconds(20))
        start()

        deliver(DeviceCommand("r1", DeviceCommandType.RestartApp))

        assertEquals(0, restarter.restartCount)
        assertEquals(listOf(Ack("r1", true, null)), commands.acks)
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

        // The restart is acked by the next process; only the refresh is acked here.
        assertEquals(listOf("c1"), commands.acks.map { it.commandId })
        assertEquals(1, restarter.restartCount)
    }
}
