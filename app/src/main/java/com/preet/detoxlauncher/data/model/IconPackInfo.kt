package com.preet.detoxlauncher.data.model

import android.graphics.drawable.Drawable

/**
 * Metadata representing an installed Icon Pack discovered on the device.
 */
data class IconPackInfo(
    val packageName: String,
    val name: String,
    val icon: Drawable? = null
)
