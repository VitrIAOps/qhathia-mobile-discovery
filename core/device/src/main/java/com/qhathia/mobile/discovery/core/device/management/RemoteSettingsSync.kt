package com.qhathia.mobile.discovery.core.device.management

import kotlinx.coroutines.flow.filterNotNull

/** Reads the remote settings again whenever a report announces a version other than the stored one. */
class RemoteSettingsSync(private val source: RemoteSettingsSource) {

    /** Runs until cancelled. */
    suspend fun run() {
        source.reportedVersions
            .filterNotNull()
            .collect { reported ->
                if (reported != source.storedVersion()) {
                    // A failure leaves the stored version unchanged, so the next report retries.
                    source.refresh()
                }
            }
    }
}
