package com.qhathia.mobile.discovery.core.domain

/**
 * Domain-level constants shared by the Discovery Home experience. Kept here so the listening
 * timeout has a single source of truth across presentation and tests.
 */
object HomeConstants {
    /** Maximum voice-search listening window, in seconds (business rule). */
    const val MIC_LISTENING_TIMEOUT_SECONDS: Int = 30
}
