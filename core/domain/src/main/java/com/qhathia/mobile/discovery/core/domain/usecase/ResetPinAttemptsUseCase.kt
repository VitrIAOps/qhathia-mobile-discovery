package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.model.PinAttempts
import com.qhathia.mobile.discovery.core.domain.repository.SettingsPinAttemptsRepository
import javax.inject.Inject

/** A correct settings PIN: the wrong-attempt count goes back to zero. */
class ResetPinAttemptsUseCase @Inject constructor(
    private val repository: SettingsPinAttemptsRepository,
) {
    suspend operator fun invoke() = repository.set(PinAttempts.NONE)
}
