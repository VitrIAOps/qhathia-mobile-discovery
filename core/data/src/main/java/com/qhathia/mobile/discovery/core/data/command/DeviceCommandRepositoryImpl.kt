package com.qhathia.mobile.discovery.core.data.command

import com.qhathia.mobile.discovery.core.domain.repository.DeviceCommandRepository
import com.qhathia.mobile.discovery.core.domain.repository.StoreSessionRepository
import com.qhathia.mobile.discovery.core.network.NetworkResult
import com.qhathia.mobile.discovery.core.network.dto.DeviceCommandAckDto
import com.qhathia.mobile.discovery.core.network.service.DeviceCommandApiService
import javax.inject.Inject
import javax.inject.Singleton

/** [DeviceCommandRepository] over [DeviceCommandApiService], authenticated with the DEVICE token. */
@Singleton
class DeviceCommandRepositoryImpl @Inject constructor(
    private val apiService: DeviceCommandApiService,
    private val storeSessionRepository: StoreSessionRepository,
) : DeviceCommandRepository {

    override suspend fun ack(commandId: String, success: Boolean, detail: String?): Result<Unit> {
        val token = storeSessionRepository.deviceAccessToken()
        if (token.isNullOrBlank()) return Result.failure(IllegalStateException("Device is not paired"))

        return when (val result = apiService.ack(token, commandId, DeviceCommandAckDto(success, detail))) {
            is NetworkResult.Success -> Result.success(Unit)
            is NetworkResult.HttpError -> Result.failure(IllegalStateException("HTTP ${result.code}: ${result.message}"))
            is NetworkResult.NetworkError -> Result.failure(result.cause)
        }
    }
}
