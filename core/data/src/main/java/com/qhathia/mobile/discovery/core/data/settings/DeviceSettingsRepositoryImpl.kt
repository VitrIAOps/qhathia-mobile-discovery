package com.qhathia.mobile.discovery.core.data.settings

import com.qhathia.mobile.discovery.core.domain.model.DeviceSettings
import com.qhathia.mobile.discovery.core.domain.repository.DeviceSettingsRepository
import com.qhathia.mobile.discovery.core.domain.repository.StoreSessionRepository
import com.qhathia.mobile.discovery.core.network.NetworkResult
import com.qhathia.mobile.discovery.core.network.service.DeviceSettingsApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

/** [DeviceSettingsRepository] over [DeviceSettingsApiService] with a DataStore cache. */
@Singleton
class DeviceSettingsRepositoryImpl @Inject constructor(
    private val apiService: DeviceSettingsApiService,
    private val localDataSource: DeviceSettingsLocalDataSource,
    private val storeSessionRepository: StoreSessionRepository,
) : DeviceSettingsRepository {

    override fun observe(): Flow<DeviceSettings> = localDataSource.observe().distinctUntilChanged()

    override suspend fun storedVersion(): Long? = localDataSource.storedVersion()

    override suspend fun refresh(): Result<DeviceSettings> {
        val token = storeSessionRepository.deviceAccessToken()
        if (token.isNullOrBlank()) return Result.failure(IllegalStateException("Device is not paired"))

        return when (val result = apiService.getMySettings(token)) {
            is NetworkResult.Success -> {
                val stored = result.data.toStored()
                localDataSource.save(stored)
                Result.success(stored.settings)
            }
            is NetworkResult.HttpError -> Result.failure(IllegalStateException("HTTP ${result.code}: ${result.message}"))
            is NetworkResult.NetworkError -> Result.failure(result.cause)
        }
    }
}
