package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.SettingsConstants
import com.qhathia.mobile.discovery.core.domain.model.PinAttempts
import com.qhathia.mobile.discovery.core.domain.model.PinLockState
import java.time.Duration
import java.time.Instant
import kotlin.time.toKotlinDuration

/** The settings PIN lock rules (design D9), shared by the PIN use cases. */
internal object PinLockPolicy {

    private val lockDuration: Duration = Duration.ofMillis(SettingsConstants.PIN_LOCK_DURATION.inWholeMilliseconds)

    /**
     * Applies the time rules to [attempts] at [now]:
     * - a lock that has lasted 24 h is cleared, with 5 new attempts;
     * - a lock that starts after [now] (the clock moved back) restarts from [now], so it is never
     *   shortened.
     */
    fun normalize(attempts: PinAttempts, now: Instant): PinAttempts {
        val lockedAt = attempts.lockedAt
        return when {
            lockedAt == null ->
                if (attempts.failedCount >= SettingsConstants.PIN_MAX_ATTEMPTS) attempts.copy(lockedAt = now) else attempts
            now.isBefore(lockedAt) -> attempts.copy(lockedAt = now)
            !now.isBefore(lockedAt.plus(lockDuration)) -> PinAttempts.NONE
            else -> attempts
        }
    }

    /** The gate state for already [normalize]d [attempts] at [now]. */
    fun state(attempts: PinAttempts, now: Instant): PinLockState {
        val lockedAt = attempts.lockedAt
            ?: return PinLockState.Open(
                attemptsLeft = (SettingsConstants.PIN_MAX_ATTEMPTS - attempts.failedCount).coerceAtLeast(0),
            )
        return PinLockState.Locked(Duration.between(now, lockedAt.plus(lockDuration)).toKotlinDuration())
    }
}
