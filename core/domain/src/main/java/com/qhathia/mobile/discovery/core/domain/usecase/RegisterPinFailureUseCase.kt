package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.SettingsConstants
import com.qhathia.mobile.discovery.core.domain.model.PinAttempts
import com.qhathia.mobile.discovery.core.domain.model.PinLockState
import com.qhathia.mobile.discovery.core.domain.repository.SettingsPinAttemptsRepository
import java.time.Clock
import javax.inject.Inject

/**
 * Counts one wrong settings PIN. The [SettingsConstants.PIN_MAX_ATTEMPTS]th in a row locks the gate;
 * each lock lasts longer than the previous one, from [SettingsConstants.PIN_LOCK_BASE] up to
 * [SettingsConstants.PIN_LOCK_MAX] (settings-pin-lockout D2). Only wrong PINs are counted: call it only when the PIN
 * was checked and rejected, never on a failed check. Returns the gate state afterwards and whether
 * this PIN started a new lock ([PinFailureResult.startedLock]), so the caller reports it without
 * reading UI state that an observer may already have updated (F-13).
 */
class RegisterPinFailureUseCase @Inject constructor(
    private val repository: SettingsPinAttemptsRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(): PinFailureResult {
        val now = clock.instant()
        val current = PinLockPolicy.normalize(repository.get(), now)
        val next = if (current.lockedAt != null) {
            current // already locked: not counted again
        } else {
            val count = current.failedCount + 1
            val locks = count >= SettingsConstants.PIN_MAX_ATTEMPTS
            PinAttempts(
                failedCount = count,
                lockedAt = if (locks) now else null,
                lockCount = if (locks) current.lockCount + 1 else current.lockCount,
            )
        }
        val startedLock = current.lockedAt == null && next.lockedAt != null
        repository.set(next)
        return PinFailureResult(PinLockPolicy.state(next, now), startedLock)
    }
}

/**
 * Result of one wrong settings PIN.
 *
 * @property state the gate state after counting it.
 * @property startedLock `true` only when this PIN locked a gate that was open, i.e. a lock to report.
 */
data class PinFailureResult(val state: PinLockState, val startedLock: Boolean)
