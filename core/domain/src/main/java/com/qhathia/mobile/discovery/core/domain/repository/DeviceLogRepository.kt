package com.qhathia.mobile.discovery.core.domain.repository

/**
 * Port for the device logs sent to the backend (`POST /api/v1/devices/logs`, api-contract.md). The
 * logs are advisory: a failure is returned, never retried, and never changes what the screen shows.
 */
interface DeviceLogRepository {

    /**
     * Reports that the settings PIN gate locked after [failedAttempts] wrong PINs, for [lockSeconds]
     * (`WARN`, `SETTINGS_PIN_LOCKOUT`). Never carries the PIN or its hash. Fails without a request
     * when the device is not paired.
     */
    suspend fun reportPinLockout(failedAttempts: Int, lockSeconds: Long): Result<Unit>
}
