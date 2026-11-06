package com.qhathia.mobile.discovery.core.network

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App-wide signal raised by the HTTP client when any call answers `401` with `errorCode:
 * DEVICE_UNLINKED` (api-contract §35). Each event carries the bearer token of the rejected request
 * (`null` when it could not be read), so the consumer only revokes the session that token belongs to.
 * The token is never logged.
 */
@Singleton
class DeviceUnlinkedSignal @Inject constructor() {

    private val _events = MutableSharedFlow<String?>(
        extraBufferCapacity = BUFFER,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    /** Hot stream of unlink events; late subscribers do not get past events. */
    val events: SharedFlow<String?> = _events.asSharedFlow()

    /** Raises an unlink event for the request authenticated with [bearerToken]. Never suspends. */
    fun notifyUnlinked(bearerToken: String?) {
        _events.tryEmit(bearerToken)
    }

    private companion object {
        const val BUFFER = 8
    }
}
