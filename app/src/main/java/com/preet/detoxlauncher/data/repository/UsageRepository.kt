package com.preet.detoxlauncher.data.repository

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Process
import com.preet.detoxlauncher.data.model.UsageStatSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

class UsageRepository(private val context: Context) {

    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
    private val packageManager: PackageManager = context.packageManager

    /**
     * Checks whether the user has granted PACKAGE_USAGE_STATS via Android Settings.
     */
    fun hasUsagePermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Calculates the total device screen time in milliseconds since midnight.
     */
    suspend fun getTodayTotalScreenTimeMillis(): Long = withContext(Dispatchers.IO) {
        if (!hasUsagePermission() || usageStatsManager == null) return@withContext 0L

        val (startTime, endTime) = getTodayTimeRange()
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        ) ?: return@withContext 0L

        var totalMillis = 0L
        for (usage in stats) {
            // Exclude own launcher from screen time total
            if (usage.packageName != context.packageName && usage.totalTimeInForeground > 0) {
                totalMillis += usage.totalTimeInForeground
            }
        }
        totalMillis
    }

    /**
     * Returns individual application usage metrics for today, ranked from highest to lowest.
     */
    suspend fun getTodayAppUsageList(): List<UsageStatSummary> = withContext(Dispatchers.IO) {
        if (!hasUsagePermission() || usageStatsManager == null) return@withContext emptyList()

        val (startTime, endTime) = getTodayTimeRange()
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        ) ?: return@withContext emptyList()

        val aggregatedMap = mutableMapOf<String, Long>()
        val lastUsedMap = mutableMapOf<String, Long>()

        for (usage in stats) {
            if (usage.packageName == context.packageName) continue
            val current = aggregatedMap[usage.packageName] ?: 0L
            aggregatedMap[usage.packageName] = current + usage.totalTimeInForeground
            val lastUsed = lastUsedMap[usage.packageName] ?: 0L
            if (usage.lastTimeUsed > lastUsed) {
                lastUsedMap[usage.packageName] = usage.lastTimeUsed
            }
        }

        aggregatedMap
            .filter { it.value >= 60 * 1000L } // Filter out apps used less than 1 minute
            .map { (pkg, duration) ->
                val appLabel = try {
                    val appInfo = packageManager.getApplicationInfo(pkg, 0)
                    packageManager.getApplicationLabel(appInfo).toString()
                } catch (e: Exception) {
                    pkg
                }
                UsageStatSummary(
                    packageName = pkg,
                    appName = appLabel,
                    totalTimeForegroundMillis = duration,
                    lastTimeUsedMillis = lastUsedMap[pkg] ?: 0L
                )
            }
            .sortedByDescending { it.totalTimeForegroundMillis }
    }

    /**
     * Computes today's phone unlock count using UsageEvents.
     */
    suspend fun getTodayUnlockCount(): Int = withContext(Dispatchers.IO) {
        if (!hasUsagePermission() || usageStatsManager == null) return@withContext 0

        val (startTime, endTime) = getTodayTimeRange()
        val events = usageStatsManager.queryEvents(startTime, endTime) ?: return@withContext 0

        var unlockCount = 0
        val event = UsageEvents.Event()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            // KEYGUARD_HIDDEN (eventType = 18) signals a successful device unlock
            if (event.eventType == 18 || event.eventType == UsageEvents.Event.SCREEN_INTERACTIVE) {
                if (event.eventType == 18) {
                    unlockCount++
                }
            }
        }
        unlockCount
    }

    /**
     * Returns today's screen time in milliseconds for a specific package.
     */
    suspend fun getAppScreenTimeToday(packageName: String): Long = withContext(Dispatchers.IO) {
        if (!hasUsagePermission() || usageStatsManager == null) return@withContext 0L

        val (startTime, endTime) = getTodayTimeRange()
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        ) ?: return@withContext 0L

        stats.filter { it.packageName == packageName }
            .sumOf { it.totalTimeInForeground }
    }

    private fun getTodayTimeRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()
        return Pair(startTime, endTime)
    }
}
