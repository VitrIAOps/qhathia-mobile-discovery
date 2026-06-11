package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.model.PairingStatus
import com.qhathia.mobile.discovery.core.domain.repository.DevicePairingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Streams pairing status for the active session while the screen waits for approval. */
class ObservePairingStatusUseCase @Inject constructor(
    private val repository: DevicePairingRepository,
) {
    operator fun invoke(sessionId: String): Flow<PairingStatus> =
        repository.observeStatus(sessionId)
}
