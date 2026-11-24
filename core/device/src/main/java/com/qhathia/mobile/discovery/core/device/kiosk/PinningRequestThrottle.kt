package com.qhathia.mobile.discovery.core.device.kiosk

/**
 * Limits how often pinning is requested: at most once per [minIntervalMillis],
 * so someone who refused "Pin app?" is not shown it in a loop. Times are monotonic milliseconds.
 */
internal class PinningRequestThrottle(private val minIntervalMillis: Long) {

    private var lastRequestAt: Long? = null

    /** Returns `true` and records [nowMillis] when a request is allowed now. */
    fun tryAcquire(nowMillis: Long): Boolean {
        val last = lastRequestAt
        if (last != null && nowMillis - last < minIntervalMillis) return false
        lastRequestAt = nowMillis
        return true
    }

    /** Records a request made without asking (e.g. on resume), so the next throttled one waits. */
    fun record(nowMillis: Long) {
        lastRequestAt = nowMillis
    }
}
