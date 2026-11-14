package com.qhathia.mobile.discovery.feature.productdetail.ui

import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import com.qhathia.mobile.discovery.core.ui.component.CartFeedbackState
import com.qhathia.mobile.discovery.core.ui.component.CartFeedbackHost
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qhathia.mobile.discovery.core.domain.model.SearchSource
import com.qhathia.mobile.discovery.core.ui.R as UiR
import com.qhathia.mobile.discovery.core.ui.permission.rememberMicPermissionGate
import com.qhathia.mobile.discovery.feature.productdetail.model.ProductDetailUiEffect
import com.qhathia.mobile.discovery.feature.productdetail.model.ProductDetailUiEvent

/**
 * Stateful product-detail entry point. Binds [ProductDetailViewModel], collects its state with
 * lifecycle awareness, and forwards one-shot navigation effects to the app.
 */
@Composable
fun ProductDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateHomeIdle: () -> Unit,
    onNavigateToSearch: (String, SearchSource) -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onOpenCart: () -> Unit,
    onNavigateToCatalog: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProductDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lineLimitMessage = stringResource(UiR.string.discovery_cart_line_limit)
    val limitOk = stringResource(UiR.string.discovery_cart_limit_ok)
    val viewCart = stringResource(UiR.string.discovery_cart_view)
    val resources = LocalContext.current.resources
    val cartFeedback = remember { CartFeedbackState() }
    val feedbackScope = rememberCoroutineScope()

    LaunchedEffect(viewModel) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                ProductDetailUiEffect.NavigateBack -> onNavigateBack()
                ProductDetailUiEffect.NavigateHomeIdle -> onNavigateHomeIdle()
                is ProductDetailUiEffect.NavigateToSearch -> onNavigateToSearch(effect.query, effect.source)
                is ProductDetailUiEffect.NavigateToDetail -> onNavigateToDetail(effect.productId)
                ProductDetailUiEffect.OpenCart -> onOpenCart()
                ProductDetailUiEffect.NavigateToCatalog -> onNavigateToCatalog()
                ProductDetailUiEffect.LineLimitReached ->
                    feedbackScope.launch { cartFeedback.showLimit(lineLimitMessage, limitOk) }
                is ProductDetailUiEffect.AddedToCart -> {
                    val message = resources.getQuantityString(
                        UiR.plurals.discovery_cart_added,
                        effect.quantity,
                        effect.productName,
                        effect.quantity,
                    )
                    feedbackScope.launch {
                        if (cartFeedback.showAdded(message, viewCart)) viewModel.onEvent(ProductDetailUiEvent.OpenCart)
                    }
                }
            }
        }
    }

    // System Back runs the same action as "Volver" (wave 2, D2).
    BackHandler { viewModel.onEvent(ProductDetailUiEvent.Back) }

    // The search screen listens as soon as it opens, so the permission is asked here (wave 2, D4).
    val activateMic = rememberMicPermissionGate { viewModel.onEvent(ProductDetailUiEvent.ActivateMic) }

    Box(modifier = modifier) {
        ProductDetailContent(
            uiState = uiState,
            onEvent = { event ->
                if (event is ProductDetailUiEvent.ActivateMic) activateMic() else viewModel.onEvent(event)
            },
            modifier = Modifier.fillMaxSize(),
        )
        CartFeedbackHost(
            state = cartFeedback,
            // Above the bottom search bar.
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 120.dp),
        )
    }
}
