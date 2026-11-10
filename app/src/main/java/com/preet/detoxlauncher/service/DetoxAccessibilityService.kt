package com.preet.detoxlauncher.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.preet.detoxlauncher.DetoxApplication
import com.preet.detoxlauncher.data.repository.LaunchDecision
import com.preet.detoxlauncher.ui.block.DetoxBlockActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Optional on-device Accessibility Service that observes foreground window transitions
 * to proactively intercept applications exceeding strict detox limits.
 *
 * Privacy Guarantees:
 * - Only reads target package name.
 * - Does NOT inspect view hierarchy, text, or keystrokes (canRetrieveWindowContent = false).
 * - 100% on-device operation, zero network transmission.
 */
class DetoxAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastInterceptedPackage: String? = null
    private var lastInterceptTimestamp: Long = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val targetPackage = event.packageName?.toString() ?: return
        if (targetPackage == packageName || targetPackage == "android" || targetPackage == "com.android.systemui") {
            return
        }

        val now = System.currentTimeMillis()
        // Debounce rapid window changes for the same app
        if (targetPackage == lastInterceptedPackage && (now - lastInterceptTimestamp) < 1500) {
            return
        }

        serviceScope.launch {
            val app = DetoxApplication.instance
            val decision = app.detoxRepository.evaluateAppLaunch(targetPackage)

            when (decision) {
                is LaunchDecision.StrictBlock -> {
                    lastInterceptedPackage = targetPackage
                    lastInterceptTimestamp = now
                    launchBlockScreen(
                        packageName = targetPackage,
                        reason = "Daily limit of ${decision.limitMinutes}m reached (${decision.currentUsageMinutes}m used).",
                        resetTime = decision.resetTimeFormatted
                    )
                }
                is LaunchDecision.ScheduledBlock -> {
                    lastInterceptedPackage = targetPackage
                    lastInterceptTimestamp = now
                    launchBlockScreen(
                        packageName = targetPackage,
                        reason = "Restricted by active schedule (${decision.windowString}).",
                        resetTime = "End of schedule"
                    )
                }
                else -> {
                    // Allowed or soft restriction (soft blocks are handled inside the launcher itself)
                }
            }
        }
    }

    private fun launchBlockScreen(packageName: String, reason: String, resetTime: String) {
        val blockIntent = Intent(this, DetoxBlockActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NO_HISTORY
            putExtra(DetoxBlockActivity.EXTRA_PACKAGE_NAME, packageName)
            putExtra(DetoxBlockActivity.EXTRA_REASON, reason)
            putExtra(DetoxBlockActivity.EXTRA_RESET_TIME, resetTime)
        }
        startActivity(blockIntent)
    }

    override fun onInterrupt() {
        // Required callback when service interrupted
    }
}
