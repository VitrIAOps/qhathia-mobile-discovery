package com.qhathia.mobile.discovery.core.data.management

import com.qhathia.mobile.discovery.core.data.heartbeat.COMMAND_REFRESH_CATALOG
import com.qhathia.mobile.discovery.core.data.heartbeat.COMMAND_RESTART_APP
import com.qhathia.mobile.discovery.core.device.management.DeviceCommandSource
import com.qhathia.mobile.discovery.core.device.management.DeviceManagementConfig
import com.qhathia.mobile.discovery.core.device.management.DeviceReporter
import com.qhathia.mobile.discovery.core.device.management.DeviceRestarter
import com.qhathia.mobile.discovery.core.device.management.PendingRestart
import com.qhathia.mobile.discovery.core.device.management.PendingRestartStore
import com.qhathia.mobile.discovery.core.device.management.RemoteCommand
import com.qhathia.mobile.discovery.core.device.management.RemoteCommandProcessor
import com.qhathia.mobile.discovery.core.device.management.RemoteSettingsSource
import com.qhathia.mobile.discovery.core.domain.DeviceConstants
import com.qhathia.mobile.discovery.core.domain.model.DeviceCommandType
import com.qhathia.mobile.discovery.core.domain.model.PendingRestartAck
import com.qhathia.mobile.discovery.core.domain.repository.AppRestarter
import com.qhathia.mobile.discovery.core.domain.repository.CatalogRefreshSignal
import com.qhathia.mobile.discovery.core.domain.repository.DeviceCommandRepository
import com.qhathia.mobile.discovery.core.domain.repository.DeviceHeartbeatRepository
import com.qhathia.mobile.discovery.core.domain.repository.DeviceSettingsRepository
import com.qhathia.mobile.discovery.core.domain.repository.NetworkSettingsRepository
import com.qhathia.mobile.discovery.core.domain.repository.PendingRestartAckRepository
import com.qhathia.mobile.discovery.core.domain.repository.StoreSessionRepository
import com.qhathia.mobile.discovery.core.domain.usecase.ObserveHeartbeatUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.SendHeartbeatUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Clock
import javax.inject.Inject

/** Discovery's pacing and texts for the `core:device` management engines. */
internal val DiscoveryDeviceManagementConfig = DeviceManagementConfig(
    heartbeatInterval = DeviceConstants.HEARTBEAT_INTERVAL,
    heartbeatRetryDelays = DeviceConstants.HEARTBEAT_RETRY_DELAYS,
    ackMaxAttempts = DeviceConstants.ACK_MAX_ATTEMPTS,
    ackRetryDelay = DeviceConstants.ACK_RETRY_DELAY,
    ackDetailMaxLength = DeviceConstants.ACK_DETAIL_MAX_LENGTH,
    pendingRestartMaxAge = DeviceConstants.PENDING_RESTART_ACK_MAX_AGE,
    restartCommandType = COMMAND_RESTART_APP,
    unsupportedCommandDetail = DeviceConstants.ACK_DETAIL_UNSUPPORTED,
    restartUnavailableDetail = DeviceConstants.ACK_DETAIL_RESTART_UNAVAILABLE,
)

/** Reports while the tablet is paired, through the heartbeat API. */
internal class HeartbeatDeviceReporter @Inject constructor(
    storeSessionRepository: StoreSessionRepository,
    networkSettingsRepository: NetworkSettingsRepository,
    private val sendHeartbeat: SendHeartbeatUseCase,
) : DeviceReporter {
    override val isActive: Flow<Boolean> = storeSessionRepository.observe().map { it.isPaired }
    override val internetAvailable: Flow<Boolean> = networkSettingsRepository.observeConnectivity().map { it.hasInternet }
    override suspend fun report(): Result<Unit> = sendHeartbeat().map { }
}

/** Commands arrive in heartbeat responses and are acknowledged through the commands API. */
internal class BackendDeviceCommandSource @Inject constructor(
    private val heartbeatRepository: DeviceHeartbeatRepository,
    private val commandRepository: DeviceCommandRepository,
) : DeviceCommandSource {
    override val commands: Flow<RemoteCommand> =
        heartbeatRepository.observePendingCommands().map { RemoteCommand(it.id, it.type.rawName()) }

    override suspend fun awaitNextReport() {
        heartbeatRepository.observeHeartbeats().first()
    }

    override suspend fun acknowledge(commandId: String, success: Boolean, detail: String?): Result<Unit> =
        commandRepository.ack(commandId, success, detail)

    private fun DeviceCommandType.rawName(): String = when (this) {
        DeviceCommandType.RefreshCatalog -> COMMAND_REFRESH_CATALOG
        DeviceCommandType.RestartApp -> COMMAND_RESTART_APP
        is DeviceCommandType.Unknown -> raw
    }
}

internal class AppDeviceRestarter @Inject constructor(
    private val appRestarter: AppRestarter,
) : DeviceRestarter {
    override fun canRestart(): Boolean = appRestarter.canRestart()
    override fun restart() = appRestarter.restart()
}

internal class PendingRestartAckStore @Inject constructor(
    private val repository: PendingRestartAckRepository,
) : PendingRestartStore {
    override suspend fun save(restart: PendingRestart) =
        repository.save(PendingRestartAck(restart.commandId, restart.savedAt))

    override suspend fun get(): PendingRestart? =
        repository.get()?.let { PendingRestart(it.commandId, it.savedAt) }

    override suspend fun clear() = repository.clear()
}

/** Settings versions arrive in heartbeats; the settings repository holds and refreshes them. */
internal class BackendRemoteSettingsSource @Inject constructor(
    observeHeartbeat: ObserveHeartbeatUseCase,
    private val repository: DeviceSettingsRepository,
) : RemoteSettingsSource {
    override val reportedVersions: Flow<Long?> = observeHeartbeat().map { it.settingsVersion }
    override suspend fun storedVersion(): Long? = repository.storedVersion()
    override suspend fun refresh() {
        repository.refresh()
    }
}

/** The command processor with Discovery's business command: "Actualizar catálogo". */
internal fun discoveryCommandProcessor(
    source: DeviceCommandSource,
    catalogRefreshSignal: CatalogRefreshSignal,
    restarter: DeviceRestarter,
    pendingRestart: PendingRestartStore,
    clock: Clock,
    config: DeviceManagementConfig = DiscoveryDeviceManagementConfig,
) = RemoteCommandProcessor(
    source = source,
    handlers = mapOf(COMMAND_REFRESH_CATALOG to suspend { catalogRefreshSignal.request() }),
    restarter = restarter,
    pendingRestart = pendingRestart,
    clock = clock,
    config = config,
)
