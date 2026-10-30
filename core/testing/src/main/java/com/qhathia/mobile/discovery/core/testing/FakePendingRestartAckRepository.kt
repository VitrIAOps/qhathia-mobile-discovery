package com.qhathia.mobile.discovery.core.testing

import com.qhathia.mobile.discovery.core.domain.model.PendingRestartAck
import com.qhathia.mobile.discovery.core.domain.repository.PendingRestartAckRepository

/** In-memory [PendingRestartAckRepository]; [stored] is the held restart. */
class FakePendingRestartAckRepository(var stored: PendingRestartAck? = null) : PendingRestartAckRepository {

    override suspend fun save(ack: PendingRestartAck) {
        stored = ack
    }

    override suspend fun get(): PendingRestartAck? = stored

    override suspend fun clear() {
        stored = null
    }
}
