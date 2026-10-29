package com.qhathia.mobile.discovery.feature.pairing.model

import com.qhathia.mobile.discovery.core.domain.model.PairedDevice

/**
 * Immutable snapshot of the pairing (Vincular) section.
 *
 * @property phase the screen phase driving which layout is shown.
 * @property code the generated pairing code, present during [PairingPhase.WaitingForApproval].
 * @property remainingSeconds remaining TTL of the current code (drives the countdown text).
 * @property totalSeconds the code's full lifetime at generation, captured from the server's
 *   `expiresAt`; the progress bar fills against this so it starts full regardless of the actual TTL.
 * @property device the paired-device details, present during [PairingPhase.Paired].
 * @property showUnpairConfirmation whether the unpair confirmation dialog is visible.
 * @property error what failed, present during [PairingPhase.Error].
 */
data class PairingUiState(
    val phase: PairingPhase = PairingPhase.Loading,
    val code: String? = null,
    val remainingSeconds: Long = 0L,
    val totalSeconds: Long = 0L,
    val device: PairedDevice? = null,
    val showUnpairConfirmation: Boolean = false,
    val error: PairingError? = null,
)

/** What failed in [PairingPhase.Error]; each has its own message and action. */
enum class PairingError {
    /** No code could be generated; retry. */
    CodeGeneration,

    /** The approved code could not be exchanged for the device token; a new code is needed. */
    Linking,
}

/** Mutually exclusive phases of the pairing section. */
enum class PairingPhase {
    /** Requesting a code or loading paired details. */
    Loading,

    /** A code is shown and the app is waiting for the merchant to approve it. */
    WaitingForApproval,

    /** The merchant rejected the code; offer to generate a new one. */
    Rejected,

    /** The code expired; a fresh code is being requested. */
    Expired,

    /** The device is paired; the paired-device details are shown. */
    Paired,

    /** A request failed; offer a retry. */
    Error,
}
