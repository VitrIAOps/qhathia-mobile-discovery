package com.qhathia.mobile.discovery.core.data.settings

import at.favre.lib.crypto.bcrypt.BCrypt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Checks a PIN against the BCrypt hash the backend stores (`$2a$`/`$2b$`, Spring Security). Runs off
 * the main thread: one verification takes a noticeable fraction of a second at cost 10.
 */
class ExitPinVerifier @Inject constructor() {

    /** `true` only when [pin] matches [hash]; a malformed hash is a mismatch, never an exception. */
    suspend fun matches(pin: String, hash: String): Boolean = withContext(Dispatchers.Default) {
        runCatching { BCrypt.verifyer().verify(pin.toCharArray(), hash.toCharArray()).verified }
            .getOrDefault(false)
    }
}
