package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.model.PairedDevice
import com.qhathia.mobile.discovery.core.domain.repository.DevicePairingRepository
import javax.inject.Inject

/** Exchanges an approved session for the persisted DEVICE token, returning the paired identity. */
class ExchangeDeviceTokenUseCase @Inject constructor(
    private val repository: DevicePairingRepository,
) {
    suspend operator fun invoke(sessionId: String): Result<PairedDevice> =
        repository.exchangeForDeviceToken(sessionId)
}
