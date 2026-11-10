package com.preet.detoxlauncher.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.preet.detoxlauncher.data.local.entity.DailyChallengeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyChallengeDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertChallenges(challenges: List<DailyChallengeEntity>)

    @Query("SELECT * FROM daily_challenges WHERE dateString = :dateString")
    fun getChallengesForDate(dateString: String): Flow<List<DailyChallengeEntity>>

    @Query("SELECT * FROM daily_challenges WHERE dateString = :dateString")
    suspend fun getChallengesForDateSync(dateString: String): List<DailyChallengeEntity>

    @Query("UPDATE daily_challenges SET currentValue = :value, completed = :completed WHERE id = :id")
    suspend fun updateProgress(id: String, value: Int, completed: Boolean)

    @Query("SELECT COUNT(*) FROM daily_challenges WHERE completed = 1")
    fun getTotalCompletedCount(): Flow<Int>
}
