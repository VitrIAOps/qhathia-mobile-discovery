package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.model.WifiNetwork
import com.qhathia.mobile.discovery.core.domain.repository.NetworkSettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Streams the list of accessible Wi-Fi networks discovered through the platform scan APIs. */
class ObserveWifiNetworksUseCase @Inject constructor(
    private val repository: NetworkSettingsRepository,
) {
    operator fun invoke(): Flow<List<WifiNetwork>> = repository.observeWifiNetworks()
}
