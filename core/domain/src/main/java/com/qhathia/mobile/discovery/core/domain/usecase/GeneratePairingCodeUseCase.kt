package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.model.PairingCode
import com.qhathia.mobile.discovery.core.domain.repository.DevicePairingRepository
import javax.inject.Inject

/** Generates a fresh pairing code + session for the pairing screen. */
class GeneratePairingCodeUseCase @Inject constructor(
    private val repository: DevicePairingRepository,
) {
    suspend operator fun invoke(): Result<PairingCode> = repository.generateCode()
}
