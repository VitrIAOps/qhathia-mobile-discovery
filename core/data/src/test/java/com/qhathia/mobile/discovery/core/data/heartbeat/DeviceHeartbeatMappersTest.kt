package com.qhathia.mobile.discovery.core.data.heartbeat

import com.qhathia.mobile.discovery.core.domain.model.DeviceCommand
import com.qhathia.mobile.discovery.core.domain.model.DeviceCommandType
import com.qhathia.mobile.discovery.core.domain.model.DeviceNetworkStatus
import com.qhathia.mobile.discovery.core.network.dto.HeartbeatResponseDto
import com.qhathia.mobile.discovery.core.network.dto.PendingCommandDto
import com.qhathia.mobile.discovery.core.testing.DeviceHeartbeatFixtures
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DeviceHeartbeatMappersTest {

    @Test
    fun telemetry_maps_to_request() {
        val dto = DeviceHeartbeatFixtures.telemetry(networkStatus = DeviceNetworkStatus.MOBILE).toRequestDto()

        assertEquals("1.4.0-alpha", dto.appVersion)
        assertEquals(85, dto.batteryLevel)
        assertEquals("MOBILE", dto.networkStatus)
        assertEquals("Lenovo TB330FU", dto.model)
        assertEquals(3_600L, dto.uptimeSeconds)
    }

    @Test
    fun missing_telemetry_stays_null() {
        val dto = DeviceHeartbeatFixtures.telemetry(batteryLevel = null, networkStatus = null, model = null)
            .toRequestDto()

        assertNull(dto.batteryLevel)
        assertNull(dto.networkStatus)
        assertNull(dto.model)
    }

    @Test
    fun response_maps_versions_and_known_commands() {
        val result = HeartbeatResponseDto(
            serverTime = "2026-10-18T20:05:00Z",
            catalogVersion = 7,
            settingsVersion = 4,
            pendingCommands = listOf(
                PendingCommandDto("c1", "REFRESH_CATALOG"),
                PendingCommandDto("c2", "RESTART_APP"),
            ),
        ).toDomain()

        assertEquals(4L, result.settingsVersion)
        assertEquals(7L, result.catalogVersion)
        assertEquals(
            listOf(
                DeviceCommand("c1", DeviceCommandType.RefreshCatalog),
                DeviceCommand("c2", DeviceCommandType.RestartApp),
            ),
            result.pendingCommands,
        )
    }

    @Test
    fun unknown_command_is_kept_as_unknown() {
        val command = PendingCommandDto("c9", "WIPE_DEVICE").toDomain()

        assertEquals(DeviceCommandType.Unknown("WIPE_DEVICE"), command.type)
    }

    @Test
    fun minimal_response_has_no_versions_or_commands() {
        val result = HeartbeatResponseDto(received = true, serverTime = "t").toDomain()

        assertNull(result.settingsVersion)
        assertNull(result.catalogVersion)
        assertTrue(result.pendingCommands.isEmpty())
    }
}
