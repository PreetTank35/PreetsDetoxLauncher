package com.preet.detoxlauncher.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "apps")
data class AppItemEntity(
    @PrimaryKey
    val packageName: String,
    val activityName: String,
    val systemLabel: String,
    val customLabel: String? = null,
    val customIconPath: String? = null,
    val iconPackPackage: String? = null,
    val isFavorite: Boolean = false,
    val isHidden: Boolean = false,
    val category: String = "General",
    val isEssential: Boolean = false,
    val lastLaunchTime: Long = 0L,
    val launchCount: Int = 0
)
