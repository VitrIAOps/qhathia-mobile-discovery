package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.model.PinAttempts
import com.qhathia.mobile.discovery.core.domain.model.PinLockState
import com.qhathia.mobile.discovery.core.testing.FakeSettingsPinAttemptsRepository
import com.qhathia.mobile.discovery.core.testing.MutableClock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

@OptIn(ExperimentalCoroutinesApi::class)
class PinLockUseCasesTest {

    private val start = Instant.parse("2026-10-29T12:00:00Z")
    private val clock = MutableClock(start)
    private val repository = FakeSettingsPinAttemptsRepository()

    private val registerFailure = RegisterPinFailureUseCase(repository, clock)
    private val reset = ResetPinAttemptsUseCase(repository)
    private val observe = ObservePinLockUseCase(repository, clock)

    private suspend fun fail(times: Int): PinLockState {
        var state: PinLockState = PinLockState.Open(5)
        repeat(times) { state = registerFailure() }
        return state
    }

    @Test
    fun four_failures_do_not_lock() = runTest {
        assertEquals(PinLockState.Open(attemptsLeft = 1), fail(4))
    }

    @Test
    fun fifth_failure_locks_for_30_seconds() = runTest {
        fail(4)

        assertEquals(PinLockState.Locked(30.seconds), registerFailure())
        assertEquals(start, repository.stored.value.lockedAt)
        assertEquals(1, repository.stored.value.lockCount)
    }

    @Test
    fun each_lock_doubles_up_to_the_15_minute_cap() = runTest {
        val expected = listOf(30.seconds, 1.minutes, 2.minutes, 4.minutes, 8.minutes, 15.minutes, 15.minutes)
        val seen = expected.map { duration ->
            val locked = fail(5)
            clock.advance(duration.toJavaDuration())
            assertEquals(PinLockState.Open(attemptsLeft = 5), observe().first())
            locked
        }

        assertEquals(expected.map { PinLockState.Locked(it) }, seen)
        assertEquals(expected.size, repository.stored.value.lockCount)
    }

    @Test
    fun lock_duration_is_coerced_and_does_not_overflow() {
        assertEquals(30.seconds, PinLockPolicy.lockDuration(0))
        assertEquals(15.minutes, PinLockPolicy.lockDuration(Int.MAX_VALUE))
    }

    @Test
    fun failures_while_locked_do_not_extend_the_lock() = runTest {
        fail(5)
        clock.advance(Duration.ofSeconds(10))

        assertEquals(PinLockState.Locked(20.seconds), registerFailure())
        assertEquals(start, repository.stored.value.lockedAt)
        assertEquals(1, repository.stored.value.lockCount)
    }

    @Test
    fun still_locked_at_29s_and_unlocked_with_5_attempts_at_30s_keeping_the_level() = runTest {
        fail(5)

        clock.advance(Duration.ofSeconds(29))
        assertEquals(PinLockState.Locked(1.seconds), observe().first())

        clock.advance(Duration.ofSeconds(1))
        assertEquals(PinLockState.Open(attemptsLeft = 5), observe().first())
        assertEquals(PinAttempts(lockCount = 1), repository.stored.value)
    }

    @Test
    fun success_after_3_failures_resets_the_count() = runTest {
        fail(3)

        reset()

        assertEquals(PinLockState.Open(attemptsLeft = 5), observe().first())
        assertEquals(PinLockState.Open(attemptsLeft = 4), registerFailure())
    }

    @Test
    fun correct_pin_after_a_lock_resets_the_level() = runTest {
        fail(5)
        clock.advance(Duration.ofSeconds(30))
        observe().first()

        reset()

        assertEquals(PinLockState.Locked(30.seconds), fail(5))
    }

    @Test
    fun clock_moved_back_restarts_the_lock_from_the_new_time() = runTest {
        fail(5)
        val earlier = start.minus(Duration.ofDays(2))
        clock.set(earlier)

        assertEquals(PinLockState.Locked(30.seconds), observe().first())
        assertEquals(earlier, repository.stored.value.lockedAt)

        // 30 s after the new time, not the original one.
        clock.advance(Duration.ofSeconds(20))
        assertEquals(PinLockState.Locked(10.seconds), observe().first())
    }

    @Test
    fun observe_refreshes_the_time_left_every_second_and_unlocks_on_time() = runTest {
        val virtualClock = MutableClock(start) { testScheduler.currentTime }
        val ticking = ObservePinLockUseCase(repository, virtualClock)
        repository.stored.value = PinAttempts(failedCount = 5, lockedAt = start.minusSeconds(28), lockCount = 1)
        val seen = collect(ticking)

        runCurrent()
        assertEquals(PinLockState.Locked(2.seconds), seen.last())

        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(PinLockState.Locked(1.seconds), seen.last())

        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(PinLockState.Open(attemptsLeft = 5), seen.last())
        assertNull(repository.stored.value.lockedAt)
        assertEquals(1, repository.stored.value.lockCount)
    }

    private fun TestScope.collect(useCase: ObservePinLockUseCase): List<PinLockState> {
        val seen = mutableListOf<PinLockState>()
        backgroundScope.launch { useCase().collect { seen += it } }
        return seen
    }
}
