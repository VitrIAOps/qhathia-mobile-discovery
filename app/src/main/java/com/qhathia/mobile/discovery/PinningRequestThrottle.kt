package com.qhathia.mobile.discovery

/**
 * Limits how often pinning is requested from Home (design D8): at most once per [minIntervalMillis],
 * so a customer who refused "Pin app?" is not shown it in a loop. Times are monotonic milliseconds.
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
