package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.DeviceConstants
import com.qhathia.mobile.discovery.core.domain.model.DeviceCommand
import com.qhathia.mobile.discovery.core.domain.model.DeviceCommandType
import com.qhathia.mobile.discovery.core.domain.model.PendingRestartAck
import com.qhathia.mobile.discovery.core.domain.repository.AppRestarter
import com.qhathia.mobile.discovery.core.domain.repository.CatalogRefreshSignal
import com.qhathia.mobile.discovery.core.domain.repository.DeviceCommandRepository
import com.qhathia.mobile.discovery.core.domain.repository.DeviceHeartbeatRepository
import com.qhathia.mobile.discovery.core.domain.repository.PendingRestartAckRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Duration as JavaDuration
import javax.inject.Inject
import kotlin.time.Duration

/**
 * Executes the remote commands delivered by the heartbeat, one at a time and in order, and
 * acknowledges each one. Suspends until cancelled.
 *
 * - `REFRESH_CATALOG` → asks open catalog screens to reload, acked as done.
 * - `RESTART_APP` → its id is stored, then the app relaunches. The ack is sent by the next process,
 *   after its first heartbeat, so `success = true` means the app really came back (design D7). A
 *   stored id older than [DeviceConstants.PENDING_RESTART_ACK_MAX_AGE] is dropped without an ack.
 * - anything else → acked as failed "Comando no soportado".
 *
 * A failed ack is retried a few times and then dropped: the server expires the command, which is
 * the honest outcome when the tablet cannot reach it.
 */
class ProcessDeviceCommandsUseCase(
    private val heartbeatRepository: DeviceHeartbeatRepository,
    private val commandRepository: DeviceCommandRepository,
    private val catalogRefreshSignal: CatalogRefreshSignal,
    private val appRestarter: AppRestarter,
    private val pendingRestartAck: PendingRestartAckRepository,
    private val clock: Clock,
    private val ackRetryDelay: Duration,
) {
    @Inject
    constructor(
        heartbeatRepository: DeviceHeartbeatRepository,
        commandRepository: DeviceCommandRepository,
        catalogRefreshSignal: CatalogRefreshSignal,
        appRestarter: AppRestarter,
        pendingRestartAck: PendingRestartAckRepository,
        clock: Clock,
    ) : this(
        heartbeatRepository,
        commandRepository,
        catalogRefreshSignal,
        appRestarter,
        pendingRestartAck,
        clock,
        DeviceConstants.ACK_RETRY_DELAY,
    )

    /** The restart the previous process carried out, read once at start. */
    private var relaunchedFrom: PendingRestartAck? = null

    suspend operator fun invoke() = coroutineScope {
        val pending = pendingRestartAck.get()
        relaunchedFrom = pending
        if (pending != null) launch { ackRestartAfterRelaunch(pending) }
        // collect (not collectLatest): a running command is never cancelled by the next one.
        heartbeatRepository.observePendingCommands().collect { command -> process(command) }
    }

    /** Acks the restart stored by the previous process once this one has sent a heartbeat. */
    private suspend fun ackRestartAfterRelaunch(pending: PendingRestartAck) {
        heartbeatRepository.observeHeartbeats().first()
        pendingRestartAck.clear()
        if (isFresh(pending)) ack(pending.commandId, success = true)
    }

    private fun isFresh(pending: PendingRestartAck): Boolean {
        val age = JavaDuration.between(pending.savedAt, clock.instant())
        return !age.isNegative && age.toMillis() <= DeviceConstants.PENDING_RESTART_ACK_MAX_AGE.inWholeMilliseconds
    }

    private suspend fun process(command: DeviceCommand) {
        try {
            when (val type = command.type) {
                DeviceCommandType.RefreshCatalog -> {
                    catalogRefreshSignal.request()
                    ack(command.id, success = true)
                }
                DeviceCommandType.RestartApp -> restart(command.id)
                is DeviceCommandType.Unknown ->
                    ack(command.id, success = false, detail = DeviceConstants.ACK_DETAIL_UNSUPPORTED.format(type.raw))
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
        if (!appRestarter.canRestart()) {
            ack(commandId, success = false, detail = DeviceConstants.ACK_DETAIL_RESTART_UNAVAILABLE)
            return
        }
        // No ack yet: the relaunched process acks it (ackRestartAfterRelaunch).
        pendingRestartAck.save(PendingRestartAck(commandId, clock.instant()))
        appRestarter.restart()
    }

    private suspend fun ack(commandId: String, success: Boolean, detail: String? = null) {
        val trimmedDetail = detail?.take(DeviceConstants.ACK_DETAIL_MAX_LENGTH)
        repeat(DeviceConstants.ACK_MAX_ATTEMPTS) { attempt ->
            if (commandRepository.ack(commandId, success, trimmedDetail).isSuccess) return
            if (attempt < DeviceConstants.ACK_MAX_ATTEMPTS - 1) delay(ackRetryDelay)
        }
    }
}
