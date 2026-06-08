package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.repository.NetworkSettingsRepository
import javax.inject.Inject

/** Requests Wi-Fi be turned on or off; the result reports whether the OS permitted the change. */
class SetWifiEnabledUseCase @Inject constructor(
    private val repository: NetworkSettingsRepository,
) {
    suspend operator fun invoke(enabled: Boolean): Result<Unit> = repository.setWifiEnabled(enabled)
}
