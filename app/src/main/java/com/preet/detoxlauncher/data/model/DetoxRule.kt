package com.preet.detoxlauncher.data.model

/**
 * Domain model representing digital wellbeing restrictions on an application.
 */
data class DetoxRule(
    val packageName: String,
    val mode: RestrictionMode = RestrictionMode.OFF,
    val dailyLimitMinutes: Int = 0,
    val scheduleStartHour: Int = 22,
    val scheduleStartMinute: Int = 0,
    val scheduleEndHour: Int = 7,
    val scheduleEndMinute: Int = 0,
    val activeDays: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7), // 1 = Sunday, 2 = Monday ... 7 = Saturday
    val requiresIntentionalPrompt: Boolean = false,
    val delaySeconds: Int = 0 // 0 = disabled, 5, 10, 15 seconds
)
