package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.DeviceConstants
import com.qhathia.mobile.discovery.core.domain.repository.NetworkSettingsRepository
import com.qhathia.mobile.discovery.core.domain.repository.StoreSessionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import kotlin.time.Duration

/**
 * Runs the heartbeat for the lifetime of the calling coroutine: while the device is paired it sends
 * a heartbeat immediately and then every [interval] after a successful one. Becoming unpaired cancels
 * the loop; pairing again restarts it with an immediate send.
 *
 * A failed heartbeat (returned or thrown) is retried sooner, after each of [retryDelays] in turn and
 * then every [interval] (F-24). Whatever the wait, the next heartbeat goes out as soon as the device
 * regains internet, so settings and commands queued while offline arrive within seconds. Suspends
 * until cancelled.
 */
class RunDeviceHeartbeatUseCase(
    private val storeSessionRepository: StoreSessionRepository,
    private val sendHeartbeat: SendHeartbeatUseCase,
    private val networkSettingsRepository: NetworkSettingsRepository,
    private val interval: Duration,
    private val retryDelays: List<Duration>,
) {
    @Inject
    constructor(
        storeSessionRepository: StoreSessionRepository,
        sendHeartbeat: SendHeartbeatUseCase,
        networkSettingsRepository: NetworkSettingsRepository,
    ) : this(
        storeSessionRepository,
        sendHeartbeat,
        networkSettingsRepository,
        DeviceConstants.HEARTBEAT_INTERVAL,
        DeviceConstants.HEARTBEAT_RETRY_DELAYS,
    )

    suspend operator fun invoke() {
        storeSessionRepository.observe()
            .map { it.isPaired }
            .distinctUntilChanged()
            .collectLatest { paired -> if (paired) beatUntilCancelled() }
    }

    private suspend fun beatUntilCancelled() {
        var failures = 0
        while (true) {
            failures = if (beatOnce()) 0 else failures + 1
            val wait = if (failures == 0) interval else retryDelays.getOrElse(failures - 1) { interval }
            withTimeoutOrNull(wait) { awaitInternetBack() }
        }
    }

    /** Sends one heartbeat; `true` on success. Cancellation is rethrown so unpairing stops the loop. */
    private suspend fun beatOnce(): Boolean = try {
        sendHeartbeat().isSuccess
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Exception) {
        false // the heartbeat must never crash the app
    }

    /** Returns when the device goes from no internet to internet. */
    private suspend fun awaitInternetBack() {
        networkSettingsRepository.observeConnectivity()
            .map { it.hasInternet }
            .distinctUntilChanged()
            .drop(1)
            .first { it }
    }
}
