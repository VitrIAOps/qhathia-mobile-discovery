package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.repository.SettingsAccessRepository
import javax.inject.Inject

/** Whether opening the settings must ask for the exit PIN (`false` when no PIN is set). */
class IsSettingsPinRequiredUseCase @Inject constructor(
    private val repository: SettingsAccessRepository,
) {
    suspend operator fun invoke(): Boolean = repository.isPinRequired()
}
