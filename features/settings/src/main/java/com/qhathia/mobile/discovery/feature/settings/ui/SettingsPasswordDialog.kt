package com.qhathia.mobile.discovery.feature.settings.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.qhathia.mobile.discovery.core.ui.component.reportUserActivity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.qhathia.mobile.discovery.feature.settings.model.SettingsUiEvent
import com.qhathia.mobile.discovery.feature.settings.model.SettingsUiState

/**
 * Hosts [SettingsPasswordDialogContent] in a Compose [Dialog]. The platform default width is disabled
 * so the dialog can be sized to ~30% of the screen (a compact PIN pad for the landscape kiosk) and
 * centered. Scrim/back dismissal maps to [SettingsUiEvent.DismissDialog] so the host leaves the
 * settings area without unlocking.
 */
@Composable
fun SettingsPasswordDialog(
    uiState: SettingsUiState,
    onEvent: (SettingsUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Dialog(
        onDismissRequest = { onEvent(SettingsUiEvent.DismissDialog) },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        // A dialog is its own window: report its touches to the inactivity timer.
        Box(modifier = Modifier.fillMaxSize().reportUserActivity(), contentAlignment = Alignment.Center) {
            SettingsPasswordDialogContent(
                uiState = uiState,
                onEvent = onEvent,
                // Cap the dialog at ~30% of the screen (≈120dp keys on the kiosk); keep a floor so it
                // stays usable on narrow screens.
                modifier = modifier
                    .fillMaxWidth(DIALOG_WIDTH_FRACTION)
                    .widthIn(min = 300.dp),
            )
        }
    }
}

private const val DIALOG_WIDTH_FRACTION = 0.3f
