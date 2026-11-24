package com.qhathia.mobile.discovery.core.ui.permission

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.qhathia.mobile.discovery.core.ui.R
import com.qhathia.mobile.discovery.core.voice.ui.rememberVoicePermissionGate

/**
 * Gates voice capture on the microphone permission (Home and product search). A refusal shows
 * "Necesitamos acceso al micrófono…".
 */
@Composable
fun rememberMicPermissionGate(onGranted: () -> Unit): () -> Unit =
    rememberVoicePermissionGate(
        deniedMessage = stringResource(R.string.discovery_mic_permission_required),
        onGranted = onGranted,
    )
