package com.preet.detoxlauncher.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.preet.detoxlauncher.data.local.entity.FocusSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusSessionDao {
    @Insert
    suspend fun insertSession(session: FocusSessionEntity): Long

    @Query("SELECT * FROM focus_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM focus_sessions WHERE startTime >= :startOfDay AND startTime <= :endOfDay AND completed = 1")
    fun getCompletedSessionsForDay(startOfDay: Long, endOfDay: Long): Flow<List<FocusSessionEntity>>

    @Query("SELECT COUNT(*) FROM focus_sessions WHERE startTime >= :startOfDay AND completed = 1")
    suspend fun getTodayCompletedFocusCount(startOfDay: Long): Int

    @Query("SELECT SUM(durationMinutes) FROM focus_sessions WHERE startTime >= :startOfDay AND completed = 1 AND sessionType = 'FOCUS'")
    suspend fun getTodayFocusMinutes(startOfDay: Long): Int?
}
