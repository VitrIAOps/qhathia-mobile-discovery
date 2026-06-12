package com.qhathia.mobile.discovery.feature.catalog.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qhathia.mobile.discovery.core.domain.model.SearchSource
import com.qhathia.mobile.discovery.feature.catalog.model.CatalogUiEffect

/**
 * Stateful catalog entry point. Binds [CatalogViewModel], collects its state with lifecycle awareness,
 * and forwards one-shot navigation effects to the app.
 */
@Composable
fun CatalogScreen(
    onNavigateHomeIdle: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToSearch: (String, SearchSource) -> Unit,
    onOpenCart: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CatalogViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                CatalogUiEffect.NavigateHomeIdle -> onNavigateHomeIdle()
                is CatalogUiEffect.NavigateToDetail -> onNavigateToDetail(effect.productId)
                CatalogUiEffect.OpenCart -> onOpenCart()
                is CatalogUiEffect.NavigateToSearch -> onNavigateToSearch(effect.query, effect.source)
            }
        }
    }

    CatalogContent(uiState = uiState, onEvent = viewModel::onEvent, modifier = modifier)
}
