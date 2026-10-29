package com.qhathia.mobile.discovery.core.testing

import com.qhathia.mobile.discovery.core.domain.repository.CustomerSessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** In-memory [CustomerSessionRepository] for tests; counts the ends it was asked to signal. */
class FakeCustomerSessionRepository : CustomerSessionRepository {

    private val ended = MutableSharedFlow<Unit>(extraBufferCapacity = 8)

    /** Number of times [end] was called. */
    var endCount: Int = 0
        private set

    override val sessionEnded: Flow<Unit> = ended.asSharedFlow()

    override suspend fun end() {
        endCount++
        ended.emit(Unit)
    }
}
