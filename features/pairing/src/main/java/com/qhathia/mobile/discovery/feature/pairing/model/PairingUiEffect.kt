package com.qhathia.mobile.discovery.feature.pairing.model

/** One-shot side-effects for the pairing section. */
sealed interface PairingUiEffect {

    /**
     * The device finished pairing successfully (an approved session whose device-token exchange
     * succeeded and was persisted). The host decides what this means — Settings keeps showing the
     * paired-device details in place, onboarding advances to Home.
     */
    data object Paired : PairingUiEffect

    /** Server-side unpairing is not available yet (no API); inform the merchant. */
    data object UnpairUnavailable : PairingUiEffect
}
