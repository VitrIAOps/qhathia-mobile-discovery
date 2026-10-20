package com.qhathia.mobile.discovery.core.domain.model

/**
 * A remote command queued by the merchant and delivered in a heartbeat response (api-contract.md
 * §23). A delivered command is never re-delivered by the server.
 *
 * @property id server id, used to acknowledge the command.
 * @property type what the tablet must do.
 */
data class DeviceCommand(
    val id: String,
    val type: DeviceCommandType,
)

/** Command kinds the server can send. Unrecognised values map to [Unknown] instead of failing. */
sealed interface DeviceCommandType {
    /** Reload the catalog outside the normal cycle. */
    data object RefreshCatalog : DeviceCommandType

    /** Restart the kiosk app (not the operating system). */
    data object RestartApp : DeviceCommandType

    /** A command this app version does not know; [raw] is the value sent by the server. */
    data class Unknown(val raw: String) : DeviceCommandType
}
