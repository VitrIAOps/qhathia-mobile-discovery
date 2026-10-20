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
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qhathia.mobile.discovery.feature.pairing.ui.PairingScreen
import com.qhathia.mobile.discovery.feature.settings.R
import com.qhathia.mobile.discovery.feature.settings.model.KioskExitUiEvent
import com.qhathia.mobile.discovery.feature.settings.model.SettingsSection

/**
 * Settings host reached after the PIN gate. Owns the selected side-menu [SettingsSection] and composes
 * the shared dark side menu once (so the selection shadow animates) next to the section's panel —
 * Wi-Fi ("Red Wifi") or device pairing ("Vincular"). Back is hoisted to the caller.
 *
 * The pairing section is the shared `:features:pairing` slice, hosted here with the "TIENDA" title, the
 * side menu present and no back affordance; a successful pairing keeps the merchant in place (the slice
 * shows the paired-device details), so [PairingScreen]'s `onPaired` is a no-op.
 *
 * The side menu also carries "Salir del modo kiosko" ([KioskExitViewModel]) while the kiosk lock is on.
 */
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    kioskExitViewModel: KioskExitViewModel = hiltViewModel(),
) {
    val kioskExit by kioskExitViewModel.uiState.collectAsStateWithLifecycle()
    var section by rememberSaveable { mutableStateOf(SettingsSection.Wifi) }

    Row(modifier = modifier.fillMaxSize()) {
        SettingsSideMenu(
            selectedSection = section,
            onSelectSection = { section = it },
            onBack = onNavigateBack,
            kioskExit = kioskExit,
            onExitKioskMode = { kioskExitViewModel.onEvent(KioskExitUiEvent.ExitKioskMode) },
        )
        Crossfade(
            targetState = section,
            modifier = Modifier.weight(1f).fillMaxHeight(),
            label = "settingsSection",
        ) { current ->
            when (current) {
                SettingsSection.Wifi -> NetworkSettingsScreen(onNavigateBack = onNavigateBack)
                SettingsSection.Pairing -> PairingScreen(title = stringResource(R.string.network_toolbar_title))
            }
        }
    }
}
