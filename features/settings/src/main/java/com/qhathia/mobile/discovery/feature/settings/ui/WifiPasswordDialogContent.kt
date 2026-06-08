package com.qhathia.mobile.discovery.feature.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.qhathia.mobile.discovery.core.domain.model.WifiNetwork
import com.qhathia.mobile.discovery.core.ui.theme.DiscoveryColors
import com.qhathia.mobile.discovery.core.ui.theme.DiscoveryTheme
import com.qhathia.mobile.discovery.core.ui.tokens.Spacing
import com.qhathia.mobile.discovery.feature.settings.R
import com.qhathia.mobile.discovery.feature.settings.model.NetworkSettingsUiEvent
import com.qhathia.mobile.discovery.core.ui.R as UiR

/**
 * Stateless Wi-Fi password dialog UI for a secured network: a close "X" in the top-right, a ~56dp
 * Wi-Fi icon, the network's name, an obscured password field, and a submit button that connects. A
 * connecting spinner and an inline error reflect the connection outcome. A pure function of its
 * inputs, so it is previewable.
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
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, DiscoveryColors.Divider, RoundedCornerShape(10.dp))
                        .clickable { onEvent(NetworkSettingsUiEvent.DismissWifiDialog) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(UiR.drawable.ic_close),
                        contentDescription = stringResource(R.string.wifi_password_close_cd),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(14.dp),
                    )
                }
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

            PasswordField(
                value = password,
                isError = connectionError,
                onValueChange = { onEvent(NetworkSettingsUiEvent.WifiPasswordChanged(it)) },
            )

            if (connectionError) {
                Text(
                    text = stringResource(R.string.network_connect_error),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            SubmitButton(
                isConnecting = isConnecting,
                onClick = { onEvent(NetworkSettingsUiEvent.SubmitWifiPassword) },
            )
        }
    }
}

@Composable
private fun PasswordField(
    value: String,
    isError: Boolean,
    onValueChange: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(
            text = stringResource(R.string.wifi_password_label),
            style = MaterialTheme.typography.bodySmall,
            color = DiscoveryColors.TextMuted,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(
                    width = 1.dp,
                    color = if (isError) MaterialTheme.colorScheme.error else DiscoveryColors.Divider,
                    shape = RoundedCornerShape(8.dp),
                )
                .padding(horizontal = Spacing.sm),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (value.isEmpty()) {
                Text(
                    text = stringResource(R.string.wifi_password_placeholder),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = PasswordVisualTransformation(),
                textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SubmitButton(
    isConnecting: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(DiscoveryColors.Blue700)
            .clickable(enabled = !isConnecting, onClick = onClick)
            .padding(horizontal = Spacing.lg),
        contentAlignment = Alignment.Center,
    ) {
        if (isConnecting) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
        } else {
            Text(
                text = stringResource(R.string.wifi_password_submit),
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
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
