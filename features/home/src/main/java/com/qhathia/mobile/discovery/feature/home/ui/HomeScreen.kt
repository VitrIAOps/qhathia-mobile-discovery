package com.qhathia.mobile.discovery.feature.home.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.LaunchedEffect
import com.qhathia.mobile.discovery.feature.home.model.HomeUiEffect

/**
 * Stateful Home entry point. Binds [HomeViewModel], collects its state with lifecycle awareness,
 * and forwards one-shot navigation effects to the hoisted callbacks.
 */
@Composable
fun HomeScreen(
    onNavigateToCatalog: () -> Unit,
    onNavigateToResults: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                HomeUiEffect.NavigateToCatalog -> onNavigateToCatalog()
                is HomeUiEffect.NavigateToResults -> onNavigateToResults(effect.query)
                HomeUiEffect.NavigateToSettings -> onNavigateToSettings()
            }
        }
    }

    HomeContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        modifier = modifier,
    )
}
