package com.qhathia.mobile.discovery.core.voice.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext

/**
 * Gates voice capture on the microphone permission. The returned function runs [onGranted] right away
 * when `RECORD_AUDIO` is granted; otherwise it asks for it and runs [onGranted] only if the user allows
 * it. A refusal shows [deniedMessage].
 */
@Composable
fun rememberVoicePermissionGate(deniedMessage: String, onGranted: () -> Unit): () -> Unit {
    val context = LocalContext.current
    val currentMessage by rememberUpdatedState(deniedMessage)
    val currentOnGranted by rememberUpdatedState(onGranted)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            currentOnGranted()
        } else {
            Toast.makeText(context, currentMessage, Toast.LENGTH_LONG).show()
        }
    }
    return {
        if (context.hasRecordAudioPermission()) {
            currentOnGranted()
        } else {
            launcher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
}

private fun Context.hasRecordAudioPermission(): Boolean =
    checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
