package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.model.NetworkConnectivityState
import com.qhathia.mobile.discovery.core.domain.repository.NetworkSettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Streams the device [NetworkConnectivityState] (Wi-Fi, mobile data, airplane mode, connected SSID). */
class ObserveNetworkStateUseCase @Inject constructor(
    private val repository: NetworkSettingsRepository,
) {
    operator fun invoke(): Flow<NetworkConnectivityState> = repository.observeConnectivity()
}
