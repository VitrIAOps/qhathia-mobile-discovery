package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.model.DeviceSettings
import com.qhathia.mobile.discovery.core.domain.repository.DeviceSettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Streams the stored remote settings ([DeviceSettings.DEFAULT] until the first sync). */
class ObserveDeviceSettingsUseCase @Inject constructor(
    private val repository: DeviceSettingsRepository,
) {
    operator fun invoke(): Flow<DeviceSettings> = repository.observe()
}
