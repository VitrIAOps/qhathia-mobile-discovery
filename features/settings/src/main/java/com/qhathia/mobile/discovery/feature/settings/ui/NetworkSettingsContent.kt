package com.qhathia.mobile.discovery.feature.settings.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.qhathia.mobile.discovery.core.domain.model.WifiNetwork
import com.qhathia.mobile.discovery.core.ui.component.DiscoveryDivider
import com.qhathia.mobile.discovery.core.ui.component.labeled
import com.qhathia.mobile.discovery.core.ui.theme.DiscoveryColors
import com.qhathia.mobile.discovery.core.ui.theme.DiscoveryTheme
import com.qhathia.mobile.discovery.core.ui.tokens.Spacing
import com.qhathia.mobile.discovery.feature.settings.R
import com.qhathia.mobile.discovery.feature.settings.model.NetworkSettingsUiEvent
import com.qhathia.mobile.discovery.feature.settings.model.NetworkSettingsUiState
import kotlinx.collections.immutable.persistentListOf
import com.qhathia.mobile.discovery.core.ui.R as UiR

/**
 * Stateless Network settings UI — a pure function of [NetworkSettingsUiState], following the Figma DS
 * "Red Wifi" node main panel: a toolbar ("TIENDA" + "Buscar redes"), heading, the kept mobile-data /
 * Wi-Fi controls, and the accessible-network list (connected network highlighted with a badge +
 * "CONECTADO" chip). The shared dark side menu is hosted by `SettingsScreen`, not here. The
 * airplane-mode, not-permitted, Wi-Fi-off and location-required states use the shared design system.
 */
@Composable
fun NetworkSettingsContent(
    uiState: NetworkSettingsUiState,
    onEvent: (NetworkSettingsUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    MainPanel(
        uiState = uiState,
        onEvent = onEvent,
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    )
}

// --- Main panel ---

@Composable
private fun MainPanel(
    uiState: NetworkSettingsUiState,
    onEvent: (NetworkSettingsUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.xl)
            .padding(top = 40.dp, bottom = Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        Toolbar(onSearch = { onEvent(NetworkSettingsUiEvent.RefreshNetworks) })

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Text(
                text = stringResource(R.string.network_heading),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(R.string.network_subheading),
                style = MaterialTheme.typography.bodyMedium,
                color = DiscoveryColors.TextMuted,
            )
        }

        // Kept controls (not in the Figma but required): mobile data above Wi-Fi. Mobile data is a
        // status only: Android does not let the app change it, the button opens Android's screen.
        MobileDataRow(
            enabled = uiState.mobileDataEnabled,
            simAvailable = uiState.simAvailable,
            onOpenSettings = { onEvent(NetworkSettingsUiEvent.OpenMobileDataSettings) },
        )
        ToggleRow(
            iconRes = R.drawable.ic_wifi,
            label = stringResource(R.string.network_wifi),
            checked = uiState.wifiEnabled,
            onCheckedChange = { onEvent(NetworkSettingsUiEvent.ToggleWifi(it)) },
        )

        if (uiState.airplaneModeActive) {
            InfoBanner(text = stringResource(R.string.network_airplane_mode_active))
        }
        if (uiState.actionNotPermitted) {
            InfoBanner(text = stringResource(R.string.network_action_not_permitted))
        }

        when {
            !uiState.wifiEnabled -> InfoBanner(text = stringResource(R.string.network_wifi_off))
            uiState.showLocationRequired -> LocationRequired(
                blocked = uiState.locationPermissionBlocked,
                onAllow = { onEvent(NetworkSettingsUiEvent.RequestLocationPermission) },
            )
            else -> AvailableNetworks(uiState = uiState, onEvent = onEvent)
        }
    }
}

@Composable
private fun Toolbar(onSearch: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.network_toolbar_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onSearch)
                    .labeled(stringResource(R.string.network_search))
                    .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Icon(
                    painter = painterResource(UiR.drawable.ic_refresh),
                    contentDescription = null,
                    tint = DiscoveryColors.CatalogText,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = stringResource(R.string.network_search),
                    style = MaterialTheme.typography.labelLarge,
                    color = DiscoveryColors.CatalogText,
                )
            }
        }
        DiscoveryDivider(color = DiscoveryColors.Blue200)
    }
}

