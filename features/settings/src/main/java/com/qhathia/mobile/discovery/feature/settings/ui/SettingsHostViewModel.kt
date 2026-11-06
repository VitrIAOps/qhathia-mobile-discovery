package com.qhathia.mobile.discovery.feature.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qhathia.mobile.discovery.core.domain.usecase.ObserveStoreSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * State of the settings host shared by its panels: the store name for the titles (F-05), or `null`
 * while it is unknown, so the titles fall back to "TIENDA" / "Tienda".
 */
@HiltViewModel
class SettingsHostViewModel @Inject constructor(
    observeStoreSession: ObserveStoreSessionUseCase,
) : ViewModel() {

    val storeName: StateFlow<String?> = observeStoreSession()
        .map { session -> session.storeName?.takeIf { it.isNotBlank() } }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
