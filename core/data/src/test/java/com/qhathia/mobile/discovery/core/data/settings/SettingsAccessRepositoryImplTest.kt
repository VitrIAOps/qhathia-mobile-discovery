package com.qhathia.mobile.discovery.core.data.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import at.favre.lib.crypto.bcrypt.BCrypt
import com.qhathia.mobile.discovery.core.domain.model.DeviceSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class SettingsAccessRepositoryImplTest {

    @TempDir
    lateinit var tempDir: File

    /** Spring Security's format (`$2a$`), low cost to keep the test fast. */
    private val hashOf246810 = BCrypt.with(BCrypt.Version.VERSION_2A).hashToString(4, "246810".toCharArray())

    private fun CoroutineScope.local(): DeviceSettingsLocalDataSource =
        DeviceSettingsLocalDataSource(
            PreferenceDataStoreFactory.create(scope = this) { File(tempDir, "store_session.preferences_pb") },
        )

    private suspend fun DeviceSettingsLocalDataSource.storeHash(hash: String?) =
        save(DeviceSettingsLocalDataSource.Stored(DeviceSettings.DEFAULT.copy(version = 1), hash))

    @Test
    fun given_no_hash_then_pin_not_required_and_nothing_validates() = runTest {
        val repository = SettingsAccessRepositoryImpl(backgroundScope.local(), ExitPinVerifier())

        assertFalse(repository.isPinRequired())
        assertFalse(repository.validatePassword("1234").getOrThrow())
    }

    @Test
    fun given_hash_then_pin_required_and_correct_pin_accepted() = runTest {
        val local = backgroundScope.local()
        local.storeHash(hashOf246810)
        val repository = SettingsAccessRepositoryImpl(local, ExitPinVerifier())

        assertTrue(repository.isPinRequired())
        assertTrue(repository.validatePassword("246810").getOrThrow())
    }

    @Test
    fun given_hash_then_wrong_pin_and_old_mock_rejected() = runTest {
        val local = backgroundScope.local()
        local.storeHash(hashOf246810)
        val repository = SettingsAccessRepositoryImpl(local, ExitPinVerifier())

        assertFalse(repository.validatePassword("1234").getOrThrow())
        assertFalse(repository.validatePassword("246811").getOrThrow())
    }

    @Test
    fun verifier_accepts_2b_and_rejects_malformed_hash() = runTest {
        val verifier = ExitPinVerifier()
        val hash2b = BCrypt.with(BCrypt.Version.VERSION_2B).hashToString(4, "4321".toCharArray())

        assertTrue(verifier.matches("4321", hash2b))
        assertFalse(verifier.matches("4321", "not-a-bcrypt-hash"))
    }
}
