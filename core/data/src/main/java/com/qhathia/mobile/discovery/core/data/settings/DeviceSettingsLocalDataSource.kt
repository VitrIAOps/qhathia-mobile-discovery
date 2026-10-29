package com.qhathia.mobile.discovery.core.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.qhathia.mobile.discovery.core.domain.model.DeviceSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Persists the remote settings and the exit PIN hash in the app's `store_session` DataStore (next to
 * the DEVICE token, which is equally app-private). Absent keys read as the server defaults.
 */
class DeviceSettingsLocalDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    /** Stored settings plus their exit PIN hash, as one consistent snapshot. */
    data class Stored(val settings: DeviceSettings, val exitPinHash: String?)

    fun observe(): Flow<DeviceSettings> = dataStore.data.map { it.toStored().settings }

    suspend fun storedVersion(): Long? = dataStore.data.first()[KEY_VERSION]

    suspend fun exitPinHash(): String? = dataStore.data.first()[KEY_EXIT_PIN_HASH]

    /** The stored exit PIN hash, again whenever the remote settings change it. */
    fun observeExitPinHash(): Flow<String?> = dataStore.data.map { it[KEY_EXIT_PIN_HASH] }

    suspend fun save(stored: Stored) {
        val settings = stored.settings
        dataStore.edit { preferences ->
            preferences[KEY_IDLE_SHOWCASE] = settings.idleShowcaseEnabled
            preferences[KEY_VOICE_ASSISTANT] = settings.voiceAssistantEnabled
            preferences[KEY_BRIGHTNESS] = settings.brightnessMode.name
            preferences[KEY_KIOSK_MODE] = settings.kioskMode
            preferences[KEY_AUTO_SYNC] = settings.autoSyncEnabled
            settings.version?.let { preferences[KEY_VERSION] = it } ?: preferences.remove(KEY_VERSION)
            if (stored.exitPinHash.isNullOrBlank()) {
                preferences.remove(KEY_EXIT_PIN_HASH)
            } else {
                preferences[KEY_EXIT_PIN_HASH] = stored.exitPinHash
            }
        }
    }

    private fun Preferences.toStored(): Stored {
        val hash = this[KEY_EXIT_PIN_HASH]
        val defaults = DeviceSettings.DEFAULT
        return Stored(
            settings = DeviceSettings(
                idleShowcaseEnabled = this[KEY_IDLE_SHOWCASE] ?: defaults.idleShowcaseEnabled,
                voiceAssistantEnabled = this[KEY_VOICE_ASSISTANT] ?: defaults.voiceAssistantEnabled,
                brightnessMode = brightnessModeOf(this[KEY_BRIGHTNESS]),
                kioskMode = this[KEY_KIOSK_MODE] ?: defaults.kioskMode,
                hasExitPin = !hash.isNullOrBlank(),
                autoSyncEnabled = this[KEY_AUTO_SYNC] ?: defaults.autoSyncEnabled,
                version = this[KEY_VERSION],
            ),
            exitPinHash = hash,
        )
    }

    private companion object {
        val KEY_IDLE_SHOWCASE = booleanPreferencesKey("settings_idle_showcase_enabled")
        val KEY_VOICE_ASSISTANT = booleanPreferencesKey("settings_voice_assistant_enabled")
        val KEY_BRIGHTNESS = stringPreferencesKey("settings_brightness_mode")
        val KEY_KIOSK_MODE = booleanPreferencesKey("settings_kiosk_mode")
        val KEY_AUTO_SYNC = booleanPreferencesKey("settings_auto_sync_enabled")
        val KEY_VERSION = longPreferencesKey("settings_version")
        val KEY_EXIT_PIN_HASH = stringPreferencesKey("settings_exit_pin_hash")
    }
}
