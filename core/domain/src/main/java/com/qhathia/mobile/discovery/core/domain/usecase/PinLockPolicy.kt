package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.SettingsConstants
import com.qhathia.mobile.discovery.core.domain.model.PinAttempts
import com.qhathia.mobile.discovery.core.domain.model.PinLockState
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.toJavaDuration
import kotlin.time.toKotlinDuration

/** The settings PIN lock rules (design D9, settings-pin-lockout D1–D2), shared by the PIN use cases. */
internal object PinLockPolicy {

    /**
     * How long the [lockCount]th lock lasts: 30 s, 1, 2, 4, 8 min, then [SettingsConstants.PIN_LOCK_MAX]
     * for every further lock. [lockCount] is coerced to at least 1.
     */
    fun lockDuration(lockCount: Int): Duration {
        val doublings = (lockCount.coerceAtLeast(1) - 1).coerceAtMost(MAX_DOUBLINGS)
        return (SettingsConstants.PIN_LOCK_BASE * (1 shl doublings)).coerceAtMost(SettingsConstants.PIN_LOCK_MAX)
    }

    /**
     * Applies the time rules to [attempts] at [now]:
     * - a lock that has lasted its [lockDuration] ends, with 5 new attempts; the lock level is kept,
     *   so the next lock lasts longer;
     * - a lock that starts after [now] (the clock moved back) restarts from [now], so it is never
     *   shortened.
     */
    fun normalize(attempts: PinAttempts, now: Instant): PinAttempts {
        val lockedAt = attempts.lockedAt
        return when {
            lockedAt == null ->
                if (attempts.failedCount >= SettingsConstants.PIN_MAX_ATTEMPTS) {
                    attempts.copy(lockedAt = now, lockCount = attempts.lockCount.coerceAtLeast(1))
                } else {
                    attempts
                }
            now.isBefore(lockedAt) -> attempts.copy(lockedAt = now)
            !now.isBefore(lockEnd(attempts, lockedAt)) -> PinAttempts(lockCount = attempts.lockCount)
            else -> attempts
        }
    }

    /** The gate state for already [normalize]d [attempts] at [now]. */
    fun state(attempts: PinAttempts, now: Instant): PinLockState {
        val lockedAt = attempts.lockedAt
            ?: return PinLockState.Open(
                attemptsLeft = (SettingsConstants.PIN_MAX_ATTEMPTS - attempts.failedCount).coerceAtLeast(0),
            )
        return PinLockState.Locked(java.time.Duration.between(now, lockEnd(attempts, lockedAt)).toKotlinDuration())
    }

    private fun lockEnd(attempts: PinAttempts, lockedAt: Instant): Instant =
        lockedAt.plus(lockDuration(attempts.lockCount).toJavaDuration())

    /** Enough doublings of 30 s to pass the cap; avoids an Int overflow for a huge lock count. */
    private const val MAX_DOUBLINGS = 10
}
