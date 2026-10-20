package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.repository.KioskSessionRepository
import javax.inject.Inject

/**
 * "Salir del modo kiosko": releases the lock until the merchant goes back to Home or the app restarts.
 * Only reachable from Configuración, which is already behind the exit PIN gate.
 */
class ExitKioskModeUseCase @Inject constructor(
    private val repository: KioskSessionRepository,
) {
    operator fun invoke() = repository.setExitedFromSettings(true)
}
