package com.qhathia.mobile.discovery.core.testing

import com.qhathia.mobile.discovery.core.domain.model.PinAttempts
import com.qhathia.mobile.discovery.core.domain.repository.SettingsPinAttemptsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory [SettingsPinAttemptsRepository]; [stored] holds the attempts. */
class FakeSettingsPinAttemptsRepository(initial: PinAttempts = PinAttempts.NONE) : SettingsPinAttemptsRepository {

    val stored = MutableStateFlow(initial)

    override fun observe(): Flow<PinAttempts> = stored

    override suspend fun get(): PinAttempts = stored.value

    override suspend fun set(attempts: PinAttempts) {
        stored.value = attempts
    }
}
