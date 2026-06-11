package com.qhathia.mobile.discovery.feature.settings.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.qhathia.mobile.discovery.feature.settings.model.SettingsSection

/**
 * Settings host reached after the PIN gate. Owns the selected side-menu [SettingsSection] and composes
 * the shared dark side menu once (so the selection shadow animates) next to the section's panel —
 * Wi-Fi ("Red Wifi") or device pairing ("Vincular"). Back is hoisted to the caller.
 */
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var section by rememberSaveable { mutableStateOf(SettingsSection.Wifi) }

    Row(modifier = modifier.fillMaxSize()) {
        SettingsSideMenu(
            selectedSection = section,
            onSelectSection = { section = it },
            onBack = onNavigateBack,
        )
        Crossfade(
            targetState = section,
            modifier = Modifier.weight(1f).fillMaxHeight(),
            label = "settingsSection",
        ) { current ->
            when (current) {
                SettingsSection.Wifi -> NetworkSettingsScreen(onNavigateBack = onNavigateBack)
                SettingsSection.Pairing -> PairingScreen()
            }
        }
    }
}
