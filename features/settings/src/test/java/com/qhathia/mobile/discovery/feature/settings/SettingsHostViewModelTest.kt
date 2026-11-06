package com.qhathia.mobile.discovery.feature.settings

import app.cash.turbine.test
import com.qhathia.mobile.discovery.core.domain.model.StoreSession
import com.qhathia.mobile.discovery.core.domain.usecase.ObserveStoreSessionUseCase
import com.qhathia.mobile.discovery.core.testing.FakeStoreSessionRepository
import com.qhathia.mobile.discovery.core.testing.MainDispatcherExtension
import com.qhathia.mobile.discovery.feature.settings.ui.SettingsHostViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsHostViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    @Test
    fun store_name_follows_the_session() = runTest(mainDispatcher.dispatcher) {
        val session = FakeStoreSessionRepository(StoreSession(deviceAccessToken = "jwt"))
        val vm = SettingsHostViewModel(ObserveStoreSessionUseCase(session))

        vm.storeName.test {
            assertNull(expectMostRecentItem())
            session.updateStoreName("QA Discovery — Ferretería")
            assertEquals("QA Discovery — Ferretería", awaitItem())
        }
    }

    @Test
    fun blank_store_name_reads_as_unknown() = runTest(mainDispatcher.dispatcher) {
        val session = FakeStoreSessionRepository(StoreSession(deviceAccessToken = "jwt", storeName = "  "))

        SettingsHostViewModel(ObserveStoreSessionUseCase(session)).storeName.test {
            assertNull(expectMostRecentItem())
        }
    }
}
