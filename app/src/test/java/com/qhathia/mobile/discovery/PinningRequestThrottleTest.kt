package com.qhathia.mobile.discovery

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PinningRequestThrottleTest {

    private val throttle = PinningRequestThrottle(minIntervalMillis = 15_000)

    @Test
    fun first_request_is_allowed() {
        assertTrue(throttle.tryAcquire(nowMillis = 1_000))
    }

    @Test
    fun requests_within_15_seconds_are_dropped() {
        throttle.tryAcquire(1_000)

        assertFalse(throttle.tryAcquire(2_000))
        assertFalse(throttle.tryAcquire(15_999))
    }

    @Test
    fun request_after_15_seconds_is_allowed_and_restarts_the_window() {
        throttle.tryAcquire(1_000)

        assertTrue(throttle.tryAcquire(16_000))
        assertFalse(throttle.tryAcquire(20_000))
    }

    @Test
    fun a_dropped_request_does_not_move_the_window() {
        throttle.tryAcquire(1_000)
        throttle.tryAcquire(10_000) // dropped

        assertTrue(throttle.tryAcquire(16_000))
    }

    @Test
    fun a_recorded_resume_request_delays_the_next_one() {
        throttle.record(5_000)

        assertFalse(throttle.tryAcquire(10_000))
        assertTrue(throttle.tryAcquire(20_000))
    }
}
