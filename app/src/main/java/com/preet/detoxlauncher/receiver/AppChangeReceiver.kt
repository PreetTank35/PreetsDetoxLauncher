package com.preet.detoxlauncher.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.preet.detoxlauncher.DetoxApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AppChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_PACKAGE_ADDED ||
            action == Intent.ACTION_PACKAGE_REMOVED ||
            action == Intent.ACTION_PACKAGE_CHANGED
        ) {
            val app = context.applicationContext as? DetoxApplication ?: return
            app.applicationScope.launch(Dispatchers.IO) {
                app.appRepository.refreshInstalledApps()
            }
        }
    }
}
