package com.qhathia.mobile.discovery.core.data.devicelog

import com.qhathia.mobile.discovery.core.domain.repository.DeviceLogRepository
import com.qhathia.mobile.discovery.core.domain.repository.StoreSessionRepository
import com.qhathia.mobile.discovery.core.network.NetworkResult
import com.qhathia.mobile.discovery.core.network.dto.DeviceLogEntryDto
import com.qhathia.mobile.discovery.core.network.dto.DeviceLogsRequestDto
import com.qhathia.mobile.discovery.core.network.service.DeviceLogApiService
import java.time.Clock
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/** [DeviceLogRepository] over [DeviceLogApiService], with the persisted DEVICE token. */
class DeviceLogRepositoryImpl @Inject constructor(
    private val apiService: DeviceLogApiService,
    private val storeSessionRepository: StoreSessionRepository,
    private val clock: Clock,
) : DeviceLogRepository {

    override suspend fun reportPinLockout(failedAttempts: Int, lockSeconds: Long): Result<Unit> {
        val token = storeSessionRepository.deviceAccessToken()
        if (token.isNullOrBlank()) return Result.failure(IllegalStateException("Device is not paired"))

        val entry = DeviceLogEntryDto(
            level = LEVEL_WARN,
            code = CODE_PIN_LOCKOUT,
            message = "Settings PIN locked for $lockSeconds s after $failedAttempts wrong attempts",
            timestamp = clock.instant().truncatedTo(ChronoUnit.SECONDS).toString(),
            context = mapOf(
                CONTEXT_FAILED_ATTEMPTS to failedAttempts.toLong(),
                CONTEXT_LOCKOUT_SECONDS to lockSeconds,
            ),
        )
        return when (val result = apiService.send(token, DeviceLogsRequestDto(listOf(entry)))) {
            is NetworkResult.Success -> Result.success(Unit)
            is NetworkResult.HttpError -> Result.failure(IllegalStateException("HTTP ${result.code}: ${result.message}"))
            is NetworkResult.NetworkError -> Result.failure(result.cause)
        }
    }

    internal companion object {
        const val LEVEL_WARN = "WARN"
        const val CODE_PIN_LOCKOUT = "SETTINGS_PIN_LOCKOUT"
        const val CONTEXT_FAILED_ATTEMPTS = "failedAttempts"
        const val CONTEXT_LOCKOUT_SECONDS = "lockoutSeconds"
    }
}
