package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.repository.DeviceSettingsRepository
import com.qhathia.mobile.discovery.core.domain.repository.DeviceUnlinkEvents
import com.qhathia.mobile.discovery.core.domain.repository.HeldPairingCodeRepository
import com.qhathia.mobile.discovery.core.domain.repository.KioskSessionRepository
import com.qhathia.mobile.discovery.core.domain.repository.StoreSessionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Revokes the local device session when the backend reports the tablet as unlinked (design D3, D4).
 * Suspends until cancelled; run it for the process lifetime.
 *
 * Events are handled one at a time, so concurrent `401`s revoke once. An event is ignored when the
 * session is not paired, or when it carries a token other than the stored one (a late answer to a
 * request sent before a new pairing). Otherwise, in order: end the customer session, clear the remote
 * settings, drop the held pairing code and the kiosk-exit flag, and revoke the session last. Removing
 * the token is what flips the app to Welcome, releases the kiosk lock and stops the heartbeat, so it
 * runs after everything else is clean. A failing step never stops the next one.
 */
class HandleDeviceUnlinkedUseCase @Inject constructor(
    private val unlinkEvents: DeviceUnlinkEvents,
    private val storeSessionRepository: StoreSessionRepository,
    private val endCustomerSession: EndCustomerSessionUseCase,
    private val deviceSettingsRepository: DeviceSettingsRepository,
    private val heldPairingCodeRepository: HeldPairingCodeRepository,
    private val kioskSessionRepository: KioskSessionRepository,
) {
    suspend operator fun invoke() {
        // A failed handling (e.g. a DataStore write error) must not end the subscription: the next
        // 401 DEVICE_UNLINKED retries the revocation.
        unlinkEvents.observe().collect { token -> step { handle(token) } }
    }

    private suspend fun handle(rejectedToken: String?) {
        val session = storeSessionRepository.observe().first()
        if (!session.isPaired) return
        if (rejectedToken != null && rejectedToken != session.deviceAccessToken) return

        step { endCustomerSession() }
        step { deviceSettingsRepository.clear() }
        step { heldPairingCodeRepository.set(null) }
        step { kioskSessionRepository.setExitedFromSettings(false) }
        storeSessionRepository.revokeDeviceSession()
    }

    private inline fun step(block: () -> Unit) {
        try {
            block()
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (_: Exception) {
            // Best effort: the session revoke below must still run.
        }
    }
}
