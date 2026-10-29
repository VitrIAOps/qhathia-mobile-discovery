package com.qhathia.mobile.discovery.core.data.session

import com.qhathia.mobile.discovery.core.domain.repository.CustomerSessionRepository
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Process-scoped [CustomerSessionRepository]: an in-memory signal, not persisted (design D2). No
 * replay, so a screen created after a session ended never resets the next customer's data.
 */
@Singleton
class CustomerSessionRepositoryImpl @Inject constructor() : CustomerSessionRepository {

    private val ended = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    override val sessionEnded: Flow<Unit> = ended.asSharedFlow()

    override suspend fun end() {
        ended.emit(Unit)
    }
}
