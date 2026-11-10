package com.preet.detoxlauncher.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.preet.detoxlauncher.data.local.entity.AppItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM apps ORDER BY CASE WHEN customLabel IS NOT NULL AND customLabel != '' THEN customLabel ELSE systemLabel END ASC")
    fun getAllApps(): Flow<List<AppItemEntity>>

    @Query("SELECT * FROM apps WHERE isFavorite = 1 ORDER BY CASE WHEN customLabel IS NOT NULL AND customLabel != '' THEN customLabel ELSE systemLabel END ASC")
    fun getFavoriteApps(): Flow<List<AppItemEntity>>

    @Query("SELECT * FROM apps WHERE isHidden = 0 ORDER BY CASE WHEN customLabel IS NOT NULL AND customLabel != '' THEN customLabel ELSE systemLabel END ASC")
    fun getVisibleApps(): Flow<List<AppItemEntity>>

    @Query("SELECT * FROM apps WHERE isHidden = 1")
    fun getHiddenApps(): Flow<List<AppItemEntity>>

    @Query("SELECT * FROM apps WHERE packageName = :packageName LIMIT 1")
    suspend fun getAppByPackage(packageName: String): AppItemEntity?

    @Query("SELECT * FROM apps WHERE packageName = :packageName LIMIT 1")
    fun observeAppByPackage(packageName: String): Flow<AppItemEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertApp(app: AppItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertApps(apps: List<AppItemEntity>)

    @Query("UPDATE apps SET isFavorite = :isFavorite WHERE packageName = :packageName")
    suspend fun setFavorite(packageName: String, isFavorite: Boolean)

    @Query("UPDATE apps SET isHidden = :isHidden WHERE packageName = :packageName")
    suspend fun setHidden(packageName: String, isHidden: Boolean)

    @Query("UPDATE apps SET customLabel = :label WHERE packageName = :packageName")
    suspend fun updateCustomLabel(packageName: String, label: String?)

    @Query("UPDATE apps SET customIconPath = :iconPath WHERE packageName = :packageName")
    suspend fun updateCustomIcon(packageName: String, iconPath: String?)

    @Query("UPDATE apps SET iconPackPackage = :iconPackPackage WHERE packageName = :packageName")
    suspend fun updateIconPackPackage(packageName: String, iconPackPackage: String?)

    @Query("UPDATE apps SET category = :category WHERE packageName = :packageName")
    suspend fun updateCategory(packageName: String, category: String)

    @Query("UPDATE apps SET lastLaunchTime = :launchTime, launchCount = launchCount + 1 WHERE packageName = :packageName")
    suspend fun recordLaunch(packageName: String, launchTime: Long = System.currentTimeMillis())

    @Query("DELETE FROM apps WHERE packageName = :packageName")
    suspend fun deleteApp(packageName: String)
}
