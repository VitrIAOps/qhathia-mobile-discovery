package com.qhathia.mobile.discovery.core.testing

import com.qhathia.mobile.discovery.core.domain.repository.DeviceLogRepository

/** In-memory [DeviceLogRepository]: records each lockout in [lockouts] and answers with [result]. */
class FakeDeviceLogRepository(var result: Result<Unit> = Result.success(Unit)) : DeviceLogRepository {

    /** Each reported lockout as (failed attempts, lock seconds). */
    val lockouts = mutableListOf<Pair<Int, Long>>()

    override suspend fun reportPinLockout(failedAttempts: Int, lockSeconds: Long): Result<Unit> {
        lockouts += failedAttempts to lockSeconds
        return result
    }
}
