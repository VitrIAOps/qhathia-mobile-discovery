package com.qhathia.mobile.discovery.feature.catalog.ui

import android.widget.Toast
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
import com.qhathia.mobile.discovery.feature.catalog.model.CatalogUiEffect
import com.qhathia.mobile.discovery.feature.catalog.model.CatalogUiEvent

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
    val context = LocalContext.current
    val lineLimitMessage = stringResource(UiR.string.discovery_cart_line_limit)

    LaunchedEffect(viewModel) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                CatalogUiEffect.NavigateHomeIdle -> onNavigateHomeIdle()
                is CatalogUiEffect.NavigateToDetail -> onNavigateToDetail(effect.productId)
                CatalogUiEffect.OpenCart -> onOpenCart()
                CatalogUiEffect.LineLimitReached ->
                    Toast.makeText(context, lineLimitMessage, Toast.LENGTH_LONG).show()
                is CatalogUiEffect.NavigateToSearch -> onNavigateToSearch(effect.query, effect.source)
            }
        }
    }

    // System Back runs the same action as "Volver" (wave 2, D2).
    BackHandler { viewModel.onEvent(CatalogUiEvent.Back) }

    // The search screen listens as soon as it opens, so the permission is asked here (wave 2, D4).
    val activateMic = rememberMicPermissionGate { viewModel.onEvent(CatalogUiEvent.ActivateMic) }

    CatalogContent(
        uiState = uiState,
        onEvent = { event -> if (event is CatalogUiEvent.ActivateMic) activateMic() else viewModel.onEvent(event) },
        modifier = modifier,
    )
}
