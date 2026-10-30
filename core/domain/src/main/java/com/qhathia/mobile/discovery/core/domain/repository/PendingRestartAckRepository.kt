package com.qhathia.mobile.discovery.core.domain.repository

import com.qhathia.mobile.discovery.core.domain.model.PendingRestartAck

/**
 * Persists the id of a `RESTART_APP` command across the restart, so it is acknowledged only once the
 * app is running again (design D7). At most one is held.
 */
interface PendingRestartAckRepository {

    /** Stores [ack], replacing any previous one. */
    suspend fun save(ack: PendingRestartAck)

    /** The stored restart, or `null`. */
    suspend fun get(): PendingRestartAck?

    /** Forgets the stored restart. */
    suspend fun clear()
}
