package com.qhathia.mobile.discovery.feature.settings.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.qhathia.mobile.discovery.core.ui.component.labeled
import com.qhathia.mobile.discovery.core.ui.theme.DiscoveryColors
import com.qhathia.mobile.discovery.core.ui.theme.DiscoveryTheme
import com.qhathia.mobile.discovery.core.ui.tokens.Spacing
import com.qhathia.mobile.discovery.feature.settings.R
import com.qhathia.mobile.discovery.feature.settings.model.KioskExitUiState
import com.qhathia.mobile.discovery.feature.settings.model.SettingsSection
import com.qhathia.mobile.discovery.core.ui.R as UiR

/**
 * Shared fixed dark side menu for the settings area (Figma "Red Wifi" / "Comercio vinculado"): a back
 * affordance, the store title ([storeName], "Tienda" when `null`), and the Wi-Fi / Vincular section items. Hosted once by [SettingsScreen]
 * so the selection shadow animates smoothly as the merchant switches sections.
 *
 * While the kiosk lock is active it also offers "Salir del modo kiosko" at the bottom, with a hint that
 * a new Wi-Fi network can only be approved outside kiosk mode; after exiting it shows that the lock
 * returns on going back to Home.
 */
@Composable
fun SettingsSideMenu(
    selectedSection: SettingsSection,
    onSelectSection: (SettingsSection) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    storeName: String? = null,
    kioskExit: KioskExitUiState = KioskExitUiState(),
    onExitKioskMode: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .width(SIDE_MENU_WIDTH)
            .fillMaxHeight()
            .background(DiscoveryColors.Blue800)
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onBack)
                    .labeled(stringResource(UiR.string.discovery_cd_back))
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = null, // the label is on the button itself
                    tint = DiscoveryColors.Blue50,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = stringResource(UiR.string.discovery_action_back),
                    style = MaterialTheme.typography.labelLarge,
                    color = DiscoveryColors.Blue50,
                )
            }
        }
        Text(
            text = storeName ?: stringResource(R.string.network_store_title),
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
            SideMenuItem(
                iconRes = R.drawable.ic_wifi,
                label = stringResource(R.string.network_section_wifi),
                selected = selectedSection == SettingsSection.Wifi,
                onClick = { onSelectSection(SettingsSection.Wifi) },
            )
            SideMenuItem(
                iconRes = UiR.drawable.ic_refresh,
                label = stringResource(R.string.network_section_link),
                selected = selectedSection == SettingsSection.Pairing,
                onClick = { onSelectSection(SettingsSection.Pairing) },
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        KioskExitBlock(state = kioskExit, onExitKioskMode = onExitKioskMode)
    }
}

@Composable
private fun KioskExitBlock(
    state: KioskExitUiState,
    onExitKioskMode: () -> Unit,
) {
    when {
        state.isLockActive -> Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(
                text = stringResource(R.string.kiosk_exit_wifi_hint),
                style = MaterialTheme.typography.bodySmall,
                color = DiscoveryColors.Blue50,
            )
            SideMenuItem(
                iconRes = R.drawable.ic_lock,
                label = stringResource(R.string.kiosk_exit_action),
                selected = false,
                onClick = onExitKioskMode,
            )
        }
        state.hasExited -> Text(
            text = stringResource(R.string.kiosk_exit_done),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
        )
    }
}

@Composable
private fun SideMenuItem(
    iconRes: Int,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    // Animate the selection shadow when switching sections (Wi-Fi ↔ Vincular).
    val background by animateColorAsState(
        targetValue = if (selected) DiscoveryColors.Blue500 else Color.Transparent,
        animationSpec = tween(durationMillis = 250),
        label = "sideMenuSelection",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .clickable(onClick = onClick)
            .labeled(label)
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            ),
            color = Color.White,
        )
    }
}

private val SIDE_MENU_WIDTH = 320.dp

@Preview(name = "Side menu — kiosk locked (light)", widthDp = 320, heightDp = 640)
@Composable
private fun SettingsSideMenuLockedPreview() {
    DiscoveryTheme(darkTheme = false) {
        SettingsSideMenu(
            selectedSection = SettingsSection.Wifi,
            onSelectSection = {},
            onBack = {},
            storeName = "QA Discovery — Ferretería",
            kioskExit = KioskExitUiState(isLockActive = true),
        )
    }
}

@Preview(name = "Side menu — kiosk exited (dark)", widthDp = 320, heightDp = 640)
@Composable
private fun SettingsSideMenuExitedPreview() {
    DiscoveryTheme(darkTheme = true) {
        SettingsSideMenu(
            selectedSection = SettingsSection.Wifi,
            onSelectSection = {},
            onBack = {},
            storeName = "QA Discovery — Ferretería",
            kioskExit = KioskExitUiState(isLockActive = false, hasExited = true),
        )
    }
}

@Preview(name = "Side menu — kiosk off (dark)", widthDp = 320, heightDp = 640)
@Composable
private fun SettingsSideMenuKioskOffPreview() {
    DiscoveryTheme(darkTheme = true) {
        SettingsSideMenu(selectedSection = SettingsSection.Pairing, onSelectSection = {}, onBack = {})
    }
}
