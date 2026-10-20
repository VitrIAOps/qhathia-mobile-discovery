package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.repository.KioskSessionRepository
import javax.inject.Inject

/** Ends a temporary exit from kiosk mode (the merchant left Configuración); the lock applies again. */
class ResumeKioskModeUseCase @Inject constructor(
    private val repository: KioskSessionRepository,
) {
    operator fun invoke() = repository.setExitedFromSettings(false)
}
