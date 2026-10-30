package com.qhathia.mobile.discovery.feature.cart.ui

import android.view.WindowManager
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.qhathia.mobile.discovery.core.ui.component.KioskImmersiveDialogEffect
import com.qhathia.mobile.discovery.core.ui.component.reportUserActivity
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qhathia.mobile.discovery.feature.cart.model.CartUiEffect
import com.qhathia.mobile.discovery.feature.cart.model.CartUiEvent

/**
 * Stateful cart entry point. Binds [CartViewModel], collects its state with lifecycle awareness, and
 * renders the cart as a Compose [Dialog] over the current screen. Navigation and dismissal are hoisted
 * as callbacks so the app pops the overlay and routes to the product detail. Closing the dialog (the
 * "X", "Seguir buscando", system back, or a scrim tap) only dismisses — the basket is untouched. While
 * a request runs, closing is ignored; on a result screen a scrim tap or Back runs its main action.
 */
@Composable
fun CartDialog(
    onNavigateToDetail: (String) -> Unit,
    onDismiss: () -> Unit,
    onReturnHome: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CartViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is CartUiEffect.NavigateToDetail -> onNavigateToDetail(effect.productId)
                CartUiEffect.Dismiss -> onDismiss()
                CartUiEffect.ReturnHome -> onReturnHome()
            }
        }
    }

    Dialog(
        onDismissRequest = { viewModel.onEvent(CartUiEvent.DismissRequest) },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        // Kiosk: keep the system bars hidden over this dialog window and its keyboard.
        KioskImmersiveDialogEffect()
        // The cart must open WITHOUT popping the soft keyboard; the consumer taps a field to type.
        // A dialog auto-opens the keyboard because it gives the initial focus to the first focusable
        // element — the "Nombres" field — and a focused text field shows the IME. We prevent that by
        // (1) telling the dialog window to keep the IME hidden on entry, and (2) parking the initial
        // focus on an invisible focus trap so no text field is focused until the user taps it.
        val view = LocalView.current
        val focusTrap = remember { FocusRequester() }
        LaunchedEffect(Unit) {
            generateSequence(view.parent) { it.parent }
                .filterIsInstance<DialogWindowProvider>()
                .firstOrNull()
                ?.window
                ?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
            runCatching { focusTrap.requestFocus() }
        }

        // A dialog is its own window: report its touches to the inactivity timer (CAR-042).
        Box(modifier = Modifier.fillMaxSize().reportUserActivity(), contentAlignment = Alignment.Center) {
            // Invisible focus trap: takes the dialog's initial focus so no field auto-focuses.
            Box(
                modifier = Modifier
                    .size(1.dp)
                    .focusRequester(focusTrap)
                    .focusable(),
            )
            CartDialogContent(
                uiState = uiState,
                onEvent = viewModel::onEvent,
                modifier = modifier
                    .fillMaxWidth(CART_DIALOG_WIDTH_FRACTION)
                    .fillMaxHeight(CART_DIALOG_HEIGHT_FRACTION),
            )
        }
    }
}

private const val CART_DIALOG_WIDTH_FRACTION = 0.92f
private const val CART_DIALOG_HEIGHT_FRACTION = 0.9f
