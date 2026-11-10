package com.preet.detoxlauncher.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val startTime: Long,
    val durationMinutes: Int,
    val sessionType: String, // FOCUS, SHORT_BREAK, LONG_BREAK
    val completed: Boolean,
    val notes: String? = null
)
