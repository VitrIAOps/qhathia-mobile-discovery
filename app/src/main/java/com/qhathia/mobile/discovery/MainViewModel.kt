package com.qhathia.mobile.discovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qhathia.mobile.discovery.core.domain.model.DeviceSettings
import com.qhathia.mobile.discovery.core.domain.model.KioskLockState
import com.qhathia.mobile.discovery.core.domain.usecase.ClearCartUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ObserveDeviceSettingsUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ObserveKioskLockUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ObserveStoreSessionUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ResumeKioskModeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * App-shell ViewModel. Owns the cross-screen actions and startup decisions the NavHost needs but no
 * single feature can own: clearing the cart on the off-Home inactivity timeout, and resolving the
 * start destination from the persisted pairing state (unpaired → onboarding, paired → Home), and the
 * kiosk lock state applied by the activity.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val clearCartUseCase: ClearCartUseCase,
    observeStoreSession: ObserveStoreSessionUseCase,
    observeDeviceSettings: ObserveDeviceSettingsUseCase,
    observeKioskLock: ObserveKioskLockUseCase,
    private val resumeKioskModeUseCase: ResumeKioskModeUseCase,
) : ViewModel() {

    /** The tablet's remote settings, applied by the app shell (voice assistant, brightness). */
    val deviceSettings: StateFlow<DeviceSettings> =
        observeDeviceSettings()
            .stateIn(viewModelScope, SharingStarted.Eagerly, DeviceSettings.DEFAULT)

    /** Whether the tablet must be locked on Discovery (screen pinning, no system bars, screen on). */
    val kioskLock: StateFlow<KioskLockState> =
        observeKioskLock()
            .stateIn(viewModelScope, SharingStarted.Eagerly, KioskLockState.INACTIVE)

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

    /** Ends a temporary exit from kiosk mode (the merchant is back on Home). */
    fun resumeKioskMode() {
        resumeKioskModeUseCase()
    }

    /** Empties the cart (invoked by the off-Home inactivity timer). */
    fun clearCart() {
        viewModelScope.launch { clearCartUseCase() }
    }
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
