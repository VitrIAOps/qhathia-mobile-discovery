package com.qhathia.mobile.discovery

import com.qhathia.mobile.discovery.core.domain.model.KioskLockState
import com.qhathia.mobile.discovery.core.domain.usecase.EndCustomerSessionUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ObserveAppRestartRequestsUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ObserveCartUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ObserveDeviceSettingsUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ObserveKioskLockUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ObserveStoreSessionUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ResumeKioskModeUseCase
import com.qhathia.mobile.discovery.core.testing.CartFixtures
import com.qhathia.mobile.discovery.core.testing.FakeAppRestarter
import com.qhathia.mobile.discovery.core.testing.FakeCartRepository
import com.qhathia.mobile.discovery.core.testing.FakeCustomerSessionRepository
import com.qhathia.mobile.discovery.core.testing.FakeDeviceSettingsRepository
import com.qhathia.mobile.discovery.core.testing.FakeKioskSessionRepository
import com.qhathia.mobile.discovery.core.testing.FakeStoreSessionRepository
import com.qhathia.mobile.discovery.core.testing.MainDispatcherExtension
import com.qhathia.mobile.discovery.feature.catalog.navigation.catalogRoute
import com.qhathia.mobile.discovery.feature.home.navigation.HOME_ROUTE
import com.qhathia.mobile.discovery.feature.onboarding.navigation.INITIAL_CONFIG_ROUTE
import com.qhathia.mobile.discovery.feature.onboarding.navigation.WELCOME_ROUTE
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension(StandardTestDispatcher())

    private val cartRepository = FakeCartRepository(CartFixtures.cart())
    private val sessionRepository = FakeCustomerSessionRepository()

    /** An off-Home customer screen. */
    private val offHomeRoute = catalogRoute()

    private fun viewModel(storeSession: FakeStoreSessionRepository = FakeStoreSessionRepository()): MainViewModel {
        val settings = FakeDeviceSettingsRepository()
        val kioskSession = FakeKioskSessionRepository()
        return MainViewModel(
            observeCart = ObserveCartUseCase(cartRepository),
            endCustomerSession = EndCustomerSessionUseCase(cartRepository, sessionRepository),
            observeStoreSession = ObserveStoreSessionUseCase(storeSession),
            observeDeviceSettings = ObserveDeviceSettingsUseCase(settings),
            observeKioskLock = ObserveKioskLockUseCase(storeSession, settings, kioskSession),
            resumeKioskModeUseCase = ResumeKioskModeUseCase(kioskSession),
            observeAppRestartRequests = ObserveAppRestartRequestsUseCase(FakeAppRestarter()),
        )
    }

    /** Counts [MainViewModel.sessionTimeout] emissions while the test runs. */
    private fun TestScope.countTimeouts(vm: MainViewModel): () -> Int {
        var count = 0
        backgroundScope.launch { vm.sessionTimeout.collect { count++ } }
        return { count }
    }

    private fun TestScope.wait(seconds: Long) {
        advanceTimeBy(seconds * 1_000L)
        runCurrent()
    }

    @Test
    fun kiosk_lock_is_never_inactive_before_the_real_state_is_read() = runTest(mainDispatcher.dispatcher) {
        // A pinned tablet restarting: paired, kioskMode on. An INACTIVE seed would unpin it (task 1.1).
        val storeSession = FakeStoreSessionRepository().apply { saveDeviceSession("device-jwt", "Tablet") }
        val vm = viewModel(storeSession)
        val seen = mutableListOf<KioskLockState?>()
        backgroundScope.launch { vm.kioskLock.collect { seen += it } }

        runCurrent()

        assertEquals(listOf(null, KioskLockState(active = true)), seen)
    }

    @Test
    fun revoked_session_routes_to_onboarding_and_releases_the_kiosk() = runTest(mainDispatcher.dispatcher) {
        // A paired, pinned tablet that the merchant unlinks (F-101).
        val storeSession = FakeStoreSessionRepository().apply { saveDeviceSession("device-jwt", "Tablet") }
        val vm = viewModel(storeSession)
        val locks = mutableListOf<KioskLockState?>()
        backgroundScope.launch { vm.kioskLock.collect { locks += it } }
        runCurrent()
        assertEquals(StartDestination.Home, vm.startDestination.value)

        storeSession.revokeDeviceSession()
        runCurrent()

        assertEquals(StartDestination.Onboarding, vm.startDestination.value)
        assertEquals(KioskLockState(active = false), locks.last())
    }

    @Test
    fun given_off_home_when_idle_for_60s_then_session_ends() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel()
        val timeouts = countTimeouts(vm)
        vm.onRouteChanged(offHomeRoute)

        wait(59)
        assertEquals(0, timeouts())
        assertFalse(cartRepository.current.isEmpty)

        wait(1)
        assertEquals(1, timeouts())
        assertTrue(cartRepository.current.isEmpty)
        assertEquals(1, sessionRepository.endCount)
    }

    @Test
    fun given_off_home_when_activity_at_59s_then_countdown_restarts() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel()
        val timeouts = countTimeouts(vm)
        vm.onRouteChanged(offHomeRoute)

        wait(59)
        vm.onUserActivity()
        wait(59)
        assertEquals(0, timeouts())
        assertFalse(cartRepository.current.isEmpty)

        wait(1)
        assertEquals(1, timeouts())
    }

    @Test
    fun given_home_with_empty_basket_when_idle_then_never_ends() = runTest(mainDispatcher.dispatcher) {
        cartRepository.clear()
        val vm = viewModel()
        val timeouts = countTimeouts(vm)
        vm.onRouteChanged(HOME_ROUTE)

        wait(600)

        assertEquals(0, timeouts())
        assertEquals(0, sessionRepository.endCount)
    }

    @Test
    fun given_home_with_items_when_idle_for_60s_then_session_ends() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel()
        val timeouts = countTimeouts(vm)
        vm.onRouteChanged(HOME_ROUTE)

        wait(60)

        assertEquals(1, timeouts())
        assertTrue(cartRepository.current.isEmpty)
    }

    @Test
    fun given_session_ended_on_home_when_idle_again_then_no_second_end() = runTest(mainDispatcher.dispatcher) {
        // After the end the basket is empty, so Home stops timing.
        val vm = viewModel()
        val timeouts = countTimeouts(vm)
        vm.onRouteChanged(HOME_ROUTE)

        wait(60)
        wait(600)

        assertEquals(1, timeouts())
        assertEquals(1, sessionRepository.endCount)
    }

    @Test
    fun given_onboarding_when_idle_then_never_ends() = runTest(mainDispatcher.dispatcher) {
        val vm = viewModel()
        val timeouts = countTimeouts(vm)
        vm.onRouteChanged(INITIAL_CONFIG_ROUTE)

        wait(600)

        assertEquals(0, timeouts())
        assertFalse(cartRepository.current.isEmpty)
    }

    @Test
    fun timed_routes_follow_the_session_rule() {
        assertFalse(isSessionTimed(null, basketHasItems = true))
        assertFalse(isSessionTimed(WELCOME_ROUTE, basketHasItems = true))
        assertFalse(isSessionTimed(INITIAL_CONFIG_ROUTE, basketHasItems = true))
        assertFalse(isSessionTimed(HOME_ROUTE, basketHasItems = false))
        assertTrue(isSessionTimed(HOME_ROUTE, basketHasItems = true))
        assertTrue(isSessionTimed(offHomeRoute, basketHasItems = false))
    }
}
