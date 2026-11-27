package com.qhathia.mobile.discovery.core.device.management

import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow

/** Generic test configuration, deliberately unrelated to any host app's values. */
internal val TestConfig = DeviceManagementConfig(
    heartbeatInterval = 60.seconds,
    heartbeatRetryDelays = listOf(1.seconds, 3.seconds),
    ackMaxAttempts = 3,
    ackRetryDelay = 2.seconds,
    ackDetailMaxLength = 10,
    pendingRestartMaxAge = 5.minutes,
    restartCommandType = "RESTART",
    unsupportedCommandDetail = "no %s",
    restartUnavailableDetail = "cannot restart",
)

internal class FakeDeviceReporter : DeviceReporter {
    override val isActive = MutableStateFlow(true)
    override val internetAvailable = MutableStateFlow(true)
    var reports = 0
    var result: () -> Result<Unit> = { Result.success(Unit) }

    override suspend fun report(): Result<Unit> {
        reports++
        return result()
    }
}

internal class FakeDeviceCommandSource : DeviceCommandSource {
    data class Ack(val commandId: String, val success: Boolean, val detail: String?)

    private val delivered = Channel<RemoteCommand>(Channel.UNLIMITED)
    override val commands: Flow<RemoteCommand> = delivered.receiveAsFlow()
    val reported = MutableSharedFlow<Unit>()
    val acks = mutableListOf<Ack>()
    var ackResults: ArrayDeque<Result<Unit>> = ArrayDeque()

    fun deliver(vararg commands: RemoteCommand) = commands.forEach { delivered.trySend(it) }

    override suspend fun awaitNextReport() {
        reported.first()
    }

    override suspend fun acknowledge(commandId: String, success: Boolean, detail: String?): Result<Unit> {
        acks += Ack(commandId, success, detail)
        return ackResults.removeFirstOrNull() ?: Result.success(Unit)
    }
}

internal class FakeDeviceRestarter : DeviceRestarter {
    var available = true
    var restarts = 0
    var onRestart: () -> Unit = {}

    override fun canRestart() = available
    override fun restart() {
        onRestart()
        restarts++
    }
}

internal class FakePendingRestartStore(var stored: PendingRestart? = null) : PendingRestartStore {
    override suspend fun save(restart: PendingRestart) {
        stored = restart
    }
    override suspend fun get() = stored
    override suspend fun clear() {
        stored = null
    }
}

internal class FakeRemoteSettingsSource(var stored: Long? = null) : RemoteSettingsSource {
    override val reportedVersions = MutableSharedFlow<Long?>()
    var refreshes = 0
    var refreshSucceeds = true

    override suspend fun storedVersion() = stored
    override suspend fun refresh() {
        refreshes++
        if (refreshSucceeds) stored = pendingVersion
    }

    /** The version a successful refresh stores. */
    var pendingVersion: Long? = null
}
