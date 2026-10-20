package com.qhathia.mobile.discovery.feature.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qhathia.mobile.discovery.core.domain.usecase.ExitKioskModeUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ObserveKioskLockUseCase
import com.qhathia.mobile.discovery.feature.settings.model.KioskExitUiEvent
import com.qhathia.mobile.discovery.feature.settings.model.KioskExitUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Drives "Salir del modo kiosko" in the settings side menu. Settings are already behind the exit PIN
 * gate (or open freely when no PIN is set), so exiting asks for nothing more. The app shell reacts to
 * the lock state: it unpins and shows the system bars.
 */
@HiltViewModel
class KioskExitViewModel @Inject constructor(
    observeKioskLock: ObserveKioskLockUseCase,
    private val exitKioskMode: ExitKioskModeUseCase,
) : ViewModel() {

    private val hasExited = MutableStateFlow(false)

    val uiState: StateFlow<KioskExitUiState> =
        combine(observeKioskLock(), hasExited) { lock, exited ->
            KioskExitUiState(isLockActive = lock.active, hasExited = exited)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), KioskExitUiState())

    fun onEvent(event: KioskExitUiEvent) {
        when (event) {
            KioskExitUiEvent.ExitKioskMode -> {
                exitKioskMode()
                hasExited.value = true
            }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
