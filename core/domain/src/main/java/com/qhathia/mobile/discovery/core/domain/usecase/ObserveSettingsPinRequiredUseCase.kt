package com.qhathia.mobile.discovery.core.domain.usecase

import com.qhathia.mobile.discovery.core.domain.repository.SettingsAccessRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Whether the settings ask for the exit PIN, again each time the staff app sets or removes it. */
class ObserveSettingsPinRequiredUseCase @Inject constructor(
    private val repository: SettingsAccessRepository,
) {
    operator fun invoke(): Flow<Boolean> = repository.observePinRequired()
}
