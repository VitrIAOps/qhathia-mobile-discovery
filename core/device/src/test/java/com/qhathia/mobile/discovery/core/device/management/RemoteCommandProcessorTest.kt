package com.qhathia.mobile.discovery.core.device.management

import com.qhathia.mobile.discovery.core.device.management.FakeDeviceCommandSource.Ack
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RemoteCommandProcessorTest {

    private val source = FakeDeviceCommandSource()
    private val restarter = FakeDeviceRestarter()
    private val pending = FakePendingRestartStore()
    private val now = Instant.parse("2026-11-24T12:00:00Z")
    private val handled = mutableListOf<String>()
    private var handlers: Map<String, suspend () -> Unit> = mapOf("PING" to { handled += "PING" })

    private fun TestScope.start() {
        val processor = RemoteCommandProcessor(
            source, handlers, restarter, pending, Clock.fixed(now, ZoneOffset.UTC), TestConfig,
        )
        backgroundScope.launch { processor.run() }
        runCurrent()
    }

    private fun TestScope.deliver(vararg commands: RemoteCommand) {
        source.deliver(*commands)
        runCurrent()
    }

    @Test
    fun handled_command_runs_and_acks_success() = runTest {
        start()
        deliver(RemoteCommand("c1", "PING"))
        assertEquals(listOf("PING"), handled)
        assertEquals(listOf(Ack("c1", true, null)), source.acks)
    }

    @Test
    fun restart_saves_the_id_before_restarting_and_sends_no_ack() = runTest {
        var savedAtRestart: PendingRestart? = null
        restarter.onRestart = { savedAtRestart = pending.stored }
        start()
        deliver(RemoteCommand("r1", "RESTART"))
        assertEquals(1, restarter.restarts)
        assertEquals("r1", savedAtRestart?.commandId)
        assertEquals(emptyList<Ack>(), source.acks)
    }

    @Test
    fun relaunched_process_acks_the_restart_after_its_first_report() = runTest {
        pending.stored = PendingRestart("r1", now.minusSeconds(30))
        start()
        assertEquals(emptyList<Ack>(), source.acks)
        source.reported.emit(Unit)
        runCurrent()
        assertEquals(listOf(Ack("r1", true, null)), source.acks)
        assertNull(pending.stored)
    }

    @Test
    fun stale_or_future_pending_restart_is_dropped_without_ack() = runTest {
        pending.stored = PendingRestart("old", now.minusSeconds(6.minutes.inWholeSeconds))
        start()
        source.reported.emit(Unit)
        runCurrent()
        assertEquals(emptyList<Ack>(), source.acks)
        assertNull(pending.stored)
    }

    @Test
    fun same_restart_delivered_after_relaunch_does_not_restart_again() = runTest {
        pending.stored = PendingRestart("r1", now)
        start()
        deliver(RemoteCommand("r1", "RESTART"))
        assertEquals(0, restarter.restarts)
    }

    @Test
    fun restart_unavailable_acks_failure() = runTest {
        restarter.available = false
        start()
        deliver(RemoteCommand("r1", "RESTART"))
        assertEquals(0, restarter.restarts)
        assertEquals(listOf(Ack("r1", false, "cannot res")), source.acks)
    }

    @Test
    fun unknown_command_acks_unsupported() = runTest {
        start()
        deliver(RemoteCommand("u1", "DANCE"))
        assertEquals(listOf(Ack("u1", false, "no DANCE")), source.acks)
    }

    @Test
    fun failing_handler_acks_failure_and_the_next_command_runs() = runTest {
        handlers = mapOf("BAD" to { error("broken") }, "PING" to { handled += "PING" })
        start()
        deliver(RemoteCommand("b1", "BAD"), RemoteCommand("p1", "PING"))
        assertEquals(listOf(Ack("b1", false, "broken"), Ack("p1", true, null)), source.acks)
    }

    @Test
    fun ack_is_retried_up_to_the_max_attempts() = runTest {
        source.ackResults = ArrayDeque(List(5) { Result.failure(IllegalStateException()) })
        start()
        deliver(RemoteCommand("c1", "PING"))
        assertEquals(1, source.acks.size)
        advanceTimeBy(2.seconds)
        runCurrent()
        assertEquals(2, source.acks.size)
        advanceTimeBy(10.seconds)
        runCurrent()
        assertEquals(3, source.acks.size)
    }

    @Test
    fun ack_stops_retrying_after_success() = runTest {
        source.ackResults = ArrayDeque(listOf(Result.failure(IllegalStateException()), Result.success(Unit)))
        start()
        deliver(RemoteCommand("c1", "PING"))
        advanceTimeBy(30.seconds)
        runCurrent()
        assertEquals(2, source.acks.size)
    }

    @Test
    fun commands_run_in_delivery_order() = runTest {
        handlers = mapOf("A" to { handled += "A" }, "B" to { handled += "B" })
        start()
        deliver(RemoteCommand("1", "B"), RemoteCommand("2", "A"), RemoteCommand("3", "B"))
        assertEquals(listOf("B", "A", "B"), handled)
    }
}
