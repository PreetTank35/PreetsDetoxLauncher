package com.preet.detoxlauncher.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.drawable.Drawable
import com.preet.detoxlauncher.data.model.IconPackInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser

class IconPackRepository(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager
    private val iconPackFilters = listOf(
        "org.adw.launcher.THEMES",
        "com.novalauncher.THEME",
        "com.fede.launcher.THEME_ICONPACK",
        "com.anddoes.launcher.THEME",
        "com.teslacoilsw.launcher.THEME"
    )

    /**
     * Discovers all installed icon pack applications on the device.
     */
    suspend fun getInstalledIconPacks(): List<IconPackInfo> = withContext(Dispatchers.IO) {
        val packs = mutableMapOf<String, IconPackInfo>()

        for (action in iconPackFilters) {
            val intent = Intent(action)
            val resolveInfos = packageManager.queryIntentActivities(intent, PackageManager.GET_META_DATA)
            for (resolveInfo in resolveInfos) {
                val pkgName = resolveInfo.activityInfo.packageName
                if (!packs.containsKey(pkgName)) {
                    val label = resolveInfo.loadLabel(packageManager).toString()
                    val icon = resolveInfo.loadIcon(packageManager)
                    packs[pkgName] = IconPackInfo(
                        packageName = pkgName,
                        name = label,
                        icon = icon
                    )
                }
            }
        }
        packs.values.toList().sortedBy { it.name }
    }

    /**
     * Attempts to resolve an icon from the active icon pack for a given component / app.
     */
    suspend fun loadIconFromPack(
        iconPackPackage: String,
        appPackageName: String,
        activityName: String
    ): Drawable? = withContext(Dispatchers.IO) {
        try {
            val iconPackResources = packageManager.getResourcesForApplication(iconPackPackage)
            val componentKey = "ComponentInfo{$appPackageName/$activityName}".lowercase()

            // Try to parse appfilter.xml from the icon pack resources
            val appFilterResId = iconPackResources.getIdentifier("appfilter", "xml", iconPackPackage)
            if (appFilterResId != 0) {
                val parser = iconPackResources.getXml(appFilterResId)
                var eventType = parser.eventType
                var drawableName: String? = null

                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG && parser.name.equals("item", ignoreCase = true)) {
                        val component = parser.getAttributeValue(null, "component")?.lowercase()
                        if (component != null && component.contains(appPackageName.lowercase())) {
                            drawableName = parser.getAttributeValue(null, "drawable")
                            break
                        }
                    }
                    eventType = parser.next()
                }

                if (!drawableName.isNullOrBlank()) {
                    val drawableId = iconPackResources.getIdentifier(drawableName, "drawable", iconPackPackage)
                    if (drawableId != 0) {
                        return@withContext iconPackResources.getDrawable(drawableId, null)
                    }
                }
            }

            // Fallback: try direct drawable lookup by sanitized package name
            val sanitizedName = appPackageName.replace(".", "_").lowercase()
            val directResId = iconPackResources.getIdentifier(sanitizedName, "drawable", iconPackPackage)
            if (directResId != 0) {
                return@withContext iconPackResources.getDrawable(directResId, null)
            }
        } catch (e: Exception) {
            // Icon pack parsing gracefully degraded
        }
        null
    }
}
