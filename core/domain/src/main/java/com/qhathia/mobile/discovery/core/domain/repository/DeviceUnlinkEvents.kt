package com.qhathia.mobile.discovery.core.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Port for "the backend says this tablet was unlinked" (`401 DEVICE_UNLINKED`, api-contract §35),
 * raised by any call. Each event carries the bearer token of the rejected request, or `null` when it
 * could not be read.
 */
interface DeviceUnlinkEvents {

    /** Hot stream of unlink events; late subscribers do not get past events. */
    fun observe(): Flow<String?>
}
