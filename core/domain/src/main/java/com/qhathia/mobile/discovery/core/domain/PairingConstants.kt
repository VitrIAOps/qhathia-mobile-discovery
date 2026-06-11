package com.qhathia.mobile.discovery.core.domain

/** Business constants for the device pairing flow (ClickUp 86e1kbt2d). */
object PairingConstants {

    /** Number of characters in a generated pairing code. */
    const val CODE_LENGTH: Int = 5

    /** Maximum lifetime of a generated code, in seconds (5 minutes). */
    const val CODE_TTL_SECONDS: Long = 5 * 60
}