@Composable
private fun ToggleRow(
    iconRes: Int,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, DiscoveryColors.Divider),
    ) {
        // The whole row is the switch, so the touchable element carries the label.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .toggleable(value = checked, onValueChange = onCheckedChange, role = Role.Switch)
                .labeled(label, role = null)
                .padding(horizontal = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Switch(checked = checked, onCheckedChange = null)
        }
    }
}

/**
 * "Datos móviles" as a status ("Activos" / "Inactivos", or "Sin SIM") plus "Abrir ajustes de datos",
 * which opens Android's own mobile-data screen. No button without a SIM.
 */
@Composable
private fun MobileDataRow(
    enabled: Boolean,
    simAvailable: Boolean,
    onOpenSettings: () -> Unit,
) {
    val status = stringResource(
        when {
            !simAvailable -> R.string.network_mobile_data_no_sim
            enabled -> R.string.network_mobile_data_active
            else -> R.string.network_mobile_data_inactive
        },
    )
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, DiscoveryColors.Divider),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_signal),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(
                    text = stringResource(R.string.network_mobile_data),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodySmall,
                    color = DiscoveryColors.TextMuted,
                )
            }
            if (simAvailable) {
                LinkButton(text = stringResource(R.string.network_mobile_data_open), onClick = onOpenSettings)
            }
        }
    }
}

/**
 * Location is needed to list networks: a message plus "Permitir ubicación", or, when Android no longer
 * shows the request, how to allow it from the device settings.
 */
@Composable
private fun LocationRequired(blocked: Boolean, onAllow: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        InfoBanner(text = stringResource(R.string.network_location_required))
        if (blocked) {
            InfoBanner(text = stringResource(R.string.network_location_blocked))
        } else {
            LinkButton(text = stringResource(R.string.network_location_allow), onClick = onAllow)
        }
    }
}

/** A text action in the "Buscar redes" style. */
@Composable
private fun LinkButton(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = DiscoveryColors.CatalogText,
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, DiscoveryColors.Blue200, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .labeled(text)
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
    )
}

