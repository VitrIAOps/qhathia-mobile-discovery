package com.qhathia.mobile.discovery.core.data.pairing

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * The session of the pairing code on screen, kept across restarts so an abandoned code can be
 * cancelled or replaced (pairing-abandoned-code D1).
 */
interface PendingPairingSessionStore {
    suspend fun get(): String?

    suspend fun set(sessionId: String)

    /** Forgets the session only if it is still [sessionId], so a late call cannot drop a newer code. */
    suspend fun clearIf(sessionId: String)
}

/** [PendingPairingSessionStore] in the app's `store_session` DataStore. */
class DataStorePendingPairingSessionStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : PendingPairingSessionStore {

    override suspend fun get(): String? = dataStore.data.first()[KEY_SESSION_ID]

    override suspend fun set(sessionId: String) {
        dataStore.edit { it[KEY_SESSION_ID] = sessionId }
    }

    override suspend fun clearIf(sessionId: String) {
        dataStore.edit { preferences ->
            if (preferences[KEY_SESSION_ID] == sessionId) preferences.remove(KEY_SESSION_ID)
        }
    }

    private companion object {
        val KEY_SESSION_ID = stringPreferencesKey("pending_pairing_session_id")
    }
}
