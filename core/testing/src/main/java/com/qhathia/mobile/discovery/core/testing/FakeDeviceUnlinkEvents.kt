package com.qhathia.mobile.discovery.core.testing

import com.qhathia.mobile.discovery.core.domain.repository.DeviceUnlinkEvents
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** In-memory [DeviceUnlinkEvents]: [emit] raises an unlink event for a request made with `token`. */
class FakeDeviceUnlinkEvents : DeviceUnlinkEvents {

    private val events = MutableSharedFlow<String?>(extraBufferCapacity = 8)

    override fun observe(): Flow<String?> = events.asSharedFlow()

    /** Raises an unlink event; collectors must already be subscribed. */
    fun emit(token: String?) {
        check(events.tryEmit(token)) { "buffer full" }
    }
}
