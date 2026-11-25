package com.qhathia.mobile.discovery.core.data.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class DeviceTokenEncryptionTest {

    @TempDir
    lateinit var tempDir: File

    private val cipher = FakeTokenCipher()
    private val file get() = File(tempDir, "store_session.preferences_pb")

    /** Opens the store as the app does, with the migration. */
    private fun CoroutineScope.openStore(): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        migrations = listOf(DeviceTokenMigration(cipher)),
        scope = this,
    ) { file }

    private fun rawFileText() = file.readBytes().toString(Charsets.ISO_8859_1)

    @Test
    fun saved_token_is_read_back_and_not_stored_in_plain_text() = runTest {
        val source = StoreSessionLocalDataSource(backgroundScope.openStore(), cipher)

        source.saveDeviceSession("jwt-secret-123", deviceName = "Tablet 1", pairingCode = null)

        assertEquals("jwt-secret-123", source.deviceAccessToken())
        assertEquals("jwt-secret-123", source.observe().first().deviceAccessToken)
        assertFalse(rawFileText().contains("jwt-secret-123"))
    }

    @Test
    fun plain_token_from_an_older_version_is_kept_and_encrypted() = runTest {
        val migration = DeviceTokenMigration(cipher)
        val before = PreferenceDataStoreFactory.create(scope = backgroundScope) { file }
        before.edit {
            it[stringPreferencesKey("device_access_token")] = "jwt-legacy-456"
            it[stringPreferencesKey("store_name")] = "El Martillo"
        }
        val current = before.data.first()

        assertTrue(migration.shouldMigrate(current))
        val migrated = migration.migrate(current)

        val stored = migrated[DeviceTokenStorage.KEY_DEVICE_TOKEN].orEmpty()
        assertTrue(DeviceTokenStorage.isEncrypted(stored))
        assertFalse(stored.contains("jwt-legacy-456"))
        assertEquals("jwt-legacy-456", DeviceTokenStorage.decodeOrNull(cipher, stored))
        assertEquals("El Martillo", migrated[stringPreferencesKey("store_name")])
        assertFalse(migration.shouldMigrate(migrated))
    }

    @Test
    fun plain_token_is_migrated_when_the_store_opens() = runTest {
        val dir = File(tempDir, "legacy").apply { mkdirs() }
        val legacyFile = File(dir, "store_session.preferences_pb")
        // The previous version's store, closed before the new one opens the same file.
        val legacyScope = CoroutineScope(Job())
        PreferenceDataStoreFactory.create(scope = legacyScope) { legacyFile }
            .edit { it[stringPreferencesKey("device_access_token")] = "jwt-legacy-456" }
        legacyScope.cancel()

        val store = PreferenceDataStoreFactory.create(
            migrations = listOf(DeviceTokenMigration(cipher)),
            scope = backgroundScope,
        ) { legacyFile }
        val source = StoreSessionLocalDataSource(store, cipher)

        assertEquals("jwt-legacy-456", source.deviceAccessToken())
        assertFalse(legacyFile.readBytes().toString(Charsets.ISO_8859_1).contains("jwt-legacy-456"))
    }

    @Test
    fun unreadable_token_is_removed_and_store_name_kept() = runTest {
        val migration = DeviceTokenMigration(cipher)
        val store = PreferenceDataStoreFactory.create(scope = backgroundScope) { file }
        store.edit {
            it[DeviceTokenStorage.KEY_DEVICE_TOKEN] = "v1:garbage-from-a-lost-key"
            it[stringPreferencesKey("device_name")] = "Tablet 1"
            it[stringPreferencesKey("store_name")] = "El Martillo"
        }
        val current = store.data.first()

        assertTrue(migration.shouldMigrate(current))
        val migrated = migration.migrate(current)

        assertNull(migrated[DeviceTokenStorage.KEY_DEVICE_TOKEN])
        assertNull(migrated[stringPreferencesKey("device_name")])
        assertEquals("El Martillo", migrated[stringPreferencesKey("store_name")])
    }

    @Test
    fun temporary_keystore_failure_keeps_the_token() = runTest {
        val migration = DeviceTokenMigration(cipher)
        val stored = DeviceTokenStorage.encode(cipher, "jwt-secret-123")
        val store = PreferenceDataStoreFactory.create(scope = backgroundScope) { file }
        store.edit { it[DeviceTokenStorage.KEY_DEVICE_TOKEN] = stored }
        cipher.failure = IllegalStateException("keystore busy")

        assertFalse(migration.shouldMigrate(store.data.first()))
        assertNull(StoreSessionLocalDataSource(store, cipher).deviceAccessToken())

        cipher.failure = null
        assertEquals("jwt-secret-123", StoreSessionLocalDataSource(store, cipher).deviceAccessToken())
    }

    @Test
    fun no_token_needs_no_migration() = runTest {
        val store = PreferenceDataStoreFactory.create(scope = backgroundScope) { file }
        store.edit { it[stringPreferencesKey("store_name")] = "El Martillo" }

        assertFalse(DeviceTokenMigration(cipher).shouldMigrate(store.data.first()))
    }
}
