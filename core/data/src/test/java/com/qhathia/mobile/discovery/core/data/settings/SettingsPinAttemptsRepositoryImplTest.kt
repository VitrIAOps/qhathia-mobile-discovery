package com.qhathia.mobile.discovery.core.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.qhathia.mobile.discovery.core.domain.model.DeviceSettings
import com.qhathia.mobile.discovery.core.domain.model.PinAttempts
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.time.Instant

class SettingsPinAttemptsRepositoryImplTest {

    @TempDir
    lateinit var tempDir: File

    private val locked = PinAttempts(failedCount = 5, lockedAt = Instant.parse("2026-10-29T12:00:00Z"))

    private fun file() = File(tempDir, "store_session.preferences_pb")

    private fun CoroutineScope.dataStore(): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(scope = this) { file() }

    private fun repositoryOver(dataStore: DataStore<Preferences>) =
        SettingsPinAttemptsRepositoryImpl(dataStore, DeviceSettingsLocalDataSource(dataStore))

    private suspend fun DataStore<Preferences>.storePinHash(hash: String?) =
        DeviceSettingsLocalDataSource(this)
            .save(DeviceSettingsLocalDataSource.Stored(DeviceSettings.DEFAULT.copy(version = 1), hash))

    @Test
    fun lock_is_read_back_by_a_new_instance() = runTest {
        // First "process": lock the gate.
        val first = backgroundScope.dataStore()
        first.storePinHash("hash-A")
        repositoryOver(first).set(locked)

        // Second "process": a new DataStore over what is on disk. DataStore forbids two live stores on
        // one file, so it reads a copy of it.
        val copy = file().copyTo(File(tempDir, "restarted.preferences_pb"))
        val second = PreferenceDataStoreFactory.create(scope = backgroundScope) { copy }

        assertEquals(locked, repositoryOver(second).get())
    }

    @Test
    fun new_pin_from_the_staff_app_clears_the_lock() = runTest {
        val store = backgroundScope.dataStore()
        store.storePinHash("hash-A")
        val repository = repositoryOver(store)
        repository.set(locked)

        store.storePinHash("hash-B")

        assertEquals(PinAttempts.NONE, repository.get())
    }

    @Test
    fun pin_removed_from_the_staff_app_clears_the_lock() = runTest {
        val store = backgroundScope.dataStore()
        store.storePinHash("hash-A")
        val repository = repositoryOver(store)
        repository.set(locked)

        store.storePinHash(null)

        assertEquals(PinAttempts.NONE, repository.get())
    }

    @Test
    fun same_pin_keeps_the_lock_and_reset_clears_it() = runTest {
        val store = backgroundScope.dataStore()
        store.storePinHash("hash-A")
        val repository = repositoryOver(store)
        repository.set(locked)

        store.storePinHash("hash-A") // a settings sync that does not change the PIN
        assertEquals(locked, repository.get())

        repository.set(PinAttempts.NONE)
        assertEquals(PinAttempts.NONE, repository.get())
    }
}
