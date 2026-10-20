package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.DeviceConstants
import com.qhathia.mobile.discovery.core.domain.model.DeviceCommand
import com.qhathia.mobile.discovery.core.domain.model.DeviceCommandType
import com.qhathia.mobile.discovery.core.domain.repository.AppRestarter
import com.qhathia.mobile.discovery.core.domain.repository.CatalogRefreshSignal
import com.qhathia.mobile.discovery.core.domain.repository.DeviceCommandRepository
import com.qhathia.mobile.discovery.core.domain.repository.DeviceHeartbeatRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import javax.inject.Inject
import kotlin.time.Duration

/**
 * Executes the remote commands delivered by the heartbeat, one at a time and in order, and
 * acknowledges each one. Suspends until cancelled.
 *
 * - `REFRESH_CATALOG` → asks open catalog screens to reload, acked as done.
 * - `RESTART_APP` → acked first (the process is about to die), then the app relaunches.
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
    private val ackRetryDelay: Duration,
) {
    @Inject
    constructor(
        heartbeatRepository: DeviceHeartbeatRepository,
        commandRepository: DeviceCommandRepository,
        catalogRefreshSignal: CatalogRefreshSignal,
        appRestarter: AppRestarter,
    ) : this(heartbeatRepository, commandRepository, catalogRefreshSignal, appRestarter, DeviceConstants.ACK_RETRY_DELAY)

    suspend operator fun invoke() {
        // collect (not collectLatest): a running command is never cancelled by the next one.
        heartbeatRepository.observePendingCommands().collect { command -> process(command) }
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
        if (!appRestarter.canRestart()) {
            ack(commandId, success = false, detail = DeviceConstants.ACK_DETAIL_RESTART_UNAVAILABLE)
            return
        }
        // Ack before restarting: once the process exits nothing more can be reported.
        ack(commandId, success = true)
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
