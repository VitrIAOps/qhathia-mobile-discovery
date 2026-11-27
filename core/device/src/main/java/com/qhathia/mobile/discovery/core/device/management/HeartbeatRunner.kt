package com.qhathia.mobile.discovery.core.device.management

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Sends the periodic device report while [DeviceReporter.isActive]: one right away, then one per
 * interval. After a failure it retries on the backoff delays, and it reports at once when internet
 * comes back. A failed report never crashes the app.
 */
class HeartbeatRunner(
    private val reporter: DeviceReporter,
    private val config: DeviceManagementConfig,
) {
    /** Runs until cancelled. */
    suspend fun run() {
        reporter.isActive
            .distinctUntilChanged()
            .collectLatest { active -> if (active) beatUntilCancelled() }
    }

    private suspend fun beatUntilCancelled() {
        var failures = 0
        while (true) {
            failures = if (beatOnce()) 0 else failures + 1
            val wait = if (failures == 0) {
                config.heartbeatInterval
            } else {
                config.heartbeatRetryDelays.getOrElse(failures - 1) { config.heartbeatInterval }
            }
            withTimeoutOrNull(wait) { awaitInternetBack() }
        }
    }

    private suspend fun beatOnce(): Boolean = try {
        reporter.report().isSuccess
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Exception) {
        false // the heartbeat must never crash the app
    }

    private suspend fun awaitInternetBack() {
        reporter.internetAvailable
            .distinctUntilChanged()
            .drop(1)
            .first { it }
    }
}
