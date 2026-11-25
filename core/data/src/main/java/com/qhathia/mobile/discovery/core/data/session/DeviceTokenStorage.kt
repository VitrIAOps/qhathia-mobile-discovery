package com.qhathia.mobile.discovery.core.data.session

import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CancellationException

/** How the device access token is written to the `store_session` DataStore. */
internal object DeviceTokenStorage {

    val KEY_DEVICE_TOKEN = stringPreferencesKey("device_access_token")

    /** Keys that only make sense with a usable token; removed with an unreadable one. */
    val KEYS_BOUND_TO_TOKEN = listOf(
        KEY_DEVICE_TOKEN,
        stringPreferencesKey("device_name"),
        stringPreferencesKey("pairing_code"),
        stringPreferencesKey("store_category"),
    )

    /** Marks an encrypted value; a value without it was written in plain text by an older version. */
    private const val PREFIX = "v1:"

    fun isEncrypted(stored: String): Boolean = stored.startsWith(PREFIX)

    fun encode(cipher: TokenCipher, token: String): String = PREFIX + cipher.encrypt(token)

    /** What reading a stored value gives. */
    sealed interface Decoded {
        data class Token(val value: String) : Decoded
        /** Can never be read on this device: the value must be removed. */
        data object Unreadable : Decoded
        /** Could not be read now (temporary failure, or not yet migrated); kept for a later try. */
        data object Unavailable : Decoded
    }

    fun decode(cipher: TokenCipher, stored: String): Decoded {
        if (!isEncrypted(stored)) return Decoded.Unavailable
        return try {
            Decoded.Token(cipher.decrypt(stored.removePrefix(PREFIX)))
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: UnreadableTokenException) {
            Decoded.Unreadable
        } catch (_: Exception) {
            Decoded.Unavailable
        }
    }

    /** The token, or `null` when it cannot be read right now. */
    fun decodeOrNull(cipher: TokenCipher, stored: String): String? =
        (decode(cipher, stored) as? Decoded.Token)?.value
}

/**
 * Runs each time the DataStore opens: encrypts a token stored in plain text by an older version,
 * so the tablet stays paired, and removes a token that can never be decrypted (for example after
 * its key was lost), so the app treats the tablet as unpaired instead of failing. A temporary
 * Keystore failure removes nothing.
 */
internal class DeviceTokenMigration(private val cipher: TokenCipher) : DataMigration<Preferences> {

    override suspend fun shouldMigrate(currentData: Preferences): Boolean {
        val stored = currentData[DeviceTokenStorage.KEY_DEVICE_TOKEN] ?: return false
        return !DeviceTokenStorage.isEncrypted(stored) ||
            DeviceTokenStorage.decode(cipher, stored) == DeviceTokenStorage.Decoded.Unreadable
    }

    override suspend fun migrate(currentData: Preferences): Preferences {
        val stored = currentData[DeviceTokenStorage.KEY_DEVICE_TOKEN] ?: return currentData
        val updated = currentData.toMutablePreferences()
        if (!DeviceTokenStorage.isEncrypted(stored)) {
            updated[DeviceTokenStorage.KEY_DEVICE_TOKEN] = DeviceTokenStorage.encode(cipher, stored)
        } else {
            DeviceTokenStorage.KEYS_BOUND_TO_TOKEN.forEach { updated.remove(it) }
        }
        return updated.toPreferences()
    }

    override suspend fun cleanUp() = Unit
}
