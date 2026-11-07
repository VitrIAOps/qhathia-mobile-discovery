package com.qhathia.mobile.discovery.core.data.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.qhathia.mobile.discovery.core.domain.model.StoreSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Reads and writes the [StoreSession] to a Preferences DataStore. An absent key maps to
 * [StoreSession.EMPTY], so callers always get a well-defined value.
 */
class StoreSessionLocalDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    fun observe(): Flow<StoreSession> = dataStore.data.map { it.toStoreSession() }

    suspend fun updateStoreName(storeName: String?) {
        dataStore.edit { preferences ->
            if (storeName.isNullOrBlank()) {
                preferences.remove(KEY_STORE_NAME)
            } else {
                preferences[KEY_STORE_NAME] = storeName
            }
        }
    }

    /** Reads the persisted DEVICE access token, or `null` when not paired. */
    suspend fun deviceAccessToken(): String? = dataStore.data.first()[KEY_DEVICE_TOKEN]

    /** Persists the DEVICE access token, device name and pairing code produced by pairing. */
    suspend fun saveDeviceSession(deviceAccessToken: String, deviceName: String?, pairingCode: String?) {
        dataStore.edit { preferences ->
            preferences[KEY_DEVICE_TOKEN] = deviceAccessToken
            preferences.putOrRemove(KEY_DEVICE_NAME, deviceName)
            preferences.putOrRemove(KEY_PAIRING_CODE, pairingCode)
        }
    }

    /** Clears the persisted device session (used by unpair). */
    suspend fun clearDeviceSession() {
        dataStore.edit { preferences ->
            preferences.remove(KEY_DEVICE_TOKEN)
            preferences.remove(KEY_DEVICE_NAME)
            preferences.remove(KEY_PAIRING_CODE)
        }
    }

    /**
     * Revokes the device session after an unlink: one write that removes the token, device name,
     * pairing code and store name, and marks the unlink notice as pending.
     */
    suspend fun revokeDeviceSession() {
        dataStore.edit { preferences ->
            preferences.remove(KEY_DEVICE_TOKEN)
            preferences.remove(KEY_DEVICE_NAME)
            preferences.remove(KEY_PAIRING_CODE)
            preferences.remove(KEY_STORE_NAME)
            preferences[KEY_UNLINKED_NOTICE] = true
        }
    }

    /** Clears the pending unlink notice and nothing else. */
    suspend fun dismissUnlinkedNotice() {
        dataStore.edit { preferences -> preferences.remove(KEY_UNLINKED_NOTICE) }
    }

    private fun MutablePreferences.putOrRemove(key: Preferences.Key<String>, value: String?) {
        if (value.isNullOrBlank()) remove(key) else this[key] = value
    }

    private fun Preferences.toStoreSession(): StoreSession = StoreSession(
        storeName = this[KEY_STORE_NAME],
        deviceAccessToken = this[KEY_DEVICE_TOKEN],
        deviceName = this[KEY_DEVICE_NAME],
        pairingCode = this[KEY_PAIRING_CODE],
        unlinkedNotice = this[KEY_UNLINKED_NOTICE] == true,
    )

    private companion object {
        val KEY_STORE_NAME = stringPreferencesKey("store_name")
        val KEY_DEVICE_TOKEN = stringPreferencesKey("device_access_token")
        val KEY_DEVICE_NAME = stringPreferencesKey("device_name")
        val KEY_PAIRING_CODE = stringPreferencesKey("pairing_code")
        val KEY_UNLINKED_NOTICE = booleanPreferencesKey("unlinked_notice")
    }
}
