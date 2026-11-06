package com.qhathia.mobile.discovery.core.data.unlink

import com.qhathia.mobile.discovery.core.domain.repository.DeviceUnlinkEvents
import com.qhathia.mobile.discovery.core.network.DeviceUnlinkedSignal
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** [DeviceUnlinkEvents] over the HTTP client's [DeviceUnlinkedSignal] (`401 DEVICE_UNLINKED`). */
class DeviceUnlinkEventsImpl @Inject constructor(
    private val signal: DeviceUnlinkedSignal,
) : DeviceUnlinkEvents {

    override fun observe(): Flow<String?> = signal.events
}
