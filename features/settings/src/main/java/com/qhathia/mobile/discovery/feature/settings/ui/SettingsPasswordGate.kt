package com.qhathia.mobile.discovery.feature.settings.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qhathia.mobile.discovery.feature.settings.model.SettingsUiEffect
import com.qhathia.mobile.discovery.feature.settings.model.SettingsUiEvent
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

/**
 * Stateful settings PIN gate, hosted as an overlay (mirroring the cart dialog) so the PIN is requested
 * **before** navigating into settings — nothing of the settings area is shown until the PIN is correct.
 * When no exit PIN is set, it unlocks immediately without showing the dialog.
 * On a correct PIN it invokes [onUnlocked] (the host opens the Network settings screen); dismissing
 * invokes [onDismiss]. The gate resets on entry so it always re-prompts.
 */
@Composable
fun SettingsPasswordGate(
    onUnlocked: () -> Unit,
    onDismiss: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        // Subscribe before Reset: with no PIN set, Reset emits Unlocked right away, and the effect flow
        // has no replay, so an Unlocked emitted before collecting was lost and left the gate stuck open
        // (invisible), making the settings gear do nothing.
        launch(start = CoroutineStart.UNDISPATCHED) {
            viewModel.uiEffect.collect { effect ->
                when (effect) {
                    SettingsUiEffect.Unlocked -> onUnlocked()
                    SettingsUiEffect.NavigateBack -> onDismiss()
                }
            }
        }
        // Always start from a blank PIN each time the gate is shown.
        viewModel.onEvent(SettingsUiEvent.Reset)
    }

    // While checking whether a PIN is set (and when none is, as the gate unlocks at once) show nothing.
    if (!uiState.isResolvingAccess) {
        SettingsPasswordDialog(uiState = uiState, onEvent = viewModel::onEvent)
    }
}
