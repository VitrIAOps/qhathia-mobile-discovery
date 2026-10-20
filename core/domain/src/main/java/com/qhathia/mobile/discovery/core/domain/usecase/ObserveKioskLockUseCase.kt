package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.model.KioskLockState
import com.qhathia.mobile.discovery.core.domain.repository.DeviceSettingsRepository
import com.qhathia.mobile.discovery.core.domain.repository.KioskSessionRepository
import com.qhathia.mobile.discovery.core.domain.repository.StoreSessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

/**
 * Streams whether the kiosk lock is active: paired, remote `kioskMode` on, and not exited from
 * Configuración. An unpaired tablet is never locked, even with the default `kioskMode = true`.
 */
class ObserveKioskLockUseCase @Inject constructor(
    private val storeSessionRepository: StoreSessionRepository,
    private val deviceSettingsRepository: DeviceSettingsRepository,
    private val kioskSessionRepository: KioskSessionRepository,
) {
    operator fun invoke(): Flow<KioskLockState> =
        combine(
            storeSessionRepository.observe(),
            deviceSettingsRepository.observe(),
            kioskSessionRepository.exitedFromSettings,
        ) { session, settings, exited ->
            KioskLockState(active = session.isPaired && settings.kioskMode && !exited)
        }.distinctUntilChanged()
}
