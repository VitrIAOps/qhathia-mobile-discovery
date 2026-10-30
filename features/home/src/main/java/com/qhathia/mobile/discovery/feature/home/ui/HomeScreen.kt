package com.qhathia.mobile.discovery.feature.home.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qhathia.mobile.discovery.core.domain.model.SearchSource
import com.qhathia.mobile.discovery.core.ui.permission.rememberMicPermissionGate
import com.qhathia.mobile.discovery.feature.home.model.HomeUiEffect
import com.qhathia.mobile.discovery.feature.home.model.HomeUiEvent

/**
 * Stateful Home entry point. Binds [HomeViewModel], collects its state with lifecycle awareness,
 * forwards one-shot navigation effects, and gates voice search on the microphone permission —
 * requesting it on activation and informing the consumer if it is denied.
 */
@Composable
fun HomeScreen(
    onNavigateToCatalog: () -> Unit,
    onNavigateToProductSearch: (String, SearchSource) -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val activateMic = rememberMicPermissionGate { viewModel.onEvent(HomeUiEvent.ActivateMic) }

    // ON_STOP fires once the navigation transition has finished and Home is fully hidden.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        viewModel.onEvent(HomeUiEvent.ScreenStopped)
    }

    LaunchedEffect(viewModel) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                HomeUiEffect.NavigateToCatalog -> onNavigateToCatalog()
                is HomeUiEffect.NavigateToProductSearch -> onNavigateToProductSearch(effect.query, effect.source)
                HomeUiEffect.NavigateToSettings -> onNavigateToSettings()
            }
        }
    }

    HomeContent(
        uiState = uiState,
        onEvent = { event ->
            // Gate mic activation on the runtime permission; everything else goes straight to the VM.
            if (event is HomeUiEvent.ActivateMic) activateMic() else viewModel.onEvent(event)
        },
        modifier = modifier,
    )
}
