package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.repository.NetworkSettingsRepository
import javax.inject.Inject

/**
 * Connects to a Wi-Fi network by [ssid], using [password] for a secured network or `null` for an
 * open one. The result reports the connection outcome.
 */
class ConnectToWifiUseCase @Inject constructor(
    private val repository: NetworkSettingsRepository,
) {
    suspend operator fun invoke(ssid: String, password: String?): Result<Unit> =
        repository.connect(ssid, password)
}
