package com.qhathia.mobile.discovery.core.testing

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * A [Clock] for tests: it reads [base] plus [offsetMillis] (for example the coroutine test scheduler's
 * virtual time), and can be moved with [advance] or set with [set], also backwards.
 */
class MutableClock(
    private var base: Instant = Instant.parse("2026-10-29T12:00:00Z"),
    private val offsetMillis: () -> Long = { 0L },
) : Clock() {

    fun advance(by: Duration) {
        base = base.plus(by)
    }

    fun set(instant: Instant) {
        base = instant.minusMillis(offsetMillis())
    }

    override fun instant(): Instant = base.plusMillis(offsetMillis())

    override fun getZone(): ZoneId = ZoneOffset.UTC

    override fun withZone(zone: ZoneId): Clock = this
}
