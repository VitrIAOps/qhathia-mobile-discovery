package com.qhathia.mobile.discovery.core.data.session

import com.qhathia.mobile.discovery.core.domain.repository.CustomerSessionRepository
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Process-scoped [CustomerSessionRepository]: an in-memory id and end signal, not persisted (design
 * D2). No replay, so a screen created after a session ended never resets the next customer's data.
 */
@Singleton
class CustomerSessionRepositoryImpl @Inject constructor() : CustomerSessionRepository {

    private val ended = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    /** `null` until the current customer first needs an id; cleared when their session ends. */
    private val sessionId = AtomicReference<String?>(null)

    override val sessionEnded: Flow<Unit> = ended.asSharedFlow()

    override fun currentSessionId(): String =
        sessionId.updateAndGet { it ?: UUID.randomUUID().toString() } ?: UUID.randomUUID().toString()

    override suspend fun end() {
        sessionId.set(null)
        ended.emit(Unit)
    }
}
