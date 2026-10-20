package com.qhathia.mobile.discovery.core.data.settings

import com.qhathia.mobile.discovery.core.domain.model.BrightnessMode
import com.qhathia.mobile.discovery.core.domain.model.DeviceSettings
import com.qhathia.mobile.discovery.core.network.dto.DeviceSettingsDto

/** Maps the remote settings to what is stored: the domain settings plus the exit PIN hash. */
internal fun DeviceSettingsDto.toStored(): DeviceSettingsLocalDataSource.Stored {
    val hash = exitPinHash?.takeIf { it.isNotBlank() }
    return DeviceSettingsLocalDataSource.Stored(
        settings = DeviceSettings(
            idleShowcaseEnabled = idleShowcaseEnabled,
            voiceAssistantEnabled = voiceAssistantEnabled,
            brightnessMode = brightnessModeOf(brightnessMode),
            kioskMode = kioskMode,
            hasExitPin = hash != null,
            autoSyncEnabled = autoSyncEnabled,
            version = settingsVersion,
        ),
        exitPinHash = hash,
    )
}

/** Parses a server brightness value; anything unknown means [BrightnessMode.AUTO]. */
internal fun brightnessModeOf(raw: String?): BrightnessMode =
    BrightnessMode.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: BrightnessMode.AUTO
