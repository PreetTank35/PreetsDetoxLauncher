package com.preet.detoxlauncher.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.preet.detoxlauncher.DetoxApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val app = context.applicationContext as? DetoxApplication ?: return
            app.applicationScope.launch(Dispatchers.IO) {
                // Refresh apps and check reset states
                app.appRepository.refreshInstalledApps()
            }
        }
    }
}
