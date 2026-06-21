package com.qhathia.mobile.discovery.feature.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.qhathia.mobile.discovery.core.domain.model.WifiNetwork
import com.qhathia.mobile.discovery.core.ui.component.CloseIconButton
import com.qhathia.mobile.discovery.core.ui.component.DiscoveryTextField
import com.qhathia.mobile.discovery.core.ui.component.InlineErrorText
import com.qhathia.mobile.discovery.core.ui.component.PrimaryActionButton
import com.qhathia.mobile.discovery.core.ui.theme.DiscoveryTheme
import com.qhathia.mobile.discovery.core.ui.tokens.Spacing
import com.qhathia.mobile.discovery.feature.settings.R
import com.qhathia.mobile.discovery.feature.settings.model.NetworkSettingsUiEvent

/**
 * Stateless Wi-Fi password dialog UI for a secured network: a close "X" in the top-right, a ~56dp
 * Wi-Fi icon, the network's name, an obscured password field, and a submit button that connects. A
 * connecting spinner and an inline error reflect the connection outcome. A pure function of its
 * inputs, so it is previewable. Built from the shared `core:ui` design-system components.
 */
@Composable
fun WifiPasswordDialogContent(
    network: WifiNetwork,
    password: String,
    isConnecting: Boolean,
    connectionError: Boolean,
    onEvent: (NetworkSettingsUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                CloseIconButton(
                    onClick = { onEvent(NetworkSettingsUiEvent.DismissWifiDialog) },
                    contentDescription = stringResource(R.string.wifi_password_close_cd),
                )
            }

            Icon(
                painter = painterResource(R.drawable.ic_wifi),
                contentDescription = stringResource(R.string.network_wifi_icon_cd),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp),
            )

            Text(
                text = network.ssid,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )

            DiscoveryTextField(
                value = password,
                onValueChange = { onEvent(NetworkSettingsUiEvent.WifiPasswordChanged(it)) },
                label = stringResource(R.string.wifi_password_label),
                placeholder = stringResource(R.string.wifi_password_placeholder),
                isError = connectionError,
                keyboardType = KeyboardType.Password,
                visualTransformation = PasswordVisualTransformation(),
            )

            if (connectionError) {
                InlineErrorText(text = stringResource(R.string.network_connect_error))
            }

            PrimaryActionButton(
                text = stringResource(R.string.wifi_password_submit),
                onClick = { onEvent(NetworkSettingsUiEvent.SubmitWifiPassword) },
                isLoading = isConnecting,
            )
        }
    }
}

@Preview(name = "Wi-Fi password — light", showBackground = true, widthDp = 420)
@Composable
private fun WifiPasswordLightPreview() {
    DiscoveryTheme(darkTheme = false) {
        WifiPasswordDialogContent(
            network = WifiNetwork(ssid = "Qhathia-Store", isSecured = true, signalLevel = 4),
            password = "",
            isConnecting = false,
            connectionError = false,
            onEvent = {},
        )
    }
}

@Preview(name = "Wi-Fi password — error (dark)", showBackground = true, widthDp = 420)
@Composable
private fun WifiPasswordErrorPreview() {
    DiscoveryTheme(darkTheme = true) {
        WifiPasswordDialogContent(
            network = WifiNetwork(ssid = "Qhathia-Store", isSecured = true, signalLevel = 4),
            password = "wrongpass",
            isConnecting = false,
            connectionError = true,
            onEvent = {},
        )
    }
}
