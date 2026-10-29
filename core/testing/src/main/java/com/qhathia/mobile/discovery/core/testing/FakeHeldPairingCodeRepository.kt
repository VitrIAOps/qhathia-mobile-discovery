package com.qhathia.mobile.discovery.core.testing

import com.qhathia.mobile.discovery.core.domain.model.HeldPairingCode
import com.qhathia.mobile.discovery.core.domain.repository.HeldPairingCodeRepository

/** In-memory [HeldPairingCodeRepository]; share one instance between ViewModels to simulate leaving and coming back. */
class FakeHeldPairingCodeRepository(var held: HeldPairingCode? = null) : HeldPairingCodeRepository {

    override fun get(): HeldPairingCode? = held

    override fun set(code: HeldPairingCode?) {
        held = code
    }
}
