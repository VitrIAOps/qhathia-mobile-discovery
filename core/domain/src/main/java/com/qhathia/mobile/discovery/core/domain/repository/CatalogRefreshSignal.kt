package com.qhathia.mobile.discovery.core.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * In-process signal asking any open catalog screen to reload its data (remote `REFRESH_CATALOG`).
 * Requests are not replayed: a screen that starts observing later loads fresh data on its own.
 */
interface CatalogRefreshSignal {

    /** Asks current observers to refresh. A no-op when nobody is observing. */
    fun request()

    /** Emits once per [request] made while collecting. */
    fun observe(): Flow<Unit>
}
