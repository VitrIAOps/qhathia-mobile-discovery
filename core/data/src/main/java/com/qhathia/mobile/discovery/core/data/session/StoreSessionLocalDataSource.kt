package com.qhathia.mobile.discovery.core.data.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.qhathia.mobile.discovery.core.domain.model.StoreCategory
import com.qhathia.mobile.discovery.core.domain.model.StoreSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Reads and writes the [StoreSession] to a Preferences DataStore. An absent key maps to
 * [StoreSession.EMPTY], so callers always get a well-defined value. The device access token is
 * stored encrypted by [cipher] and never in plain text; [DeviceTokenMigration] upgrades older values.
 */
class StoreSessionLocalDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val cipher: TokenCipher,
) {
    fun observe(): Flow<StoreSession> = dataStore.data.map { it.toStoreSession() }.flowOn(Dispatchers.IO)

    suspend fun updateStoreName(storeName: String?) {
        dataStore.edit { preferences ->
            if (storeName.isNullOrBlank()) {
                preferences.remove(KEY_STORE_NAME)
            } else {
                preferences[KEY_STORE_NAME] = storeName
            }
        }
    }

    suspend fun updateStoreCategory(category: StoreCategory?) {
        dataStore.edit { preferences ->
            if (category == null) preferences.remove(KEY_STORE_CATEGORY) else preferences[KEY_STORE_CATEGORY] = category.name
        }
    }

    /** Reads the persisted DEVICE access token, or `null` when not paired. */
    suspend fun deviceAccessToken(): String? = withContext(Dispatchers.IO) { dataStore.data.first().deviceToken() }

    /** Persists the DEVICE access token, device name and pairing code produced by pairing. */
    suspend fun saveDeviceSession(deviceAccessToken: String, deviceName: String?, pairingCode: String?) {
        val encrypted = withContext(Dispatchers.IO) { DeviceTokenStorage.encode(cipher, deviceAccessToken) }
        dataStore.edit { preferences ->
            preferences[KEY_DEVICE_TOKEN] = encrypted
            preferences.putOrRemove(KEY_DEVICE_NAME, deviceName)
            preferences.putOrRemove(KEY_PAIRING_CODE, pairingCode)
        }
    }

    /** Clears the persisted device session (local clean-up). */
    suspend fun clearDeviceSession() {
        dataStore.edit { preferences ->
            preferences.remove(KEY_DEVICE_TOKEN)
            preferences.remove(KEY_DEVICE_NAME)
            preferences.remove(KEY_PAIRING_CODE)
            preferences.remove(KEY_STORE_CATEGORY)
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
            preferences.remove(KEY_STORE_CATEGORY)
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

    private fun Preferences.deviceToken(): String? =
        this[KEY_DEVICE_TOKEN]?.let { DeviceTokenStorage.decodeOrNull(cipher, it) }

    private fun Preferences.toStoreSession(): StoreSession = StoreSession(
        storeName = this[KEY_STORE_NAME],
        deviceAccessToken = deviceToken(),
        deviceName = this[KEY_DEVICE_NAME],
        pairingCode = this[KEY_PAIRING_CODE],
        unlinkedNotice = this[KEY_UNLINKED_NOTICE] == true,
        storeCategory = StoreCategory.fromApi(this[KEY_STORE_CATEGORY]),
    )

    private companion object {
        val KEY_STORE_NAME = stringPreferencesKey("store_name")
        val KEY_DEVICE_TOKEN = DeviceTokenStorage.KEY_DEVICE_TOKEN
        val KEY_DEVICE_NAME = stringPreferencesKey("device_name")
        val KEY_PAIRING_CODE = stringPreferencesKey("pairing_code")
        val KEY_UNLINKED_NOTICE = booleanPreferencesKey("unlinked_notice")
        val KEY_STORE_CATEGORY = stringPreferencesKey("store_category")
    }
}
