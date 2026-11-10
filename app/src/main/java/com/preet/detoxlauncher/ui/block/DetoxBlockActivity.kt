package com.preet.detoxlauncher.ui.block

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.preet.detoxlauncher.DetoxApplication
import com.preet.detoxlauncher.ui.theme.PreetsDetoxLauncherTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class DetoxBlockActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val packageNameExtra = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: "Application"
        val reasonExtra = intent.getStringExtra(EXTRA_REASON) ?: "Daily digital wellbeing limit reached."
        val resetTimeExtra = intent.getStringExtra(EXTRA_RESET_TIME) ?: "Midnight"

        val app = application as DetoxApplication
        val settings = runBlocking { app.userPreferences.settingsFlow.first() }

        setContent {
            PreetsDetoxLauncherTheme(
                isPureAmoled = settings.isPureAmoled,
                accentIndex = settings.accentColorIndex,
                isLightMode = settings.isLightMode
            ) {
                DetoxBlockScreen(
                    packageName = packageNameExtra,
                    reason = reasonExtra,
                    resetTime = resetTimeExtra,
                    overridesRemaining = settings.emergencyOverridesRemainingToday,
                    onEmergencyOverride = {
                        runBlocking {
                            val success = app.userPreferences.useEmergencyOverride()
                            if (success) {
                                finish()
                            }
                        }
                    },
                    onReturnHome = {
                        // Return to Home Launcher
                        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                            addCategory(Intent.CATEGORY_HOME)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        startActivity(homeIntent)
                        finish()
                    }
                )
            }
        }
    }

    companion object {
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_REASON = "extra_reason"
        const val EXTRA_RESET_TIME = "extra_reset_time"
    }
}
