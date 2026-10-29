package com.qhathia.mobile.discovery.feature.settings.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.qhathia.mobile.discovery.core.ui.component.reportUserActivity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.qhathia.mobile.discovery.core.domain.model.WifiNetwork
import com.qhathia.mobile.discovery.feature.settings.model.NetworkSettingsUiEvent

/**
 * Hosts [WifiPasswordDialogContent] in a Compose [Dialog] for the selected secured [network].
 * Scrim/back dismissal maps to [NetworkSettingsUiEvent.DismissWifiDialog] so no connection is made.
 */
@Composable
fun WifiPasswordDialog(
    network: WifiNetwork,
    password: String,
    isConnecting: Boolean,
    connectionError: Boolean,
    onEvent: (NetworkSettingsUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Dialog(onDismissRequest = { onEvent(NetworkSettingsUiEvent.DismissWifiDialog) }) {
        WifiPasswordDialogContent(
            network = network,
            password = password,
            isConnecting = isConnecting,
            connectionError = connectionError,
            onEvent = onEvent,
            // A dialog is its own window: report its touches to the inactivity timer.
            modifier = modifier
                .reportUserActivity()
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
        )
    }
}
