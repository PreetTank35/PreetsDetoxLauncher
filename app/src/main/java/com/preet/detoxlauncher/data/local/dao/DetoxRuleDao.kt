package com.preet.detoxlauncher.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.preet.detoxlauncher.data.local.entity.DetoxRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DetoxRuleDao {
    @Query("SELECT * FROM detox_rules")
    fun getAllRules(): Flow<List<DetoxRuleEntity>>

    @Query("SELECT * FROM detox_rules WHERE packageName = :packageName LIMIT 1")
    fun getRuleForPackage(packageName: String): Flow<DetoxRuleEntity?>

    @Query("SELECT * FROM detox_rules WHERE packageName = :packageName LIMIT 1")
    suspend fun getRuleForPackageSync(packageName: String): DetoxRuleEntity?

    @Query("SELECT * FROM detox_rules WHERE mode != 'OFF'")
    suspend fun getActiveRulesSync(): List<DetoxRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRule(rule: DetoxRuleEntity)

    @Query("DELETE FROM detox_rules WHERE packageName = :packageName")
    suspend fun deleteRule(packageName: String)
}
