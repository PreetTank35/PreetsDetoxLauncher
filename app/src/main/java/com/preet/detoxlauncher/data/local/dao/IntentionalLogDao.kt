package com.preet.detoxlauncher.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.preet.detoxlauncher.data.local.entity.IntentionalLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IntentionalLogDao {
    @Insert
    suspend fun insertLog(log: IntentionalLogEntity)

    @Query("SELECT * FROM intentional_logs WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getLogsBetween(startTime: Long, endTime: Long): Flow<List<IntentionalLogEntity>>

    @Query("SELECT * FROM intentional_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int = 100): Flow<List<IntentionalLogEntity>>

    @Query("SELECT COUNT(*) FROM intentional_logs WHERE timestamp >= :startTime")
    suspend fun getLogCountSince(startTime: Long): Int
}
