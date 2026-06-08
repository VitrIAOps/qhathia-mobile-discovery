package com.qhathia.mobile.discovery.feature.settings.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qhathia.mobile.discovery.feature.settings.model.NetworkSettingsUiEffect
import com.qhathia.mobile.discovery.feature.settings.model.NetworkSettingsUiEvent

/**
 * Stateful Network settings entry point. Binds [NetworkSettingsViewModel], requests the runtime
 * `ACCESS_FINE_LOCATION` permission on entry (Wi-Fi scans require it; mirrors how `MainActivity`
 * requests `RECORD_AUDIO`), forwards the result to the ViewModel, and overlays the Wi-Fi password
 * dialog while a secured network is selected.
 */
@Composable
fun NetworkSettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NetworkSettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.onEvent(NetworkSettingsUiEvent.LocationPermissionResult(granted))
    }

    LaunchedEffect(Unit) {
        val granted = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) {
            viewModel.onEvent(NetworkSettingsUiEvent.LocationPermissionResult(true))
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                NetworkSettingsUiEffect.NavigateBack -> onNavigateBack()
            }
        }
    }

    NetworkSettingsContent(uiState = uiState, onEvent = viewModel::onEvent, modifier = modifier)

    val selected = uiState.selectedSecuredNetwork
    if (selected != null) {
        WifiPasswordDialog(
            network = selected,
            password = uiState.wifiPasswordInput,
            isConnecting = uiState.isConnecting,
            connectionError = uiState.connectionError,
            onEvent = viewModel::onEvent,
        )
    }
}
