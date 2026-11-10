package com.preet.detoxlauncher.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "detox_streaks")
data class DetoxStreakEntity(
    @PrimaryKey
    val dateString: String, // "yyyy-MM-dd"
    val goalMinutes: Int,
    val actualMinutes: Int,
    val goalMet: Boolean,
    val unlockCount: Int = 0
)
