package com.qhathia.mobile.discovery.core.domain.model

/**
 * The tablet's remote settings, set by the merchant from the staff app (api-contract.md §21) and
 * cached locally. The exit PIN hash is deliberately not part of this model: it stays in `core:data`.
 *
 * @property idleShowcaseEnabled "Modo Vitrina en reposo" (stored, not applied yet).
 * @property voiceAssistantEnabled "Asistente por voz": when `false` the microphone controls are hidden.
 * @property brightnessMode "Brillo".
 * @property kioskMode "Modo kiosco" (applied by the kiosk lock change).
 * @property hasExitPin whether a "PIN de salida" is set.
 * @property autoSyncEnabled "Sincronizar automáticamente" (stored, not applied yet).
 * @property version server `settingsVersion` of these values, or `null` when never synced.
 */
data class DeviceSettings(
    val idleShowcaseEnabled: Boolean = true,
    val voiceAssistantEnabled: Boolean = true,
    val brightnessMode: BrightnessMode = BrightnessMode.AUTO,
    val kioskMode: Boolean = true,
    val hasExitPin: Boolean = false,
    val autoSyncEnabled: Boolean = true,
    val version: Long? = null,
) {
    companion object {
        /** Server defaults, used until the first sync. */
        val DEFAULT: DeviceSettings = DeviceSettings()
    }
}

/** Screen brightness intent; Android resolves the actual level. */
enum class BrightnessMode { AUTO, LOW, MEDIUM, HIGH }
