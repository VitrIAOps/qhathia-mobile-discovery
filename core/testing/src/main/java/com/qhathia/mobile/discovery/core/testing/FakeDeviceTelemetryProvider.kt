package com.qhathia.mobile.discovery.core.testing

import com.qhathia.mobile.discovery.core.domain.model.DeviceTelemetry
import com.qhathia.mobile.discovery.core.domain.repository.DeviceTelemetryProvider

/** [DeviceTelemetryProvider] returning a fixed, replaceable [telemetry] snapshot. */
class FakeDeviceTelemetryProvider(
    var telemetry: DeviceTelemetry = DeviceHeartbeatFixtures.telemetry(),
) : DeviceTelemetryProvider {

    override suspend fun current(): DeviceTelemetry = telemetry
}
