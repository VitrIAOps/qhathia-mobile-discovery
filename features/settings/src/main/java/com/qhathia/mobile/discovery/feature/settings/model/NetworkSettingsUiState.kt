package com.qhathia.mobile.discovery.feature.settings.model

import com.qhathia.mobile.discovery.core.domain.model.WifiNetwork
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Immutable snapshot of the Network settings screen.
 *
 * @property wifiEnabled whether Wi-Fi is on (when off, the network list is hidden).
 * @property mobileDataEnabled whether mobile data is active.
 * @property airplaneModeActive whether airplane mode is on (blocks selecting/changing a network).
 * @property networks the accessible Wi-Fi networks.
 * @property selectedSecuredNetwork the secured network awaiting a password (drives the Wi-Fi dialog).
 * @property wifiPasswordInput the password typed into the Wi-Fi dialog.
 * @property isConnecting whether a connection attempt is in progress.
 * @property connectionError whether the last connection attempt failed.
 * @property actionNotPermitted whether the last toggle was rejected by the OS.
 * @property locationPermissionGranted whether location access (required for scans) was granted.
 */
data class NetworkSettingsUiState(
    val wifiEnabled: Boolean = false,
    val mobileDataEnabled: Boolean = false,
    val airplaneModeActive: Boolean = false,
    val networks: ImmutableList<WifiNetwork> = persistentListOf(),
    val selectedSecuredNetwork: WifiNetwork? = null,
    val wifiPasswordInput: String = "",
    val isConnecting: Boolean = false,
    val connectionError: Boolean = false,
    val actionNotPermitted: Boolean = false,
    val locationPermissionGranted: Boolean = false,
) {
    /** A network can be selected/changed only when airplane mode is off (business rule). */
    val selectionEnabled: Boolean get() = !airplaneModeActive

    /** The Wi-Fi password dialog is shown while a secured network is selected. */
    val showWifiPasswordDialog: Boolean get() = selectedSecuredNetwork != null

    /** The list is gated behind Wi-Fi being on and location being granted. */
    val showLocationRequired: Boolean get() = wifiEnabled && !locationPermissionGranted
}
