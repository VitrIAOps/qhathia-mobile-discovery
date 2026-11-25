package com.qhathia.mobile.discovery.core.data.session

/** Encrypts the device access token before it is stored. */
interface TokenCipher {
    fun encrypt(plaintext: String): String

    /**
     * Throws [UnreadableTokenException] when [ciphertext] can never be read on this device (wrong
     * or missing key, corrupted value). Any other exception is treated as temporary.
     */
    fun decrypt(ciphertext: String): String
}

/** The stored token can never be decrypted on this device. */
class UnreadableTokenException(cause: Throwable? = null) : Exception("Stored token cannot be decrypted", cause)
