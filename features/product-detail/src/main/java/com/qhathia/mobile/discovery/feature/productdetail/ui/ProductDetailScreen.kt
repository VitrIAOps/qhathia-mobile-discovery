package com.qhathia.mobile.discovery.feature.productdetail.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qhathia.mobile.discovery.feature.productdetail.model.ProductDetailUiEffect

/**
 * Stateful product-detail entry point. Binds [ProductDetailViewModel], collects its state with
 * lifecycle awareness, and forwards one-shot navigation effects to the app.
 */
@Composable
fun ProductDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateHomeIdle: () -> Unit,
    onNavigateToSearch: (String) -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onOpenCart: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProductDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                ProductDetailUiEffect.NavigateBack -> onNavigateBack()
                ProductDetailUiEffect.NavigateHomeIdle -> onNavigateHomeIdle()
                is ProductDetailUiEffect.NavigateToSearch -> onNavigateToSearch(effect.query)
                is ProductDetailUiEffect.NavigateToDetail -> onNavigateToDetail(effect.productId)
                ProductDetailUiEffect.OpenCart -> onOpenCart()
            }
        }
    }

    ProductDetailContent(uiState = uiState, onEvent = viewModel::onEvent, modifier = modifier)
}
