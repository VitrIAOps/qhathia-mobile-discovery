package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.repository.DevicePairingRepository
import javax.inject.Inject

/** Unpairs the device. No backend API exists yet — see [DevicePairingRepository.unpair]. */
class UnpairDeviceUseCase @Inject constructor(
    private val repository: DevicePairingRepository,
) {
    suspend operator fun invoke(): Result<Unit> = repository.unpair()
}
