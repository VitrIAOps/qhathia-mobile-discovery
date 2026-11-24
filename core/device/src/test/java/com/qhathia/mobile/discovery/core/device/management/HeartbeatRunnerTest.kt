package com.qhathia.mobile.discovery.core.device.management

import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HeartbeatRunnerTest {

    private val reporter = FakeDeviceReporter()

    private fun TestScope.start() {
        backgroundScope.launch { HeartbeatRunner(reporter, TestConfig).run() }
        runCurrent()
    }

    private fun TestScope.advance(seconds: Int) {
        advanceTimeBy(seconds.seconds)
        runCurrent()
    }

    @Test
    fun active_on_start_reports_right_away() = runTest {
        start()
        assertEquals(1, reporter.reports)
    }

    @Test
    fun reports_once_per_interval() = runTest {
        start()
        advance(59)
        assertEquals(1, reporter.reports)
        advance(1)
        assertEquals(2, reporter.reports)
    }

    @Test
    fun inactive_never_reports() = runTest {
        reporter.isActive.value = false
        start()
        advance(600)
        assertEquals(0, reporter.reports)
    }

    @Test
    fun becoming_active_reports_right_away_and_inactive_stops() = runTest {
        reporter.isActive.value = false
        start()
        reporter.isActive.value = true
        runCurrent()
        assertEquals(1, reporter.reports)
        reporter.isActive.value = false
        runCurrent()
        advance(600)
        assertEquals(1, reporter.reports)
    }

    @Test
    fun failures_retry_on_the_backoff_then_every_interval() = runTest {
        reporter.result = { Result.failure(IllegalStateException()) }
        start()
        advance(1)
        assertEquals(2, reporter.reports)
        advance(3)
        assertEquals(3, reporter.reports)
        advance(59)
        assertEquals(3, reporter.reports)
        advance(1)
        assertEquals(4, reporter.reports)
    }

    @Test
    fun success_after_failure_returns_to_the_interval() = runTest {
        reporter.result = { Result.failure(IllegalStateException()) }
        start()
        reporter.result = { Result.success(Unit) }
        advance(1)
        assertEquals(2, reporter.reports)
        advance(3)
        assertEquals(2, reporter.reports)
        advance(57)
        assertEquals(3, reporter.reports)
    }

    @Test
    fun internet_back_reports_at_once() = runTest {
        reporter.internetAvailable.value = false
        start()
        advance(10)
        reporter.internetAvailable.value = true
        runCurrent()
        assertEquals(2, reporter.reports)
    }

    @Test
    fun internet_staying_up_sends_no_extra_report() = runTest {
        start()
        reporter.internetAvailable.value = true
        runCurrent()
        assertEquals(1, reporter.reports)
    }

    @Test
    fun a_throwing_report_does_not_stop_the_loop() = runTest {
        reporter.result = { throw IllegalStateException("boom") }
        start()
        advance(1)
        assertEquals(2, reporter.reports)
    }
}
