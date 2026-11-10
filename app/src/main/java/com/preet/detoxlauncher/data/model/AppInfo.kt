package com.preet.detoxlauncher.data.model

import android.graphics.drawable.Drawable

/**
 * Domain model representing an installed application.
 */
data class AppInfo(
    val packageName: String,
    val activityName: String,
    val label: String,
    val customLabel: String? = null,
    val customIconPath: String? = null,
    val iconPackPackage: String? = null,
    val iconDrawable: Drawable? = null,
    val isFavorite: Boolean = false,
    val isHidden: Boolean = false,
    val category: String = "General",
    val isEssential: Boolean = false,
    val firstInstallTime: Long = 0L,
    val lastLaunchTime: Long = 0L,
    val launchCount: Int = 0
) {
    val displayLabel: String
        get() = customLabel?.takeIf { it.isNotBlank() } ?: label
}
