package com.preet.detoxlauncher.data.repository

import com.preet.detoxlauncher.data.local.dao.DetoxStreakDao
import com.preet.detoxlauncher.data.local.entity.DetoxStreakEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class StreakInfo(
    val currentStreakDays: Int,
    val longestStreakDays: Int,
    val totalGoalsMet: Int
)

class StreakRepository(
    private val detoxStreakDao: DetoxStreakDao,
    private val usageRepository: UsageRepository
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun getAllHistory(): Flow<List<DetoxStreakEntity>> {
        return detoxStreakDao.getAllHistory()
    }

    /**
     * Evaluates today's or yesterday's completion and writes to the history database.
     */
    suspend fun recordDailyGoalEvaluation(targetMinutes: Int): Unit = withContext(Dispatchers.IO) {
        val todayStr = dateFormat.format(Calendar.getInstance().time)
        val todayUsageMillis = usageRepository.getTodayTotalScreenTimeMillis()
        val actualMinutes = (todayUsageMillis / (1000 * 60)).toInt()
        val unlockCount = usageRepository.getTodayUnlockCount()

        val goalMet = actualMinutes <= targetMinutes && targetMinutes > 0

        detoxStreakDao.recordDayResult(
            DetoxStreakEntity(
                dateString = todayStr,
                goalMinutes = targetMinutes,
                actualMinutes = actualMinutes,
                goalMet = goalMet,
                unlockCount = unlockCount
            )
        )
    }

    /**
     * Calculates current consecutive streak and longest streak accurately.
     */
    suspend fun calculateStreaks(): StreakInfo = withContext(Dispatchers.IO) {
        val records = detoxStreakDao.getRecentRecordsSync(90) // Last 90 days
        if (records.isEmpty()) return@withContext StreakInfo(0, 0, 0)

        val recordMap = records.associateBy { it.dateString }
        val calendar = Calendar.getInstance()

        // Check if yesterday or today was met
        val todayStr = dateFormat.format(calendar.time)
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayStr = dateFormat.format(calendar.time)

        var currentStreak = 0
        var checkCalendar = Calendar.getInstance()

        // If today is completed or pending, we trace backwards from yesterday/today
        val todayRecord = recordMap[todayStr]
        val startFromYesterday = todayRecord?.goalMet != true

        if (startFromYesterday) {
            checkCalendar.add(Calendar.DAY_OF_YEAR, -1)
        }

        while (true) {
            val dateStr = dateFormat.format(checkCalendar.time)
            val dayRecord = recordMap[dateStr]
            if (dayRecord != null && dayRecord.goalMet) {
                currentStreak++
                checkCalendar.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }

        // Longest Streak calculation
        var longestStreak = 0
        var tempStreak = 0
        val sortedByDate = records.sortedBy { it.dateString }

        for (record in sortedByDate) {
            if (record.goalMet) {
                tempStreak++
                if (tempStreak > longestStreak) longestStreak = tempStreak
            } else {
                tempStreak = 0
            }
        }

        val totalGoalsMet = records.count { it.goalMet }
        StreakInfo(
            currentStreakDays = currentStreak,
            longestStreakDays = maxOf(currentStreak, longestStreak),
            totalGoalsMet = totalGoalsMet
        )
    }
}
