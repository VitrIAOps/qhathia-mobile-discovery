package com.qhathia.mobile.discovery.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.qhathia.mobile.discovery.MainActivity
import com.qhathia.mobile.discovery.core.domain.usecase.ObserveStoreSessionUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Opens the showcase when a paired tablet finishes booting (boot-autostart, F-12). `MainActivity`
 * then pins itself, and starting the process starts the heartbeat. Android only lets this work up to
 * Android 9; on Android 10+ the showcase comes back as the device home screen instead (D1).
 */
@AndroidEntryPoint
class BootCompletedReceiver : BroadcastReceiver() {

    @Inject
    lateinit var observeStoreSession: ObserveStoreSessionUseCase

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val isPaired = observeStoreSession().first().isPaired
                if (BootLaunchPolicy.shouldOpen(isPaired, isDefaultHome(context))) {
                    context.startActivity(
                        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }
            } finally {
                pending.finish()
            }
        }
    }

    /** Whether Discovery is the home screen the installer chose ("Siempre"). */
    private fun isDefaultHome(context: Context): Boolean {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolved = context.packageManager.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)
        return resolved?.activityInfo?.packageName == context.packageName
    }
}
