package com.qhathia.mobile.discovery.core.ui.permission

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
import androidx.compose.ui.res.stringResource
import com.qhathia.mobile.discovery.core.ui.R

/**
 * Gates voice capture on the microphone permission (Home and product search). The returned function
 * runs [onGranted] right away when `RECORD_AUDIO` is granted; otherwise it asks for it and runs
 * [onGranted] only if the consumer allows it. A refusal shows "Necesitamos acceso al micrófono…".
 */
@Composable
fun rememberMicPermissionGate(onGranted: () -> Unit): () -> Unit {
    val context = LocalContext.current
    val deniedMessage = stringResource(R.string.discovery_mic_permission_required)
    val currentOnGranted by rememberUpdatedState(onGranted)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            currentOnGranted()
        } else {
            Toast.makeText(context, deniedMessage, Toast.LENGTH_LONG).show()
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
