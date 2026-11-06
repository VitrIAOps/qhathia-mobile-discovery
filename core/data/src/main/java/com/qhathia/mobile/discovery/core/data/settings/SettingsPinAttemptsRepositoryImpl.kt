package com.qhathia.mobile.discovery.core.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.qhathia.mobile.discovery.core.domain.model.PinAttempts
import com.qhathia.mobile.discovery.core.domain.repository.SettingsPinAttemptsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import java.security.MessageDigest
import java.time.Instant
import java.util.Locale
import javax.inject.Inject

/**
 * [SettingsPinAttemptsRepository] in the app's `store_session` DataStore (design D9). Each write
 * stores a fingerprint of the exit PIN it applies to: a SHA-256 of the stored PIN hash, never the
 * PIN. When the staff app sets a new PIN or removes it, the fingerprint no longer matches and the
 * attempts read as [PinAttempts.NONE], so the lock is lifted. This is the merchant's way back in.
 */
class SettingsPinAttemptsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val settingsLocalDataSource: DeviceSettingsLocalDataSource,
) : SettingsPinAttemptsRepository {

    override fun observe(): Flow<PinAttempts> =
        combine(dataStore.data, settingsLocalDataSource.observeExitPinHash()) { preferences, pinHash ->
            val storedFingerprint = preferences[KEY_PIN_FINGERPRINT]
            if (storedFingerprint != null && storedFingerprint != fingerprintOf(pinHash)) {
                PinAttempts.NONE
            } else {
                PinAttempts(
                    failedCount = preferences[KEY_FAILED_COUNT] ?: 0,
                    lockedAt = preferences[KEY_LOCKED_AT]?.let(Instant::ofEpochMilli),
                    lockCount = preferences[KEY_LOCK_COUNT] ?: 0,
                )
            }
        }.distinctUntilChanged()

    override suspend fun get(): PinAttempts = observe().first()

    override suspend fun set(attempts: PinAttempts) {
        val fingerprint = fingerprintOf(settingsLocalDataSource.exitPinHash())
        dataStore.edit { preferences ->
            if (attempts == PinAttempts.NONE) {
                preferences.remove(KEY_FAILED_COUNT)
                preferences.remove(KEY_LOCKED_AT)
                preferences.remove(KEY_LOCK_COUNT)
                preferences.remove(KEY_PIN_FINGERPRINT)
                return@edit
            }
            preferences[KEY_FAILED_COUNT] = attempts.failedCount
            attempts.lockedAt?.let { preferences[KEY_LOCKED_AT] = it.toEpochMilli() }
                ?: preferences.remove(KEY_LOCKED_AT)
            preferences[KEY_LOCK_COUNT] = attempts.lockCount
            preferences[KEY_PIN_FINGERPRINT] = fingerprint
        }
    }

    private fun fingerprintOf(pinHash: String?): String {
        if (pinHash.isNullOrBlank()) return NO_PIN
        val digest = MessageDigest.getInstance("SHA-256").digest(pinHash.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { String.format(Locale.ROOT, "%02x", it) }
    }

    private companion object {
        const val NO_PIN = "none"
        val KEY_FAILED_COUNT = intPreferencesKey("settings_pin_failed_count")
        val KEY_LOCKED_AT = longPreferencesKey("settings_pin_locked_at")
        val KEY_LOCK_COUNT = intPreferencesKey("settings_pin_lock_count")
        val KEY_PIN_FINGERPRINT = stringPreferencesKey("settings_pin_fingerprint")
    }
}
