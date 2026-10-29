package com.qhathia.mobile.discovery.core.data.pairing

import com.qhathia.mobile.discovery.core.domain.model.HeldPairingCode
import com.qhathia.mobile.discovery.core.domain.repository.HeldPairingCodeRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory [HeldPairingCodeRepository]. Process-scoped on purpose: after a restart the server's
 * session may be gone, and generating a new code is the safe choice.
 */
@Singleton
class HeldPairingCodeRepositoryImpl @Inject constructor() : HeldPairingCodeRepository {

    @Volatile
    private var held: HeldPairingCode? = null

    override fun get(): HeldPairingCode? = held

    override fun set(code: HeldPairingCode?) {
        held = code
    }
}
