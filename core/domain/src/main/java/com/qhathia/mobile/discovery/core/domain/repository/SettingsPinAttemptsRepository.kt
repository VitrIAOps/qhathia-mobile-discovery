package com.qhathia.mobile.discovery.core.domain.repository

import com.qhathia.mobile.discovery.core.domain.model.PinAttempts
import kotlinx.coroutines.flow.Flow

/**
 * Port for the wrong-PIN attempts of the settings gate. Persistent, so a lock survives closing the
 * dialog, restarting the app and rebooting (design D9). The attempts belong to the current exit PIN:
 * when the staff app sets a new PIN or removes it, they read as [PinAttempts.NONE].
 */
interface SettingsPinAttemptsRepository {

    /** The stored attempts for the current PIN, again whenever they or the PIN change. */
    fun observe(): Flow<PinAttempts>

    /** The stored attempts for the current PIN. */
    suspend fun get(): PinAttempts

    /** Stores [attempts] for the current PIN. */
    suspend fun set(attempts: PinAttempts)
}
