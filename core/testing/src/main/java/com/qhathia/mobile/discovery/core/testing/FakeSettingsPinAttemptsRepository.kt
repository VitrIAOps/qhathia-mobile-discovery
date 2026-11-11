package com.qhathia.mobile.discovery.core.testing

import com.qhathia.mobile.discovery.core.domain.model.PinAttempts
import com.qhathia.mobile.discovery.core.domain.repository.SettingsPinAttemptsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory [SettingsPinAttemptsRepository]; [stored] holds the attempts. [afterSet] runs after each
 * store and before [set] returns, so a test can let observers of [observe] react first, as can happen
 * with the real DataStore (F-13).
 */
class FakeSettingsPinAttemptsRepository(
    initial: PinAttempts = PinAttempts.NONE,
    var afterSet: suspend (PinAttempts) -> Unit = {},
) : SettingsPinAttemptsRepository {

    val stored = MutableStateFlow(initial)

    override fun observe(): Flow<PinAttempts> = stored

    override suspend fun get(): PinAttempts = stored.value

    override suspend fun set(attempts: PinAttempts) {
        stored.value = attempts
        afterSet(attempts)
    }
}
