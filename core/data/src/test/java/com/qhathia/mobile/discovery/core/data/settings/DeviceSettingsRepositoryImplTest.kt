package com.qhathia.mobile.discovery.core.data.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.qhathia.mobile.discovery.core.domain.model.BrightnessMode
import com.qhathia.mobile.discovery.core.domain.model.DeviceSettings
import com.qhathia.mobile.discovery.core.domain.model.StoreSession
import com.qhathia.mobile.discovery.core.network.NetworkErrorKind
import com.qhathia.mobile.discovery.core.network.NetworkResult
import com.qhathia.mobile.discovery.core.network.dto.DeviceSettingsDto
import com.qhathia.mobile.discovery.core.network.service.DeviceSettingsApiService
import com.qhathia.mobile.discovery.core.testing.FakeStoreSessionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.IOException

class DeviceSettingsRepositoryImplTest {

    @TempDir
    lateinit var tempDir: File

    private val api = mockk<DeviceSettingsApiService>()
    private val paired = FakeStoreSessionRepository(StoreSession(deviceAccessToken = "device-jwt"))

    private fun CoroutineScope.local() = DeviceSettingsLocalDataSource(
        PreferenceDataStoreFactory.create(scope = this) { File(tempDir, "store_session.preferences_pb") },
    )

    @Test
    fun before_any_sync_observes_server_defaults() = runTest {
        val repository = DeviceSettingsRepositoryImpl(api, backgroundScope.local(), paired)

        assertEquals(DeviceSettings.DEFAULT, repository.observe().first())
        assertNull(repository.storedVersion())
    }

    @Test
    fun refresh_stores_settings_version_and_hash() = runTest {
        val local = backgroundScope.local()
        coEvery { api.getMySettings("device-jwt") } returns NetworkResult.Success(
            DeviceSettingsDto(voiceAssistantEnabled = false, brightnessMode = "HIGH", exitPinHash = "\$2a\$10\$x", settingsVersion = 5),
        )
        val repository = DeviceSettingsRepositoryImpl(api, local, paired)

        repository.refresh()

        val settings = repository.observe().first()
        assertFalse(settings.voiceAssistantEnabled)
        assertEquals(BrightnessMode.HIGH, settings.brightnessMode)
        assertTrue(settings.hasExitPin)
        assertEquals(5L, repository.storedVersion())
        assertEquals("\$2a\$10\$x", local.exitPinHash())
    }

    @Test
    fun refresh_without_pin_clears_a_previous_hash() = runTest {
        val local = backgroundScope.local()
        local.save(DeviceSettingsLocalDataSource.Stored(DeviceSettings.DEFAULT.copy(version = 1), "\$2a\$10\$old"))
        coEvery { api.getMySettings(any()) } returns NetworkResult.Success(DeviceSettingsDto(settingsVersion = 2))

        DeviceSettingsRepositoryImpl(api, local, paired).refresh()

        assertNull(local.exitPinHash())
    }

    @Test
    fun unknown_brightness_maps_to_auto() {
        assertEquals(BrightnessMode.AUTO, brightnessModeOf("ULTRA"))
        assertEquals(BrightnessMode.AUTO, brightnessModeOf(null))
        assertEquals(BrightnessMode.LOW, brightnessModeOf("low"))
    }

    @Test
    fun not_paired_fails_without_request() = runTest {
        val repository = DeviceSettingsRepositoryImpl(api, backgroundScope.local(), FakeStoreSessionRepository())

        assertTrue(repository.refresh().isFailure)
        coVerify(exactly = 0) { api.getMySettings(any()) }
    }

    @Test
    fun network_failure_keeps_stored_settings() = runTest {
        val local = backgroundScope.local()
        local.save(DeviceSettingsLocalDataSource.Stored(DeviceSettings.DEFAULT.copy(version = 3), null))
        coEvery { api.getMySettings(any()) } returns NetworkResult.NetworkError(IOException("offline"), NetworkErrorKind.Connectivity)
        val repository = DeviceSettingsRepositoryImpl(api, local, paired)

        assertTrue(repository.refresh().isFailure)
        assertEquals(3L, repository.storedVersion())
    }
}
