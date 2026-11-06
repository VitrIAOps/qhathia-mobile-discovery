package com.qhathia.mobile.discovery.feature.settings.ui

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qhathia.mobile.discovery.feature.settings.model.NetworkSettingsUiEffect
import com.qhathia.mobile.discovery.feature.settings.model.NetworkSettingsUiEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Stateful Network settings entry point. Binds [NetworkSettingsViewModel], requests the runtime
 * `ACCESS_FINE_LOCATION` permission on entry and on "Permitir ubicación" (Wi-Fi scans require it),
 * forwards the result to the ViewModel, opens Android's mobile-data screen when asked, re-reads the
 * network state on resume, and overlays the Wi-Fi password dialog while a secured network is selected.
 */
@Composable
fun NetworkSettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    viewModel: NetworkSettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = LocalActivity.current

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        // After a refusal Android stops asking once the rationale is no longer offered.
        val canAskAgain = granted ||
            activity?.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) == true
        viewModel.onEvent(NetworkSettingsUiEvent.LocationPermissionResult(granted, canAskAgain))
    }

    LifecycleResumeEffect(viewModel) {
        viewModel.onEvent(NetworkSettingsUiEvent.ScreenResumed)
        onPauseOrDispose {}
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
                NetworkSettingsUiEffect.RequestLocationPermission ->
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                NetworkSettingsUiEffect.OpenMobileDataSettings -> openMobileDataSettings(context)
            }
        }
    }

    NetworkSettingsContent(uiState = uiState, onEvent = viewModel::onEvent, modifier = modifier, title = title)

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

/**
 * Opens Android's mobile-data screen: the Internet panel on Android 10+, "Red e Internet" on 8–9 (one
 * step from mobile data; the mobile-data switch itself has no public action there). Kiosk mode was
 * left just before, but the task is unpinned by the activity a moment later; starting the screen while
 * still pinned is refused, so wait for it (about 2 s at most).
 */
private suspend fun openMobileDataSettings(context: Context) {
    val activityManager = context.getSystemService(ActivityManager::class.java)
    withTimeoutOrNull(UNPIN_WAIT_MILLIS) {
        while (activityManager?.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE) delay(UNPIN_POLL_MILLIS)
    }
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
    } else {
        Intent(Settings.ACTION_WIRELESS_SETTINGS)
    }
    runCatching { context.startActivity(intent) }
}

private const val UNPIN_WAIT_MILLIS = 2_000L
private const val UNPIN_POLL_MILLIS = 50L
