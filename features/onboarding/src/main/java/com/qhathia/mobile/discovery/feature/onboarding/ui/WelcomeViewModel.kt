package com.qhathia.mobile.discovery.feature.onboarding.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qhathia.mobile.discovery.core.domain.usecase.DismissUnlinkedNoticeUseCase
import com.qhathia.mobile.discovery.core.domain.usecase.ObserveStoreSessionUseCase
import com.qhathia.mobile.discovery.feature.onboarding.model.WelcomeUiEffect
import com.qhathia.mobile.discovery.feature.onboarding.model.WelcomeUiEvent
import com.qhathia.mobile.discovery.feature.onboarding.model.WelcomeUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Welcome screen state: shows the "this tablet was unlinked" notice while the store session has one
 * pending, and dismisses it on "Continuar" before navigating on.
 */
@HiltViewModel
class WelcomeViewModel @Inject constructor(
    observeStoreSession: ObserveStoreSessionUseCase,
    private val dismissUnlinkedNotice: DismissUnlinkedNoticeUseCase,
) : ViewModel() {

    val uiState: StateFlow<WelcomeUiState> = observeStoreSession()
        .map { WelcomeUiState(showUnlinkedNotice = it.unlinkedNotice) }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), WelcomeUiState())

    private val _uiEffect = MutableSharedFlow<WelcomeUiEffect>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val uiEffect: SharedFlow<WelcomeUiEffect> = _uiEffect.asSharedFlow()

    fun onEvent(event: WelcomeUiEvent) {
        when (event) {
            WelcomeUiEvent.Continue -> viewModelScope.launch {
                dismissUnlinkedNotice()
                _uiEffect.emit(WelcomeUiEffect.NavigateToInitialConfiguration)
            }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
