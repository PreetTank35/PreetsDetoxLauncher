package com.preet.detoxlauncher.data.repository

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.MediaStore
import android.provider.Telephony
import android.util.LruCache
import com.preet.detoxlauncher.data.local.dao.AppDao
import com.preet.detoxlauncher.data.local.entity.AppItemEntity
import com.preet.detoxlauncher.data.model.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class AppRepository(
    private val context: Context,
    private val appDao: AppDao,
    private val iconPackRepository: IconPackRepository
) {
    private val packageManager: PackageManager = context.packageManager
    private val iconCache = LruCache<String, Drawable>(150)

    /**
     * Resolves the set of critical system applications that must never be restricted.
     */
    private val essentialPackages: Set<String> by lazy {
        val essential = mutableSetOf<String>()
        
        // Settings
        essential.add("com.android.settings")

        // Default Phone / Dialer
        val dialerIntent = Intent(Intent.ACTION_DIAL)
        packageManager.resolveActivity(dialerIntent, 0)?.activityInfo?.packageName?.let {
            essential.add(it)
        }

        // Default SMS
        Telephony.Sms.getDefaultSmsPackage(context)?.let {
            essential.add(it)
        }

        // Camera
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        packageManager.resolveActivity(cameraIntent, 0)?.activityInfo?.packageName?.let {
            essential.add(it)
        }

        // Emergency Dialer
        essential.add("com.android.phone")
        essential.add("com.android.server.telecom")
        essential
    }

    /**
     * Scans installed applications from the system and synchronizes with local Room overrides.
     */
    suspend fun refreshInstalledApps() = withContext(Dispatchers.IO) {
        val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = packageManager.queryIntentActivities(launcherIntent, 0)

        val discoveredEntities = resolveInfos.mapNotNull { resolveInfo ->
            val pkg = resolveInfo.activityInfo.packageName
            val activity = resolveInfo.activityInfo.name
            if (pkg == context.packageName) return@mapNotNull null // Don't list own launcher in app drawer

            val label = resolveInfo.loadLabel(packageManager).toString()
            val isEssentialApp = essentialPackages.contains(pkg)

            // Check if existing record has overrides
            val existing = appDao.getAppByPackage(pkg)
            existing?.copy(
                activityName = activity,
                systemLabel = label,
                isEssential = isEssentialApp
            ) ?: AppItemEntity(
                packageName = pkg,
                activityName = activity,
                systemLabel = label,
                isEssential = isEssentialApp,
                category = if (isEssentialApp) "Utilities" else "General"
            )
        }

        appDao.upsertApps(discoveredEntities)
    }

    /**
     * Returns flow of all visible applications, merged with dynamic icons.
     */
    fun getVisibleApps(activeIconPack: String?): Flow<List<AppInfo>> {
        return appDao.getVisibleApps().map { visibleEntities ->
            visibleEntities.map { entity ->
                mapEntityToAppInfo(entity, activeIconPack)
            }
        }
    }

    /**
     * Returns favorite applications.
     */
    fun getFavoriteApps(activeIconPack: String?): Flow<List<AppInfo>> {
        return appDao.getFavoriteApps().map { list ->
            list.map { entity -> mapEntityToAppInfo(entity, activeIconPack) }
        }
    }

    /**
     * Returns all applications, including hidden ones (for management screens).
     */
    fun getAllApps(activeIconPack: String?): Flow<List<AppInfo>> {
        return appDao.getAllApps().map { list ->
            list.map { entity -> mapEntityToAppInfo(entity, activeIconPack) }
        }
    }

    private fun mapEntityToAppInfo(entity: AppItemEntity, activeIconPack: String?): AppInfo {
        return AppInfo(
            packageName = entity.packageName,
            activityName = entity.activityName,
            label = entity.systemLabel,
            customLabel = entity.customLabel,
            customIconPath = entity.customIconPath,
            iconPackPackage = activeIconPack,
            iconDrawable = loadAppIcon(entity, activeIconPack),
            isFavorite = entity.isFavorite,
            isHidden = entity.isHidden,
            category = entity.category,
            isEssential = entity.isEssential,
            lastLaunchTime = entity.lastLaunchTime,
            launchCount = entity.launchCount
        )
    }

    /**
     * Resolves app icon with priority: Custom Image File -> Icon Pack -> System Default.
     */
    fun loadAppIcon(entity: AppItemEntity, activeIconPack: String?): Drawable? {
        val cacheKey = "${entity.packageName}_${entity.customIconPath ?: ""}_${activeIconPack ?: ""}"
        iconCache.get(cacheKey)?.let { return it }

        // 1. Custom User Image from internal storage
        if (!entity.customIconPath.isNullOrBlank()) {
            val file = File(entity.customIconPath)
            if (file.exists()) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                if (bitmap != null) {
                    val drawable = BitmapDrawable(context.resources, bitmap)
                    iconCache.put(cacheKey, drawable)
                    return drawable
                }
            }
        }

        // 2. System Default Icon
        return try {
            val drawable = packageManager.getActivityIcon(
                ComponentName(entity.packageName, entity.activityName)
            )
            iconCache.put(cacheKey, drawable)
            drawable
        } catch (e: Exception) {
            try {
                val fallback = packageManager.getApplicationIcon(entity.packageName)
                iconCache.put(cacheKey, fallback)
                fallback
            } catch (ex: Exception) {
                null
            }
        }
    }

    suspend fun setFavorite(packageName: String, isFavorite: Boolean) {
        appDao.setFavorite(packageName, isFavorite)
    }

    suspend fun setHidden(packageName: String, isHidden: Boolean) {
        appDao.setHidden(packageName, isHidden)
    }

    suspend fun updateCustomLabel(packageName: String, label: String?) {
        appDao.updateCustomLabel(packageName, label?.trim())
    }

    suspend fun updateCategory(packageName: String, category: String) {
        appDao.updateCategory(packageName, category)
    }

    /**
     * Saves a user-selected image from Photo Picker / SAF into private storage and binds it.
     */
    suspend fun saveCustomIcon(packageName: String, imageUri: Uri): String = withContext(Dispatchers.IO) {
        val iconDir = File(context.filesDir, "custom_icons")
        if (!iconDir.exists()) iconDir.mkdirs()

        val destFile = File(iconDir, "${packageName}_icon.png")
        context.contentResolver.openInputStream(imageUri)?.use { input ->
            val bitmap = BitmapFactory.decodeStream(input)
            val scaled = Bitmap.createScaledBitmap(bitmap, 192, 192, true)
            FileOutputStream(destFile).use { out ->
                scaled.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        }
        val absolutePath = destFile.absolutePath
        appDao.updateCustomIcon(packageName, absolutePath)
        iconCache.evictAll()
        absolutePath
    }

    suspend fun resetCustomIcon(packageName: String) = withContext(Dispatchers.IO) {
        val iconDir = File(context.filesDir, "custom_icons")
        val file = File(iconDir, "${packageName}_icon.png")
        if (file.exists()) file.delete()
        appDao.updateCustomIcon(packageName, null)
        iconCache.evictAll()
    }

    /**
     * Executes the launch intent for the requested application.
     */
    suspend fun launchApp(packageName: String, activityName: String) = withContext(Dispatchers.Main) {
        try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                component = ComponentName(packageName, activityName)
                addCategory(Intent.CATEGORY_LAUNCHER)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            }
            context.startActivity(intent)
            withContext(Dispatchers.IO) {
                appDao.recordLaunch(packageName)
            }
        } catch (e: Exception) {
            // Fallback to getLaunchIntentForPackage
            val fallback = context.packageManager.getLaunchIntentForPackage(packageName)
            fallback?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            fallback?.let { context.startActivity(it) }
        }
    }
}
