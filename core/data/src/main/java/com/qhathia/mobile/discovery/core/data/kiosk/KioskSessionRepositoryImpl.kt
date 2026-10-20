package com.qhathia.mobile.discovery.core.data.kiosk

import com.qhathia.mobile.discovery.core.domain.repository.KioskSessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Process-scoped [KioskSessionRepository]. Deliberately not persisted: a forgotten exit must not leave
 * the tablet unlocked after a restart (design D2).
 */
@Singleton
class KioskSessionRepositoryImpl @Inject constructor() : KioskSessionRepository {

    private val exited = MutableStateFlow(false)

    override val exitedFromSettings: StateFlow<Boolean> = exited.asStateFlow()

    override fun setExitedFromSettings(exited: Boolean) {
        this.exited.value = exited
    }
}
