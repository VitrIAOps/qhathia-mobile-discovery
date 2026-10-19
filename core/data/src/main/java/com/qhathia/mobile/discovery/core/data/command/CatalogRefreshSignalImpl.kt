package com.qhathia.mobile.discovery.core.data.command

import com.qhathia.mobile.discovery.core.domain.repository.CatalogRefreshSignal
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory [CatalogRefreshSignal]. No replay: a catalog screen opened after a request loads fresh
 * data on its own and must not refresh a second time.
 */
@Singleton
class CatalogRefreshSignalImpl @Inject constructor() : CatalogRefreshSignal {

    private val requests = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    override fun request() {
        requests.tryEmit(Unit)
    }

    override fun observe(): Flow<Unit> = requests.asSharedFlow()
}
