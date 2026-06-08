package com.qhathia.mobile.discovery.feature.settings.navigation

import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.qhathia.mobile.discovery.feature.settings.ui.NetworkSettingsScreen

/**
 * Stable route for the Network settings screen — the settings landing reached after the PIN gate. The
 * PIN gate itself is an app-hosted overlay (see `SettingsPasswordGate`), not a navigation destination,
 * so the merchant never sees settings content before entering the correct PIN.
 */
const val NETWORK_SETTINGS_ROUTE: String = "network_settings"

/**
 * Registers the Network settings destination. The back navigation is hoisted as a callback so the app
 * pops the destination.
 */
fun NavGraphBuilder.networkSettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    composable(route = NETWORK_SETTINGS_ROUTE) {
        NetworkSettingsScreen(
            onNavigateBack = onNavigateBack,
            modifier = modifier,
        )
    }
}
