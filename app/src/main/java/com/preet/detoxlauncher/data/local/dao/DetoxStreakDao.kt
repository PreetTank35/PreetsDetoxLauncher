package com.preet.detoxlauncher.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.preet.detoxlauncher.data.local.entity.DetoxStreakEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DetoxStreakDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun recordDayResult(result: DetoxStreakEntity)

    @Query("SELECT * FROM detox_streaks ORDER BY dateString DESC")
    fun getAllHistory(): Flow<List<DetoxStreakEntity>>

    @Query("SELECT * FROM detox_streaks WHERE dateString = :dateString LIMIT 1")
    suspend fun getDayResult(dateString: String): DetoxStreakEntity?

    @Query("SELECT * FROM detox_streaks ORDER BY dateString DESC LIMIT :limit")
    suspend fun getRecentRecordsSync(limit: Int = 30): List<DetoxStreakEntity>
}
