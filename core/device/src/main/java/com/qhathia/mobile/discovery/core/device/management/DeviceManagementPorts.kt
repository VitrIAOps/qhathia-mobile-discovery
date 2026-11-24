package com.qhathia.mobile.discovery.core.device.management

import kotlinx.coroutines.flow.Flow
import java.time.Instant
import kotlin.time.Duration

/** How the device management engines pace themselves and what they report. */
data class DeviceManagementConfig(
    val heartbeatInterval: Duration,
    val heartbeatRetryDelays: List<Duration>,
    val ackMaxAttempts: Int,
    val ackRetryDelay: Duration,
    val ackDetailMaxLength: Int,
    val pendingRestartMaxAge: Duration,
    /** The remote command type that restarts the app. */
    val restartCommandType: String,
    /** Acknowledgement detail for an unknown command; `%s` is its type. */
    val unsupportedCommandDetail: String,
    val restartUnavailableDetail: String,
)

/** Sends the device's periodic report to whatever backend the host app uses. */
interface DeviceReporter {
    /** Whether the device should be reporting at all. */
    val isActive: Flow<Boolean>

    /** Whether the device has internet; when it comes back, the next report goes out at once. */
    val internetAvailable: Flow<Boolean>

    suspend fun report(): Result<Unit>
}

/** A command the backend asked this device to run. [type] is the backend's raw name. */
data class RemoteCommand(val id: String, val type: String)

/** Where remote commands come from and where their outcome goes. */
interface DeviceCommandSource {
    val commands: Flow<RemoteCommand>

    /** Suspends until the next successful report. */
    suspend fun awaitNextReport()

    suspend fun acknowledge(commandId: String, success: Boolean, detail: String?): Result<Unit>
}

/** Restarts the host app. */
interface DeviceRestarter {
    fun canRestart(): Boolean
    fun restart()
}

/** A restart command waiting to be acknowledged by the relaunched process. */
data class PendingRestart(val commandId: String, val savedAt: Instant)

/** Persists a [PendingRestart] across the process restart. */
interface PendingRestartStore {
    suspend fun save(restart: PendingRestart)
    suspend fun get(): PendingRestart?
    suspend fun clear()
}

/** The remote settings the host app keeps, identified by version. */
interface RemoteSettingsSource {
    /** The latest version the backend reported on each report; `null` when it sent none. */
    val reportedVersions: Flow<Long?>

    suspend fun storedVersion(): Long?

    /** Reads the settings again; a failure leaves the stored version unchanged. */
    suspend fun refresh()
}
