package com.qhathia.mobile.discovery.feature.pairing.model

/** User-driven intents on the pairing (Vincular) section. */
sealed interface PairingUiEvent {

    /** Request a fresh pairing code (retry after error/rejection/expiry). */
    data object GenerateCode : PairingUiEvent

    /** Open the unpair confirmation dialog. */
    data object UnpairClicked : PairingUiEvent

    /** Confirm unpairing in the dialog. */
    data object UnpairConfirmed : PairingUiEvent

    /** Dismiss the unpair confirmation dialog without unpairing. */
    data object UnpairDismissed : PairingUiEvent

    /** Retry loading the paired-device details after [PairingError.PairedDeviceLoad]. */
    data object RetryPairedDeviceLoad : PairingUiEvent
}
