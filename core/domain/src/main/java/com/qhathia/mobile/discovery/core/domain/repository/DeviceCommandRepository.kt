package com.qhathia.mobile.discovery.core.domain.repository

/** Port for acknowledging remote commands (`POST /api/v1/devices/me/commands/{id}/ack`). */
interface DeviceCommandRepository {

    /**
     * Reports the outcome of command [commandId]: [success] marks it done ("Listo"), otherwise failed
     * with [detail] shown to the merchant. Fails without a request when the device is not paired.
     */
    suspend fun ack(commandId: String, success: Boolean, detail: String? = null): Result<Unit>
}
