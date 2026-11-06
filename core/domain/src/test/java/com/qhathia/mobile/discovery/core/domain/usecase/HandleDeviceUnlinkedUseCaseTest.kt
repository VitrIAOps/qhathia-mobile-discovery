package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.model.DeviceSettings
import com.qhathia.mobile.discovery.core.domain.model.HeldPairingCode
import com.qhathia.mobile.discovery.core.domain.model.PairingCode
import com.qhathia.mobile.discovery.core.domain.model.StoreSession
import com.qhathia.mobile.discovery.core.testing.CartFixtures
import com.qhathia.mobile.discovery.core.testing.FakeCartRepository
import com.qhathia.mobile.discovery.core.testing.FakeCustomerSessionRepository
import com.qhathia.mobile.discovery.core.testing.FakeDeviceSettingsRepository
import com.qhathia.mobile.discovery.core.testing.FakeDeviceUnlinkEvents
import com.qhathia.mobile.discovery.core.testing.FakeHeldPairingCodeRepository
import com.qhathia.mobile.discovery.core.testing.FakeKioskSessionRepository
import com.qhathia.mobile.discovery.core.testing.FakeStoreSessionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class HandleDeviceUnlinkedUseCaseTest {

    private val events = FakeDeviceUnlinkEvents()
    private val session = FakeStoreSessionRepository(PAIRED)
    private val cart = FakeCartRepository(CartFixtures.cart())
    private val customerSession = FakeCustomerSessionRepository()
    private val settings = FakeDeviceSettingsRepository(
        initial = DeviceSettings.DEFAULT.copy(version = 7, kioskMode = true, hasExitPin = true),
    )
    private val heldCode = FakeHeldPairingCodeRepository(
        HeldPairingCode(PairingCode("ABCDE", "s1", Instant.EPOCH, 300), expiresAtElapsedMillis = 1L),
    )
    private val kiosk = FakeKioskSessionRepository(initial = true)

    private fun TestScope.start() {
        val useCase = HandleDeviceUnlinkedUseCase(
            unlinkEvents = events,
            storeSessionRepository = session,
            endCustomerSession = EndCustomerSessionUseCase(cart, customerSession),
            deviceSettingsRepository = settings,
            heldPairingCodeRepository = heldCode,
            kioskSessionRepository = kiosk,
        )
        backgroundScope.launch { useCase() }
        runCurrent()
    }

    @Test
    fun unlink_while_paired_clears_everything_and_revokes() = runTest {
        start()

        events.emit(TOKEN)
        runCurrent()

        assertTrue(cart.current.isEmpty)
        assertEquals(1, customerSession.endCount)
        assertEquals(DeviceSettings.DEFAULT, settings.observe().first())
        assertNull(heldCode.get())
        assertFalse(kiosk.exitedFromSettings.value)
        val revoked = session.current
        assertFalse(revoked.isPaired)
        assertNull(revoked.storeName)
        assertNull(revoked.deviceName)
        assertNull(revoked.pairingCode)
        assertTrue(revoked.unlinkedNotice)
    }

    @Test
    fun unlink_while_unpaired_does_nothing() = runTest {
        session.revokeDeviceSession()
        session.dismissUnlinkedNotice()
        start()

        events.emit(TOKEN)
        runCurrent()

        assertEquals(1, session.revokeCount) // only the setup call
        assertEquals(0, settings.clearCount)
        assertFalse(cart.current.isEmpty)
    }

    @Test
    fun concurrent_events_revoke_once() = runTest {
        start()

        events.emit(TOKEN)
        events.emit(TOKEN)
        runCurrent()

        assertEquals(1, session.revokeCount)
        assertEquals(1, settings.clearCount)
    }

    @Test
    fun event_for_another_token_does_not_revoke() = runTest {
        start()

        events.emit("old-token-before-re-pair")
        runCurrent()

        assertTrue(session.current.isPaired)
        assertEquals(0, session.revokeCount)
        assertEquals(0, settings.clearCount)
    }

    @Test
    fun event_without_token_revokes() = runTest {
        start()

        events.emit(null)
        runCurrent()

        assertFalse(session.current.isPaired)
    }

    @Test
    fun failing_step_still_revokes() = runTest {
        settings.clearFailure = IllegalStateException("disk")
        start()

        events.emit(TOKEN)
        runCurrent()

        assertNull(heldCode.get())
        assertFalse(session.current.isPaired)
        assertTrue(session.current.unlinkedNotice)
    }

    @Test
    fun dismiss_clears_only_the_notice() = runTest {
        start()
        events.emit(TOKEN)
        runCurrent()

        DismissUnlinkedNoticeUseCase(session)()

        assertFalse(session.current.unlinkedNotice)
        assertFalse(session.current.isPaired)
    }

    private companion object {
        const val TOKEN = "device-jwt"
        val PAIRED = StoreSession(
            storeName = "QA Discovery — Ferretería",
            deviceAccessToken = TOKEN,
            deviceName = "Tablet 3",
            pairingCode = "CXZC7",
        )
    }
}
