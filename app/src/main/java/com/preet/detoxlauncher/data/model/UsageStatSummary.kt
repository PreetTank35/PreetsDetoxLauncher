package com.preet.detoxlauncher.data.model

/**
 * Aggregated screen-time metrics for an application or overall device.
 */
data class UsageStatSummary(
    val packageName: String,
    val appName: String,
    val totalTimeForegroundMillis: Long,
    val lastTimeUsedMillis: Long,
    val launchCount: Int = 0
) {
    val totalTimeMinutes: Long
        get() = totalTimeForegroundMillis / (1000 * 60)

    val formattedDuration: String
        get() {
            val hours = totalTimeMinutes / 60
            val mins = totalTimeMinutes % 60
            return if (hours > 0) {
                "${hours}h ${mins}m"
            } else {
                "${mins}m"
            }
        }
}
