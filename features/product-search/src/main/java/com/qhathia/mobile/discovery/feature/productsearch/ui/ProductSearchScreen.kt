package com.qhathia.mobile.discovery.feature.productsearch.ui

import androidx.compose.ui.platform.LocalResources
import com.qhathia.mobile.discovery.feature.productsearch.R
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
import com.qhathia.mobile.discovery.core.domain.model.SttErrorCategory
import com.qhathia.mobile.discovery.core.ui.R as UiR
import com.qhathia.mobile.discovery.core.ui.permission.rememberMicPermissionGate
import com.qhathia.mobile.discovery.feature.productsearch.model.ProductSearchUiEffect
import com.qhathia.mobile.discovery.feature.productsearch.model.ProductSearchUiEvent

/**
 * Stateful product-search entry point. Binds [ProductSearchViewModel], collects its state with
 * lifecycle awareness, and forwards one-shot navigation effects to the app.
 *
 * [previousIsHome] tells where Volver goes with no previous result (F-38): from Home it opens the
 * close-search dialog; from anywhere else it calls [onNavigateBack] and the cart is kept.
 */
@Composable
fun ProductSearchScreen(
    onNavigateHomeIdle: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToCatalog: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onOpenCart: () -> Unit,
    previousIsHome: () -> Boolean,
    modifier: Modifier = Modifier,
    viewModel: ProductSearchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lineLimitMessage = stringResource(UiR.string.discovery_cart_line_limit)
    val limitOk = stringResource(UiR.string.discovery_cart_limit_ok)
    val viewCart = stringResource(UiR.string.discovery_cart_view)
    val resources = LocalResources.current
    val cartFeedback = remember { CartFeedbackState() }
    val feedbackScope = rememberCoroutineScope()
    // F-17: a Toast, like the microphone-permission notice (VOZ-008), so it outlives a NavigateBack.
    val voiceNetworkMessage = stringResource(R.string.ps_voice_network)
    val voiceFailedMessage = stringResource(UiR.string.discovery_voice_failed)

    LaunchedEffect(viewModel) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                ProductSearchUiEffect.NavigateHomeIdle -> onNavigateHomeIdle()
                ProductSearchUiEffect.NavigateBack -> onNavigateBack()
                ProductSearchUiEffect.ExitRequested ->
                    if (previousIsHome()) viewModel.onEvent(ProductSearchUiEvent.NewSearch) else onNavigateBack()
                ProductSearchUiEffect.NavigateToCatalog -> onNavigateToCatalog()
                is ProductSearchUiEffect.NavigateToDetail -> onNavigateToDetail(effect.productId)
                ProductSearchUiEffect.OpenCart -> onOpenCart()
                ProductSearchUiEffect.LineLimitReached ->
                    feedbackScope.launch { cartFeedback.showLimit(lineLimitMessage, limitOk) }
                is ProductSearchUiEffect.AddedToCart -> {
                    val message = resources.getQuantityString(
                        UiR.plurals.discovery_cart_added,
                        effect.quantity,
                        effect.productName,
                        effect.quantity,
                    )
                    feedbackScope.launch {
                        if (cartFeedback.showAdded(message, viewCart)) viewModel.onEvent(ProductSearchUiEvent.OpenCart)
                    }
                }
                is ProductSearchUiEffect.VoiceFailed -> {
                    val message = if (effect.category == SttErrorCategory.Network) voiceNetworkMessage else voiceFailedMessage
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }
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

    Box(modifier = modifier) {
        ProductSearchContent(
            uiState = uiState,
            onEvent = { event ->
                if (event is ProductSearchUiEvent.ActivateMic) activateMic() else viewModel.onEvent(event)
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
