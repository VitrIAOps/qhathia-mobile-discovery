package com.qhathia.mobile.discovery.core.data.command

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.qhathia.mobile.discovery.core.domain.model.PendingRestartAck
import com.qhathia.mobile.discovery.core.domain.repository.PendingRestartAckRepository
import kotlinx.coroutines.flow.first
import java.time.Instant
import javax.inject.Inject

/** [PendingRestartAckRepository] in the app's `store_session` DataStore, so it survives the restart. */
class PendingRestartAckRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : PendingRestartAckRepository {

    override suspend fun save(ack: PendingRestartAck) {
        dataStore.edit { preferences ->
            preferences[KEY_ID] = ack.commandId
            preferences[KEY_SAVED_AT] = ack.savedAt.toEpochMilli()
        }
    }

    override suspend fun get(): PendingRestartAck? {
        val preferences = dataStore.data.first()
        val id = preferences[KEY_ID] ?: return null
        val savedAt = preferences[KEY_SAVED_AT] ?: return null
        return PendingRestartAck(id, Instant.ofEpochMilli(savedAt))
    }

    override suspend fun clear() {
        dataStore.edit { preferences ->
            preferences.remove(KEY_ID)
            preferences.remove(KEY_SAVED_AT)
        }
    }

    private companion object {
        val KEY_ID = stringPreferencesKey("pending_restart_ack_id")
        val KEY_SAVED_AT = longPreferencesKey("pending_restart_ack_saved_at")
    }
}
