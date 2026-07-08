package com.qhathia.mobile.discovery.feature.pairing.ui

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qhathia.mobile.discovery.feature.pairing.R
import com.qhathia.mobile.discovery.feature.pairing.model.PairingUiEffect
import com.qhathia.mobile.discovery.feature.pairing.model.PairingUiEvent

/**
 * Stateful pairing (Vincular) entry point. Binds [PairingViewModel] and ties the code generation /
 * status subscription to composition: [PairingViewModel.start] runs when the section becomes visible
 * and [PairingViewModel.stop] tears the subscription down when the merchant switches away. Overlays the
 * unpair confirmation dialog and surfaces the "unpairing unavailable" notice.
 *
 * The slice is host-agnostic. The host supplies the header [title] ("TIENDA" in Settings, "QHATHIA" in
 * onboarding), whether a back affordance is shown ([showBackButton] / [onBack]), and what a successful
 * pairing means ([onPaired] — a no-op in Settings, "navigate to Home" in onboarding).
 *
 * @param title header title shown at the top of the section.
 * @param showBackButton whether a back affordance is rendered in the header.
 * @param onBack invoked when the back affordance is activated.
 * @param onPaired invoked once pairing completes successfully (device token persisted).
 */
@Composable
fun PairingScreen(
    title: String,
    modifier: Modifier = Modifier,
    showBackButton: Boolean = false,
    onBack: () -> Unit = {},
    onPaired: () -> Unit = {},
    viewModel: PairingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val currentOnPaired by rememberUpdatedState(onPaired)

    DisposableEffect(viewModel) {
        viewModel.start()
        onDispose { viewModel.stop() }
    }

    LaunchedEffect(viewModel) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                PairingUiEffect.Paired -> currentOnPaired()
                PairingUiEffect.UnpairUnavailable ->
                    Toast.makeText(
                        context,
                        R.string.pairing_unpair_unavailable,
                        Toast.LENGTH_LONG,
                    ).show()
            }
        }
    }

    PairingContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        title = title,
        showBackButton = showBackButton,
        onBack = onBack,
        modifier = modifier,
    )

    if (uiState.showUnpairConfirmation) {
        UnpairConfirmationDialog(
            onConfirm = { viewModel.onEvent(PairingUiEvent.UnpairConfirmed) },
            onDismiss = { viewModel.onEvent(PairingUiEvent.UnpairDismissed) },
        )
    }
}
