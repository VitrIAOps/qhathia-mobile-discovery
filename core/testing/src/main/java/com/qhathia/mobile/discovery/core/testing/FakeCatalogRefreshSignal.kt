package com.qhathia.mobile.discovery.core.testing

import com.qhathia.mobile.discovery.core.domain.repository.CatalogRefreshSignal
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Non-replaying [CatalogRefreshSignal] like production; [requestCount] counts [request] calls. */
class FakeCatalogRefreshSignal : CatalogRefreshSignal {

    var requestCount: Int = 0
        private set

    private val requests = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    override fun request() {
        requestCount++
        requests.tryEmit(Unit)
    }

    override fun observe(): Flow<Unit> = requests.asSharedFlow()
}
