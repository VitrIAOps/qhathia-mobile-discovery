package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.repository.DeviceLogRepository
import javax.inject.Inject
import kotlin.time.Duration

/** Tells the backend the settings PIN gate just locked (settings-pin-lockout D4). */
class ReportPinLockoutUseCase @Inject constructor(
    private val repository: DeviceLogRepository,
) {
    suspend operator fun invoke(failedAttempts: Int, lockDuration: Duration): Result<Unit> =
        repository.reportPinLockout(failedAttempts, lockDuration.inWholeSeconds)
}
