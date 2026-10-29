package com.qhathia.mobile.discovery.core.common

import android.os.SystemClock

/**
 * Monotonic milliseconds since boot, counting deep sleep (screen off). Use it to measure how long is
 * left on a deadline across a screen off/on, where `System.nanoTime` would stop. Injected so tests can
 * drive it with virtual time.
 */
fun interface ElapsedRealtimeClock {
    fun elapsedRealtimeMillis(): Long
}

/** Production [ElapsedRealtimeClock] over [SystemClock.elapsedRealtime]. */
class SystemElapsedRealtimeClock : ElapsedRealtimeClock {
    override fun elapsedRealtimeMillis(): Long = SystemClock.elapsedRealtime()
}
