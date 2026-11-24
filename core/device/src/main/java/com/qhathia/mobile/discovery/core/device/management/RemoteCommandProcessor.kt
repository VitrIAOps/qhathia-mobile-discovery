package com.qhathia.mobile.discovery.core.device.management

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Duration as JavaDuration

/**
 * Runs the commands the backend delivers and acknowledges each one. The restart command is handled
 * here because it spans two processes: the id is stored, the app restarts, and the relaunched process
 * acknowledges it after its first report. Every other command goes to its entry in [handlers];
 * a type with no handler is acknowledged as unsupported.
 */
class RemoteCommandProcessor(
    private val source: DeviceCommandSource,
    private val handlers: Map<String, suspend () -> Unit>,
    private val restarter: DeviceRestarter,
    private val pendingRestart: PendingRestartStore,
    private val clock: Clock,
    private val config: DeviceManagementConfig,
) {
    private var relaunchedFrom: PendingRestart? = null

    /** Runs until cancelled. */
    suspend fun run() = coroutineScope {
        val pending = pendingRestart.get()
        relaunchedFrom = pending
        if (pending != null) launch { ackRestartAfterRelaunch(pending) }
        // collect (not collectLatest): a running command is never cancelled by the next one.
        source.commands.collect { command -> process(command) }
    }

    private suspend fun ackRestartAfterRelaunch(pending: PendingRestart) {
        source.awaitNextReport()
        pendingRestart.clear()
        if (isFresh(pending)) ack(pending.commandId, success = true)
    }

    private fun isFresh(pending: PendingRestart): Boolean {
        val age = JavaDuration.between(pending.savedAt, clock.instant())
        return !age.isNegative && age.toMillis() <= config.pendingRestartMaxAge.inWholeMilliseconds
    }

    private suspend fun process(command: RemoteCommand) {
        try {
            val handler = handlers[command.type]
            when {
                command.type == config.restartCommandType -> restart(command.id)
                handler != null -> {
                    handler()
                    ack(command.id, success = true)
                }
                else -> ack(command.id, success = false, detail = config.unsupportedCommandDetail.format(command.type))
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            ack(command.id, success = false, detail = error.message ?: error::class.simpleName)
        }
    }

    private suspend fun restart(commandId: String) {
        // The same restart delivered again to the relaunched app: it already happened, never loop.
        if (relaunchedFrom?.commandId == commandId) return
        if (!restarter.canRestart()) {
            ack(commandId, success = false, detail = config.restartUnavailableDetail)
            return
        }
        // No ack yet: the relaunched process acks it (ackRestartAfterRelaunch).
        pendingRestart.save(PendingRestart(commandId, clock.instant()))
        restarter.restart()
    }

    private suspend fun ack(commandId: String, success: Boolean, detail: String? = null) {
        val trimmedDetail = detail?.take(config.ackDetailMaxLength)
        repeat(config.ackMaxAttempts) { attempt ->
            if (source.acknowledge(commandId, success, trimmedDetail).isSuccess) return
            if (attempt < config.ackMaxAttempts - 1) delay(config.ackRetryDelay)
        }
    }
}
