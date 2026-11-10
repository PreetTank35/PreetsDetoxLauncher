package com.preet.detoxlauncher.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "detox_rules")
data class DetoxRuleEntity(
    @PrimaryKey
    val packageName: String,
    val mode: String, // SOFT, STRICT, SCHEDULED, OFF
    val dailyLimitMinutes: Int = 0,
    val scheduleStartHour: Int = 22,
    val scheduleStartMinute: Int = 0,
    val scheduleEndHour: Int = 7,
    val scheduleEndMinute: Int = 0,
    val activeDaysString: String = "1,2,3,4,5,6,7", // Comma-delimited integers (Calendar days)
    val requiresIntentionalPrompt: Boolean = false,
    val delaySeconds: Int = 0
)
