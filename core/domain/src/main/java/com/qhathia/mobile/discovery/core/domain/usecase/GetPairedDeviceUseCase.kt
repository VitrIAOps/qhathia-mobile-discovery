package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.model.PairedDevice
import com.qhathia.mobile.discovery.core.domain.repository.DevicePairingRepository
import javax.inject.Inject

/** Reads the currently paired device for the "Comercio vinculado" view. */
class GetPairedDeviceUseCase @Inject constructor(
    private val repository: DevicePairingRepository,
) {
    suspend operator fun invoke(): Result<PairedDevice> = repository.getPairedDevice()
}
