package com.qhathia.mobile.discovery.core.data.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import app.cash.turbine.test
import com.qhathia.mobile.discovery.core.domain.model.StoreSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class StoreSessionRepositoryImplTest {

    @TempDir
    lateinit var tempDir: File

    private fun CoroutineScope.repository(): StoreSessionRepositoryImpl {
        val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
            scope = this,
        ) { File(tempDir, "store_session.preferences_pb") }
        return StoreSessionRepositoryImpl(StoreSessionLocalDataSource(dataStore))
    }

    @Test
    fun given_no_value_written_when_observed_then_empty_session() = runTest {
        val repository = backgroundScope.repository()

        assertEquals(StoreSession.EMPTY, repository.observe().first())
    }

    @Test
    fun given_store_name_updated_when_observed_then_value_persists() = runTest {
        val repository = backgroundScope.repository()

        repository.updateStoreName("Joyería María")

        assertEquals("Joyería María", repository.observe().first().storeName)
    }

    @Test
    fun given_observer_when_value_changes_then_emits_current_then_update() = runTest {
        val repository = backgroundScope.repository()

        repository.observe().test {
            assertEquals(StoreSession.EMPTY, awaitItem())

            repository.updateStoreName("Joyería María")

            assertEquals("Joyería María", awaitItem().storeName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun revoke_clears_device_session_and_store_name_and_sets_notice() = runTest {
        val repository = backgroundScope.repository()
        repository.saveDeviceSession("device-jwt", "Tablet 3", "CXZC7")
        repository.updateStoreName("QA Discovery")

        repository.revokeDeviceSession()

        val session = repository.observe().first()
        assertEquals(StoreSession(unlinkedNotice = true), session)
        assertFalse(session.isPaired)
    }

    @Test
    fun dismiss_clears_only_the_notice() = runTest {
        val repository = backgroundScope.repository()
        repository.revokeDeviceSession()
        repository.updateStoreName("Otra tienda")

        repository.dismissUnlinkedNotice()

        assertEquals(StoreSession(storeName = "Otra tienda"), repository.observe().first())
    }

    @Test
    fun no_notice_by_default() = runTest {
        val repository = backgroundScope.repository()
        repository.saveDeviceSession("device-jwt", "Tablet 3", null)

        assertFalse(repository.observe().first().unlinkedNotice)
    }
}
