package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.repository.DevicePairingRepository
import javax.inject.Inject

/**
 * Cancels the pairing code a previous app process left on screen (F-02), so it can no longer be
 * approved. Run once when the app starts: a new process never reuses that code.
 */
class CancelAbandonedPairingUseCase @Inject constructor(
    private val repository: DevicePairingRepository,
) {
    suspend operator fun invoke(): Result<Unit> = repository.cancelAbandonedSession()
}
