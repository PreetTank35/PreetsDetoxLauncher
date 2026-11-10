package com.preet.detoxlauncher.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_challenges")
data class DailyChallengeEntity(
    @PrimaryKey
    val id: String, // e.g. "CHALLENGE_2026-11-10_1"
    val dateString: String, // "yyyy-MM-dd"
    val title: String,
    val description: String,
    val targetValue: Int,
    val currentValue: Int,
    val completed: Boolean
)
