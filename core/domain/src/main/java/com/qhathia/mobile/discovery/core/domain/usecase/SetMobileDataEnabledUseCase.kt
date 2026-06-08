package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.repository.NetworkSettingsRepository
import javax.inject.Inject

/** Requests mobile data be turned on or off; the result reports whether the OS permitted the change. */
class SetMobileDataEnabledUseCase @Inject constructor(
    private val repository: NetworkSettingsRepository,
) {
    suspend operator fun invoke(enabled: Boolean): Result<Unit> = repository.setMobileDataEnabled(enabled)
}