@Composable
private fun AvailableNetworks(
    uiState: NetworkSettingsUiState,
    onEvent: (NetworkSettingsUiEvent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.network_available_caps),
                style = MaterialTheme.typography.bodySmall,
                color = DiscoveryColors.TextMuted,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = pluralStringResource(
                    R.plurals.network_found_count,
                    uiState.networks.size,
                    uiState.networks.size,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = DiscoveryColors.TextMuted,
            )
        }

        if (uiState.isConnecting && !uiState.showWifiPasswordDialog) {
            ConnectingRow()
        }

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, DiscoveryColors.Neutral300),
        ) {
            Column {
                uiState.networks.forEachIndexed { index, network ->
                    NetworkRow(
                        network = network,
                        enabled = uiState.selectionEnabled,
                        onClick = { onEvent(NetworkSettingsUiEvent.SelectNetwork(network)) },
                    )
                    if (index < uiState.networks.lastIndex) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(DIVIDER_THICKNESS)
                                .background(DiscoveryColors.Blue200),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NetworkRow(
    network: WifiNetwork,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val securityText = if (network.isSecured) {
        stringResource(R.string.network_row_secured)
    } else {
        stringResource(R.string.network_row_open)
    }
    // One label for the whole row: name, security and, when it applies, the connected state.
    val rowLabel = listOfNotNull(
        network.ssid,
        securityText,
        if (network.isConnected) stringResource(R.string.network_connected) else null,
    ).joinToString(", ")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (network.isConnected) DiscoveryColors.Blue50 else MaterialTheme.colorScheme.surfaceVariant)
            .clickable(enabled = enabled, onClick = onClick)
            .labeled(rowLabel)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            NetworkBadge(connected = network.isConnected)
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(
                    text = network.ssid,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else DiscoveryColors.TextDisabled,
                )
                Text(
                    text = securityText,
                    style = MaterialTheme.typography.bodySmall,
                    color = DiscoveryColors.TextMuted,
                )
            }
            if (network.isConnected) {
                ConnectedChip()
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            if (network.isSecured) {
                Icon(
                    painter = painterResource(R.drawable.ic_lock),
                    contentDescription = null, // part of the row label
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
            Icon(
                painter = painterResource(R.drawable.ic_signal),
                contentDescription = null, // decorative; the row label names the network
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun NetworkBadge(connected: Boolean) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (connected) DiscoveryColors.Green300 else DiscoveryColors.Teal50)
            .then(
                if (connected) Modifier else Modifier.border(1.dp, DiscoveryColors.Divider, CircleShape),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(if (connected) R.drawable.ic_check else R.drawable.ic_wifi),
            contentDescription = null,
            tint = if (connected) Color.White else DiscoveryColors.Teal400,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun ConnectedChip() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, DiscoveryColors.Green400, RoundedCornerShape(4.dp))
            .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
    ) {
        Text(
            text = stringResource(R.string.network_connected_chip),
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = DiscoveryColors.Green600,
        )
    }
}

@Composable
private fun ConnectingRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = stringResource(R.string.network_connecting),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun InfoBanner(text: String) {
    Surface(shape = RoundedCornerShape(12.dp), color = DiscoveryColors.Blue50) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = DiscoveryColors.CatalogText,
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
        )
    }
}

private val DIVIDER_THICKNESS = 1.dp

// --- Previews (landscape kiosk) ---

private fun sampleNetworks() = persistentListOf(
    WifiNetwork(ssid = "Tienda_Caja", isSecured = true, signalLevel = 4, isConnected = true),
    WifiNetwork(ssid = "Vecino-2.4G", isSecured = true, signalLevel = 2),
    WifiNetwork(ssid = "Plaza-Libre", isSecured = false, signalLevel = 3),
)

@Preview(name = "Network settings — light", showBackground = true, widthDp = 1280, heightDp = 800)
@Composable
private fun NetworkSettingsLightPreview() {
    DiscoveryTheme(darkTheme = false) {
        NetworkSettingsContent(
            uiState = NetworkSettingsUiState(
                wifiEnabled = true,
                mobileDataEnabled = false,
                simAvailable = true,
                networks = sampleNetworks(),
                locationPermissionGranted = true,
            ),
            onEvent = {},
        )
    }
}

@Preview(name = "Network settings — data on, location missing (dark)", showBackground = true, widthDp = 1280, heightDp = 800)
@Composable
private fun NetworkSettingsLocationMissingPreview() {
    DiscoveryTheme(darkTheme = true) {
        NetworkSettingsContent(
            uiState = NetworkSettingsUiState(
                wifiEnabled = true,
                mobileDataEnabled = true,
                simAvailable = true,
            ),
            onEvent = {},
        )
    }
}

@Preview(name = "Network settings — no SIM, location blocked (light)", showBackground = true, widthDp = 1280, heightDp = 800)
@Composable
private fun NetworkSettingsLocationBlockedPreview() {
    DiscoveryTheme(darkTheme = false) {
        NetworkSettingsContent(
            uiState = NetworkSettingsUiState(
                wifiEnabled = true,
                simAvailable = false,
                locationPermissionBlocked = true,
            ),
            onEvent = {},
        )
    }
}

@Preview(name = "Network settings — airplane (dark)", showBackground = true, widthDp = 1280, heightDp = 800)
@Composable
private fun NetworkSettingsAirplanePreview() {
    DiscoveryTheme(darkTheme = true) {
        NetworkSettingsContent(
            uiState = NetworkSettingsUiState(
                wifiEnabled = true,
                airplaneModeActive = true,
                networks = sampleNetworks(),
                locationPermissionGranted = true,
            ),
            onEvent = {},
        )
    }
}
