package com.qhathia.mobile.discovery.core.domain.repository

import com.qhathia.mobile.discovery.core.domain.model.DeviceTelemetry

/** Port that reads the tablet's current telemetry. Implemented in `core:data` over platform APIs. */
interface DeviceTelemetryProvider {

    /** Returns a fresh telemetry snapshot. Never throws: unreadable fields are `null`. */
    suspend fun current(): DeviceTelemetry
}
