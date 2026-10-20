package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.repository.DeviceSettingsRepository
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Keeps the stored remote settings current: each heartbeat reports `settingsVersion`, and when it
 * differs from the stored one the settings are read again. The comparison is against the **stored**
 * version, so a failed read is retried by the next heartbeat. Suspends until cancelled.
 */
class SyncDeviceSettingsUseCase @Inject constructor(
    private val observeHeartbeat: ObserveHeartbeatUseCase,
    private val repository: DeviceSettingsRepository,
) {
    suspend operator fun invoke() {
        observeHeartbeat()
            .map { it.settingsVersion }
            .filterNotNull()
            .collect { reported ->
                if (reported != repository.storedVersion()) {
                    // A failure leaves the stored version unchanged, so the next heartbeat retries.
                    repository.refresh()
                }
            }
    }
}
