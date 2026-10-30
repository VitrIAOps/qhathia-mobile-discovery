package com.qhathia.mobile.discovery.core.domain.model

import java.time.Instant

/** A `RESTART_APP` command to acknowledge after the relaunch, and when it was stored. */
data class PendingRestartAck(val commandId: String, val savedAt: Instant)
