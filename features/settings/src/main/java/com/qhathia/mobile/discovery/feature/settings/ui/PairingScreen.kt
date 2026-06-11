package com.qhathia.mobile.discovery.feature.settings.ui

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qhathia.mobile.discovery.feature.settings.R
import com.qhathia.mobile.discovery.feature.settings.model.PairingUiEffect
import com.qhathia.mobile.discovery.feature.settings.model.PairingUiEvent

/**
 * Stateful pairing (Vincular) entry point. Binds [PairingViewModel] and ties the code generation /
 * status subscription to composition: [PairingViewModel.start] runs when the section becomes visible
 * and [PairingViewModel.stop] tears the subscription down when the merchant switches away. Overlays the
 * unpair confirmation dialog and surfaces the "unpairing unavailable" notice.
 */
@Composable
fun PairingScreen(
    modifier: Modifier = Modifier,
    viewModel: PairingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    DisposableEffect(viewModel) {
        viewModel.start()
        onDispose { viewModel.stop() }
    }

    LaunchedEffect(viewModel) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                PairingUiEffect.UnpairUnavailable ->
                    Toast.makeText(
                        context,
                        R.string.pairing_unpair_unavailable,
                        Toast.LENGTH_LONG,
                    ).show()
            }
        }
    }

    PairingContent(uiState = uiState, onEvent = viewModel::onEvent, modifier = modifier)

    if (uiState.showUnpairConfirmation) {
        UnpairConfirmationDialog(
            onConfirm = { viewModel.onEvent(PairingUiEvent.UnpairConfirmed) },
            onDismiss = { viewModel.onEvent(PairingUiEvent.UnpairDismissed) },
        )
    }
}
