package com.qhathia.mobile.discovery.feature.productsearch.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qhathia.mobile.discovery.core.ui.R as UiR
import com.qhathia.mobile.discovery.core.ui.permission.rememberMicPermissionGate
import com.qhathia.mobile.discovery.feature.productsearch.model.ProductSearchUiEffect
import com.qhathia.mobile.discovery.feature.productsearch.model.ProductSearchUiEvent

/**
 * Stateful product-search entry point. Binds [ProductSearchViewModel], collects its state with
 * lifecycle awareness, and forwards one-shot navigation effects to the app.
 */
@Composable
fun ProductSearchScreen(
    onNavigateHomeIdle: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToCatalog: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onOpenCart: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProductSearchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lineLimitMessage = stringResource(UiR.string.discovery_cart_line_limit)

    LaunchedEffect(viewModel) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                ProductSearchUiEffect.NavigateHomeIdle -> onNavigateHomeIdle()
                ProductSearchUiEffect.NavigateBack -> onNavigateBack()
                ProductSearchUiEffect.NavigateToCatalog -> onNavigateToCatalog()
                is ProductSearchUiEffect.NavigateToDetail -> onNavigateToDetail(effect.productId)
                ProductSearchUiEffect.OpenCart -> onOpenCart()
                ProductSearchUiEffect.LineLimitReached ->
                    Toast.makeText(context, lineLimitMessage, Toast.LENGTH_LONG).show()
            }
        }
    }

    // Hidden screen: stop voice capture; shown again: finish a pending exit (wave 2, D3).
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.onEvent(ProductSearchUiEvent.ScreenStopped) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onEvent(ProductSearchUiEvent.ScreenResumed) }

    // System Back runs the same action as "Volver" (wave 2, D2).
    BackHandler { viewModel.onEvent(ProductSearchUiEvent.Back) }

    // The microphone needs the runtime permission, like on Home (wave 2, D4: VOZ-008).
    val activateMic = rememberMicPermissionGate { viewModel.onEvent(ProductSearchUiEvent.ActivateMic) }

    ProductSearchContent(
        uiState = uiState,
        onEvent = { event ->
            if (event is ProductSearchUiEvent.ActivateMic) activateMic() else viewModel.onEvent(event)
        },
        modifier = modifier,
    )
}
