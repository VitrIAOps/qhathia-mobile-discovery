package com.qhathia.mobile.discovery.core.domain

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** Business constants for the device heartbeat (handoff BUG-025, api-contract.md §5/§21/§23). */
object DeviceConstants {

    /**
     * Wait between heartbeat attempts. The staff app treats a tablet as "Conectada" while its last
     * ping is under 10 minutes old, so 60 s tolerates several missed ticks and keeps remote-command
     * latency around one minute.
     */
    val HEARTBEAT_INTERVAL: Duration = 60.seconds

    /** Maximum length the backend accepts for the reported device `model`. */
    const val MODEL_MAX_LENGTH: Int = 100

    /** Valid range for the reported battery level, in percent. */
    val BATTERY_LEVEL_RANGE: IntRange = 0..100

    /** Total attempts to acknowledge one remote command before giving up (the server then expires it). */
    const val ACK_MAX_ATTEMPTS: Int = 3

    /** Wait between failed acknowledgement attempts. */
    val ACK_RETRY_DELAY: Duration = 2.seconds

    /** Maximum `detail` length the ack endpoint accepts. */
    const val ACK_DETAIL_MAX_LENGTH: Int = 300

    /** Ack detail for a command type this app version does not support; `%s` is the raw command. */
    const val ACK_DETAIL_UNSUPPORTED: String = "Comando no soportado: %s"

    /** Ack detail when `RESTART_APP` cannot be carried out. */
    const val ACK_DETAIL_RESTART_UNAVAILABLE: String = "No se pudo reiniciar la app"
}
