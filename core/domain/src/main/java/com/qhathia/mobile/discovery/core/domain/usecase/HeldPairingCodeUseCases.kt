package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.model.HeldPairingCode
import com.qhathia.mobile.discovery.core.domain.repository.HeldPairingCodeRepository
import javax.inject.Inject

/** The pairing code held from a previous visit to the pairing section, or `null` (design D6). */
class GetHeldPairingCodeUseCase @Inject constructor(
    private val repository: HeldPairingCodeRepository,
) {
    operator fun invoke(): HeldPairingCode? = repository.get()
}

/** Holds the pairing code on screen, or forgets it with `null` (design D6). */
class HoldPairingCodeUseCase @Inject constructor(
    private val repository: HeldPairingCodeRepository,
) {
    operator fun invoke(code: HeldPairingCode?) = repository.set(code)
}
