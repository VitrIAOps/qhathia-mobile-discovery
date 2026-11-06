package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.SettingsConstants
import com.qhathia.mobile.discovery.core.domain.model.PinLockState
import com.qhathia.mobile.discovery.core.domain.repository.SettingsPinAttemptsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transformLatest
import java.time.Clock
import javax.inject.Inject

/**
 * Streams the settings PIN gate state. While locked it emits again every
 * [SettingsConstants.PIN_LOCK_REFRESH_INTERVAL] with the time left, and emits
 * [PinLockState.Open] once the lock has lasted its duration (the stored lock ends then, keeping its level). A clock
 * moved back restarts the lock from the current time (design D9).
 */
class ObservePinLockUseCase @Inject constructor(
    private val repository: SettingsPinAttemptsRepository,
    private val clock: Clock,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<PinLockState> = repository.observe().transformLatest { stored ->
        var attempts = stored
        while (true) {
            val now = clock.instant()
            val normalized = PinLockPolicy.normalize(attempts, now)
            if (normalized != attempts) {
                repository.set(normalized)
                attempts = normalized
            }
            emit(PinLockPolicy.state(normalized, now))
            if (normalized.lockedAt == null) break
            delay(SettingsConstants.PIN_LOCK_REFRESH_INTERVAL)
        }
    }
}
