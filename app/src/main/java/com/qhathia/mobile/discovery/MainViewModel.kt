package com.qhathia.mobile.discovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qhathia.mobile.discovery.core.domain.CartConstants
import com.qhathia.mobile.discovery.core.domain.model.DeviceSettings
import com.qhathia.mobile.discovery.core.domain.model.KioskLockState
import com.qhathia.mobile.discovery.core.domain.usecase.EndCustomerSessionUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ObserveAppRestartRequestsUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ObserveCartUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ObserveDeviceSettingsUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ObserveKioskLockUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ObserveStoreSessionUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ResumeKioskModeUseCase
import com.qhathia.mobile.discovery.feature.home.navigation.HOME_ROUTE
import com.qhathia.mobile.discovery.feature.onboarding.navigation.INITIAL_CONFIG_ROUTE
import com.qhathia.mobile.discovery.feature.onboarding.navigation.WELCOME_ROUTE
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * App-shell ViewModel. Owns the cross-screen actions and startup decisions the NavHost needs but no
 * single feature can own: the customer-session inactivity timer, resolving the start destination from
 * the persisted pairing state (unpaired → onboarding, paired → Home), and the kiosk lock state applied
 * by the activity.
 *
 * **Inactivity (design D2, `customer-session`).** The shell reports the current route
 * ([onRouteChanged]) and every interaction ([onUserActivity]), including touches inside dialogs and
 * typing. After [CartConstants.INACTIVITY_TIMEOUT_SECONDS] without activity, or
 * [CartConstants.CART_DIALOG_INACTIVITY_SECONDS] while the cart dialog is open
 * ([onCartDialogVisibilityChanged]), the session ends: the basket and the consumer forms are cleared
 * and [sessionTimeout] asks the shell to close its overlays and return Home. The timer runs on every
 * screen except Home and the onboarding flow; on Home it runs only while the basket is not empty.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    observeCart: ObserveCartUseCase,
    private val endCustomerSession: EndCustomerSessionUseCase,
    observeStoreSession: ObserveStoreSessionUseCase,
    observeDeviceSettings: ObserveDeviceSettingsUseCase,
    observeKioskLock: ObserveKioskLockUseCase,
    private val resumeKioskModeUseCase: ResumeKioskModeUseCase,
    observeAppRestartRequests: ObserveAppRestartRequestsUseCase,
) : ViewModel() {

    /** The tablet's remote settings, applied by the app shell (voice assistant, brightness). */
    val deviceSettings: StateFlow<DeviceSettings> =
        observeDeviceSettings()
            .stateIn(viewModelScope, SharingStarted.Eagerly, DeviceSettings.DEFAULT)

    /**
     * Whether the tablet must be locked on Discovery (screen pinning, no system bars, screen on).
     * `null` until the real state is read: the shell must not apply anything before that, or an
     * INACTIVE seed would unpin a pinned task on every start (design D8, task 1.1).
     */
    val kioskLock: StateFlow<KioskLockState?> =
        observeKioskLock()
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /**
     * Remote `RESTART_APP` requests the shell carries out from its Activity, so the trampoline joins
     * the pinned task (design D7). Collect only while the activity is started.
     */
    val restartRequests: Flow<Unit> = observeAppRestartRequests()

    /**
     * The first destination to show. Starts as [StartDestination.Loading] until the persisted session
     * is read, then resolves to [StartDestination.Home] when a device token is present or
     * [StartDestination.Onboarding] otherwise. The app must not build the NavHost with Home while this
     * is still `Loading`, so an unpaired device never flashes Home.
     */
    val startDestination: StateFlow<StartDestination> =
        observeStoreSession()
            .map { session ->
                if (session.isPaired) StartDestination.Home else StartDestination.Onboarding
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, StartDestination.Loading)

    private val currentRoute = MutableStateFlow<String?>(null)

    /** Bumped on every interaction; each change restarts the countdown. */
    private val activityTick = MutableStateFlow(0L)

    /** Whether the cart dialog is shown; it gets the longer inactivity budget. */
    private val cartDialogOpen = MutableStateFlow(false)

    private val _sessionTimeout = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    /**
     * Emits when the customer session ended on inactivity. The shell closes the cart and settings
     * overlays and pops back to Home. The data is already cleared when this emits.
     */
    val sessionTimeout: SharedFlow<Unit> = _sessionTimeout.asSharedFlow()

    init {
        startInactivityTimer(observeCart().map { it.totalItemCount > 0 }.distinctUntilChanged())
    }

    /** The NavHost's current route changed (`null` while the graph is not built yet). */
    fun onRouteChanged(route: String?) {
        currentRoute.value = route
    }

    /** Any touch or text change anywhere in the app, dialogs included. */
    fun onUserActivity() {
        activityTick.update { it + 1 }
    }

    /** The shell opened or closed the cart dialog; either is an interaction. */
    fun onCartDialogVisibilityChanged(open: Boolean) {
        cartDialogOpen.value = open
        onUserActivity()
    }

    /** Ends a temporary exit from kiosk mode (the merchant is back on Home). */
    fun resumeKioskMode() {
        resumeKioskModeUseCase()
    }

    private fun startInactivityTimer(basketHasItems: Flow<Boolean>) {
        viewModelScope.launch {
            // Every route, basket, activity or cart-dialog change re-evaluates and restarts the countdown.
            combine(currentRoute, basketHasItems, activityTick, cartDialogOpen) { route, hasItems, _, cartOpen ->
                when {
                    !isSessionTimed(route, hasItems) -> null
                    cartOpen -> CartConstants.CART_DIALOG_INACTIVITY_SECONDS
                    else -> CartConstants.INACTIVITY_TIMEOUT_SECONDS
                }
            }.collectLatest { budgetSeconds ->
                if (budgetSeconds == null) return@collectLatest
                delay(budgetSeconds * 1_000L)
                endCustomerSession()
                _sessionTimeout.emit(Unit)
            }
        }
    }
}

/**
 * Whether the inactivity countdown runs for [route]: never before the graph is built or during
 * onboarding (there is no customer yet), on Home only while the basket has items, and always on
 * every other screen.
 */
internal fun isSessionTimed(route: String?, basketHasItems: Boolean): Boolean = when (route) {
    null, WELCOME_ROUTE, INITIAL_CONFIG_ROUTE -> false
    HOME_ROUTE -> basketHasItems
    else -> true
}

/** The resolved first destination of the app on launch. */
enum class StartDestination {
    /** The persisted session has not been read yet. */
    Loading,

    /** The device is not paired; show the first-run onboarding (Welcome) flow. */
    Onboarding,

    /** The device is already paired; show Home (voice search). */
    Home,
}
