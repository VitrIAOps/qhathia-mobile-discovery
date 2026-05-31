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
}
