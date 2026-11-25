package com.qhathia.mobile.discovery.core.data.session

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject

/**
 * [TokenCipher] backed by an AES-256/GCM key held in the Android Keystore, so the key material
 * never leaves secure hardware. The result is Base64 of `iv || ciphertext`.
 */
class KeystoreTokenCipher @Inject constructor() : TokenCipher {

    override fun encrypt(plaintext: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val combined = cipher.iv + cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    override fun decrypt(ciphertext: String): String {
        val combined = try {
            Base64.decode(ciphertext, Base64.NO_WRAP)
        } catch (malformed: IllegalArgumentException) {
            throw UnreadableTokenException(malformed)
        }
        if (combined.size <= IV_LENGTH) throw UnreadableTokenException()
        val key = existingKey() ?: throw UnreadableTokenException()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_LENGTH_BITS, combined.copyOfRange(0, IV_LENGTH)))
        return try {
            String(cipher.doFinal(combined, IV_LENGTH, combined.size - IV_LENGTH), Charsets.UTF_8)
        } catch (wrongKey: AEADBadTagException) {
            throw UnreadableTokenException(wrongKey) // encrypted with a key this device no longer has
        }
    }

    private fun keyStore(): KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    /** Never creates a key: a missing key means the stored token can no longer be read. */
    private fun existingKey(): SecretKey? =
        (keyStore().getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey

    private fun getOrCreateKey(): SecretKey {
        existingKey()?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE_BITS)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "qhathia_discovery_session_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_LENGTH = 12
        const val TAG_LENGTH_BITS = 128
        const val KEY_SIZE_BITS = 256
    }
}
