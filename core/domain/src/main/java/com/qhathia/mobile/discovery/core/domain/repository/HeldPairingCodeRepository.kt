package com.qhathia.mobile.discovery.core.domain.repository

import com.qhathia.mobile.discovery.core.domain.model.HeldPairingCode

/**
 * Keeps the pairing code on screen for the life of the app process (design D6). The pairing screen's
 * state is destroyed when the merchant leaves it (for example "Volver" during onboarding), so the code
 * is held here to show it again when they come back while it is still valid.
 */
interface HeldPairingCodeRepository {

    /** The held code, or `null`. */
    fun get(): HeldPairingCode?

    /** Holds [code], or forgets the held one when `null`. */
    fun set(code: HeldPairingCode?)
}
