package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.repository.NetworkSettingsRepository
import javax.inject.Inject

/** "Buscar redes": starts a Wi-Fi scan; the observed network list refreshes with the results. */
class RescanWifiNetworksUseCase @Inject constructor(
    private val repository: NetworkSettingsRepository,
) {
    operator fun invoke() = repository.rescan()
}
