package com.qhathia.mobile.discovery.core.ui.component

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Whether the voice assistant is enabled for this tablet (remote setting "Asistente por voz"). The
 * app shell provides it; when `false`, every [MicButton] renders nothing, so voice search cannot be
 * started from any screen. Defaults to `true` (previews, tests, and before the first settings sync).
 */
val LocalVoiceAssistantEnabled = staticCompositionLocalOf { true }
