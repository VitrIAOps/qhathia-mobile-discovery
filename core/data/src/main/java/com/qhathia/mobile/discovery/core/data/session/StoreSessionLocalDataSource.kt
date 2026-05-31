package com.qhathia.mobile.discovery.core.data.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.qhathia.mobile.discovery.core.domain.model.StoreSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Reads and writes the [StoreSession] to a Preferences DataStore. An absent key maps to
 * [StoreSession.EMPTY], so callers always get a well-defined value.
 */
class StoreSessionLocalDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    fun observe(): Flow<StoreSession> = dataStore.data.map { preferences ->
        StoreSession(storeName = preferences[KEY_STORE_NAME])
    }

    suspend fun updateStoreName(storeName: String?) {
        dataStore.edit { preferences ->
            if (storeName.isNullOrBlank()) {
                preferences.remove(KEY_STORE_NAME)
            } else {
                preferences[KEY_STORE_NAME] = storeName
            }
        }
    }

    private companion object {
        val KEY_STORE_NAME = stringPreferencesKey("store_name")
    }
}
