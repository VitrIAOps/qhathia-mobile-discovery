package com.qhathia.mobile.discovery.core.device.management

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RemoteSettingsSyncTest {

    private val source = FakeRemoteSettingsSource()

    private fun TestScope.start() {
        backgroundScope.launch { RemoteSettingsSync(source).run() }
        runCurrent()
    }

    private suspend fun TestScope.report(version: Long?) {
        source.pendingVersion = version
        source.reportedVersions.emit(version)
        runCurrent()
    }

    @Test
    fun first_reported_version_triggers_one_refresh() = runTest {
        start()
        report(3)
        report(3)
        assertEquals(1, source.refreshes)
    }

    @Test
    fun stored_version_reported_again_does_not_refresh() = runTest {
        source.stored = 3
        start()
        report(3)
        assertEquals(0, source.refreshes)
    }

    @Test
    fun new_version_triggers_refresh() = runTest {
        source.stored = 3
        start()
        report(4)
        assertEquals(1, source.refreshes)
    }

    @Test
    fun failed_refresh_is_retried_on_the_next_report() = runTest {
        source.refreshSucceeds = false
        start()
        report(4)
        source.refreshSucceeds = true
        report(4)
        report(4)
        assertEquals(2, source.refreshes)
    }

    @Test
    fun missing_version_is_ignored() = runTest {
        start()
        report(null)
        assertEquals(0, source.refreshes)
    }
}
