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
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

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
    fun fifth_failure_locks_for_24_hours() = runTest {
        fail(4)

        assertEquals(PinLockState.Locked(24.hours), registerFailure())
        assertEquals(start, repository.stored.value.lockedAt)
    }

    @Test
    fun failures_while_locked_do_not_extend_the_lock() = runTest {
        fail(5)
        clock.advance(Duration.ofHours(1))

        assertEquals(PinLockState.Locked(23.hours), registerFailure())
        assertEquals(start, repository.stored.value.lockedAt)
    }

    @Test
    fun still_locked_at_23h59m_and_unlocked_with_5_attempts_at_24h() = runTest {
        fail(5)

        clock.advance(Duration.ofHours(23).plusMinutes(59))
        assertEquals(PinLockState.Locked(1.minutes), observe().first())

        clock.advance(Duration.ofMinutes(1))
        assertEquals(PinLockState.Open(attemptsLeft = 5), observe().first())
        assertEquals(PinAttempts.NONE, repository.stored.value)
    }

    @Test
    fun success_after_3_failures_resets_the_count() = runTest {
        fail(3)

        reset()

        assertEquals(PinLockState.Open(attemptsLeft = 5), observe().first())
        assertEquals(PinLockState.Open(attemptsLeft = 4), registerFailure())
    }

    @Test
    fun clock_moved_back_restarts_the_lock_from_the_new_time() = runTest {
        fail(5)
        val earlier = start.minus(Duration.ofDays(2))
        clock.set(earlier)

        assertEquals(PinLockState.Locked(24.hours), observe().first())
        assertEquals(earlier, repository.stored.value.lockedAt)

        // 24 h after the new time, not the original one.
        clock.advance(Duration.ofHours(23))
        assertEquals(PinLockState.Locked(1.hours), observe().first())
    }

    @Test
    fun observe_refreshes_the_time_left_every_minute_and_unlocks_on_time() = runTest {
        val virtualClock = MutableClock(start) { testScheduler.currentTime }
        val ticking = ObservePinLockUseCase(repository, virtualClock)
        repository.stored.value = PinAttempts(failedCount = 5, lockedAt = start.minus(Duration.ofHours(23).plusMinutes(58)))
        val seen = collect(ticking)

        runCurrent()
        assertEquals(PinLockState.Locked(2.minutes), seen.last())

        advanceTimeBy(60_000)
        runCurrent()
        assertEquals(PinLockState.Locked(1.minutes), seen.last())

        advanceTimeBy(60_000)
        runCurrent()
        assertEquals(PinLockState.Open(attemptsLeft = 5), seen.last())
        assertNull(repository.stored.value.lockedAt)
    }

    private fun TestScope.collect(useCase: ObservePinLockUseCase): List<PinLockState> {
        val seen = mutableListOf<PinLockState>()
        backgroundScope.launch { useCase().collect { seen += it } }
        return seen
    }
}
