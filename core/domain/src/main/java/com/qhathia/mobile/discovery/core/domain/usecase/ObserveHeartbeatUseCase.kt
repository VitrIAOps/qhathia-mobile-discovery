package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.model.HeartbeatResult
import com.qhathia.mobile.discovery.core.domain.repository.DeviceHeartbeatRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Streams successful heartbeat results (a new collector first receives the latest one). */
class ObserveHeartbeatUseCase @Inject constructor(
    private val repository: DeviceHeartbeatRepository,
) {
    operator fun invoke(): Flow<HeartbeatResult> = repository.observeHeartbeats()
}
