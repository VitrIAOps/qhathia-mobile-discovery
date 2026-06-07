package com.qhathia.mobile.discovery.feature.productsearch.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qhathia.mobile.discovery.feature.productsearch.model.ProductSearchUiEffect

/**
 * Stateful product-search entry point. Binds [ProductSearchViewModel], collects its state with
 * lifecycle awareness, and forwards one-shot navigation effects to the app.
 */
@Composable
fun ProductSearchScreen(
    onNavigateHomeIdle: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProductSearchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                ProductSearchUiEffect.NavigateHomeIdle -> onNavigateHomeIdle()
                is ProductSearchUiEffect.NavigateToDetail -> onNavigateToDetail(effect.productId)
            }
        }
    }

    ProductSearchContent(uiState = uiState, onEvent = viewModel::onEvent, modifier = modifier)
}
