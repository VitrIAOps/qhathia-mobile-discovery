package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.model.DeviceSettings
import com.qhathia.mobile.discovery.core.testing.DeviceHeartbeatFixtures
import com.qhathia.mobile.discovery.core.testing.FakeDeviceHeartbeatRepository
import com.qhathia.mobile.discovery.core.testing.FakeDeviceSettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SyncDeviceSettingsUseCaseTest {

    private val heartbeat = FakeDeviceHeartbeatRepository()
    private val settings = FakeDeviceSettingsRepository()

    private fun TestScope.start() {
        val useCase = SyncDeviceSettingsUseCase(ObserveHeartbeatUseCase(heartbeat), settings)
        backgroundScope.launch { useCase() }
        runCurrent()
    }

    private suspend fun TestScope.beat(settingsVersion: Long?) {
        heartbeat.nextResult = Result.success(DeviceHeartbeatFixtures.result(settingsVersion = settingsVersion))
        heartbeat.sendHeartbeat(DeviceHeartbeatFixtures.telemetry())
        runCurrent()
    }

    @Test
    fun first_version_after_install_triggers_one_read() = runTest {
        settings.remote = DeviceSettings.DEFAULT.copy(version = 4)
        start()

        beat(4)
        beat(4)

        assertEquals(1, settings.refreshCount)
        assertEquals(4L, settings.storedVersion())
    }

    @Test
    fun equal_version_does_not_read() = runTest {
        settings.store(DeviceSettings.DEFAULT.copy(version = 4))
        start()

        beat(4)

        assertEquals(0, settings.refreshCount)
    }

    @Test
    fun new_version_triggers_read() = runTest {
        settings.store(DeviceSettings.DEFAULT.copy(version = 4))
        settings.remote = DeviceSettings.DEFAULT.copy(version = 5, voiceAssistantEnabled = false)
        start()

        beat(5)

        assertEquals(1, settings.refreshCount)
        assertEquals(5L, settings.storedVersion())
    }

    @Test
    fun failed_read_is_retried_on_next_heartbeat() = runTest {
        settings.refreshFailure = IllegalStateException("offline")
        settings.remote = DeviceSettings.DEFAULT.copy(version = 4)
        start()

        beat(4)
        settings.refreshFailure = null
        beat(4)

        assertEquals(2, settings.refreshCount)
        assertEquals(4L, settings.storedVersion())
    }

    @Test
    fun missing_version_is_ignored() = runTest {
        start()

        beat(null)

        assertEquals(0, settings.refreshCount)
    }
}
