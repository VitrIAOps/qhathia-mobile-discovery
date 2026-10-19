package com.qhathia.mobile.discovery.core.testing

import com.qhathia.mobile.discovery.core.domain.repository.DeviceCommandRepository

/**
 * Records every ack attempt in [acks]. The first [failuresBeforeSuccess] attempts fail, the rest
 * succeed — enough to exercise the retry policy.
 */
class FakeDeviceCommandRepository(
    var failuresBeforeSuccess: Int = 0,
) : DeviceCommandRepository {

    data class Ack(val commandId: String, val success: Boolean, val detail: String?)

    val acks: MutableList<Ack> = mutableListOf()

    override suspend fun ack(commandId: String, success: Boolean, detail: String?): Result<Unit> {
        acks += Ack(commandId, success, detail)
        return if (acks.size <= failuresBeforeSuccess) {
            Result.failure(IllegalStateException("ack failed"))
        } else {
            Result.success(Unit)
        }
    }
}
