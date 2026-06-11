package com.qhathia.mobile.discovery.feature.settings.model

/** One-shot side-effects for the pairing section. */
sealed interface PairingUiEffect {

    /** Server-side unpairing is not available yet (no API); inform the merchant. */
    data object UnpairUnavailable : PairingUiEffect
}
