package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.DeviceConstants
import com.qhathia.mobile.discovery.core.domain.repository.StoreSessionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.time.Duration

/**
 * Runs the heartbeat for the lifetime of the calling coroutine: while the device is paired it sends
 * a heartbeat immediately and then every [interval] after each attempt. Becoming unpaired cancels the
 * loop; pairing again restarts it with an immediate send. A failed heartbeat is ignored and retried
 * on the next tick. Suspends until cancelled.
 */
class RunDeviceHeartbeatUseCase(
    private val storeSessionRepository: StoreSessionRepository,
    private val sendHeartbeat: SendHeartbeatUseCase,
    private val interval: Duration,
) {
    @Inject
    constructor(
        storeSessionRepository: StoreSessionRepository,
        sendHeartbeat: SendHeartbeatUseCase,
    ) : this(storeSessionRepository, sendHeartbeat, DeviceConstants.HEARTBEAT_INTERVAL)

    suspend operator fun invoke() {
        storeSessionRepository.observe()
            .map { it.isPaired }
            .distinctUntilChanged()
            .collectLatest { paired -> if (paired) beatUntilCancelled() }
    }

    private suspend fun beatUntilCancelled() {
        while (true) {
            // A failure (returned or thrown) is dropped: the next tick is the retry. Cancellation is
            // rethrown so unpairing or scope shutdown stops the loop.
            try {
                sendHeartbeat()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                // Ignored by design — the heartbeat must never crash the app.
            }
            delay(interval)
        }
    }
}
