package com.qhathia.mobile.discovery.feature.settings.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.unit.dp
import com.qhathia.mobile.discovery.core.ui.theme.DiscoveryColors
import com.qhathia.mobile.discovery.core.ui.tokens.Spacing
import com.qhathia.mobile.discovery.feature.settings.R
import com.qhathia.mobile.discovery.feature.settings.model.SettingsSection
import com.qhathia.mobile.discovery.core.ui.R as UiR

/**
 * Shared fixed dark side menu for the settings area (Figma "Red Wifi" / "Comercio vinculado"): a back
 * affordance, the store title, and the Wi-Fi / Vincular section items. Hosted once by [SettingsScreen]
 * so the selection shadow animates smoothly as the merchant switches sections.
 */
@Composable
fun SettingsSideMenu(
    selectedSection: SettingsSection,
    onSelectSection: (SettingsSection) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
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
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = stringResource(R.string.network_settings_back_cd),
                    tint = DiscoveryColors.Blue50,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = stringResource(R.string.network_back),
                    style = MaterialTheme.typography.labelLarge,
                    color = DiscoveryColors.Blue50,
                )
            }
        }
        Text(
            text = stringResource(R.string.network_store_title),
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
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
