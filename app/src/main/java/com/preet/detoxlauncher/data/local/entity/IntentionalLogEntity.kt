package com.preet.detoxlauncher.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "intentional_logs")
data class IntentionalLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val packageName: String,
    val reason: String,
    val timestamp: Long = System.currentTimeMillis()
)
