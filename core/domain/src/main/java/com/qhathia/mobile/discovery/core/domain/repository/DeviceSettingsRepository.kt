package com.qhathia.mobile.discovery.core.domain.repository

import com.qhathia.mobile.discovery.core.domain.model.DeviceSettings
import kotlinx.coroutines.flow.Flow

/** Port for the tablet's remote settings (`GET /api/v1/devices/me/settings`) and their local cache. */
interface DeviceSettingsRepository {

    /** Emits the stored settings ([DeviceSettings.DEFAULT] before the first sync) and every change. */
    fun observe(): Flow<DeviceSettings>

    /** The `settingsVersion` of the stored settings, or `null` when never synced. */
    suspend fun storedVersion(): Long?

    /** Reads the remote settings and stores them. Fails without a request when not paired. */
    suspend fun refresh(): Result<DeviceSettings>
}
