package com.qhathia.mobile.discovery.core.testing

import com.qhathia.mobile.discovery.core.domain.model.DeviceSettings
import com.qhathia.mobile.discovery.core.domain.repository.DeviceSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory [DeviceSettingsRepository]. [refresh] stores [remote] (or fails with [refreshFailure]) and
 * counts calls in [refreshCount].
 */
class FakeDeviceSettingsRepository(
    initial: DeviceSettings = DeviceSettings.DEFAULT,
    var remote: DeviceSettings = DeviceSettings.DEFAULT.copy(version = 1),
    var refreshFailure: Throwable? = null,
) : DeviceSettingsRepository {

    private val stored = MutableStateFlow(initial)

    var refreshCount: Int = 0
        private set

    override fun observe(): Flow<DeviceSettings> = stored.asStateFlow()

    override suspend fun storedVersion(): Long? = stored.value.version

    override suspend fun refresh(): Result<DeviceSettings> {
        refreshCount++
        refreshFailure?.let { return Result.failure(it) }
        stored.value = remote
        return Result.success(remote)
    }

    /** Test helper: replaces the stored settings. */
    fun store(settings: DeviceSettings) {
        stored.value = settings
    }
}
