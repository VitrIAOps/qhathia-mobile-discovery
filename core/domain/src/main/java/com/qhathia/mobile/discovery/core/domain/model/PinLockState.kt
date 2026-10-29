package com.qhathia.mobile.discovery.core.domain.model

import kotlin.time.Duration

/** Whether the settings PIN gate accepts a PIN right now. */
sealed interface PinLockState {

    /** PINs are accepted; [attemptsLeft] wrong ones are left before the lock. */
    data class Open(val attemptsLeft: Int) : PinLockState

    /** Too many wrong PINs; the keypad is disabled for [remaining]. */
    data class Locked(val remaining: Duration) : PinLockState
}
