package com.preet.detoxlauncher.data.repository

import com.preet.detoxlauncher.data.local.dao.DailyChallengeDao
import com.preet.detoxlauncher.data.local.entity.DailyChallengeEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ChallengeRepository(
    private val dailyChallengeDao: DailyChallengeDao,
    private val usageRepository: UsageRepository,
    private val focusRepository: FocusRepository
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun getTodayChallenges(): Flow<List<DailyChallengeEntity>> {
        val todayStr = dateFormat.format(Calendar.getInstance().time)
        return dailyChallengeDao.getChallengesForDate(todayStr)
    }

    suspend fun generateOrRefreshTodayChallenges(targetMinutes: Int) = withContext(Dispatchers.IO) {
        val todayStr = dateFormat.format(Calendar.getInstance().time)
        val existing = dailyChallengeDao.getChallengesForDateSync(todayStr)

        if (existing.isEmpty()) {
            val initialChallenges = listOf(
                DailyChallengeEntity(
                    id = "TARGET_${todayStr}",
                    dateString = todayStr,
                    title = "Stay Under Goal",
                    description = "Keep total screen time under your daily goal of ${targetMinutes}m.",
                    targetValue = targetMinutes,
                    currentValue = 0,
                    completed = false
                ),
                DailyChallengeEntity(
                    id = "FOCUS_${todayStr}",
                    dateString = todayStr,
                    title = "Deep Focus Flow",
                    description = "Complete 2 mindful Pomodoro focus sessions today.",
                    targetValue = 2,
                    currentValue = 0,
                    completed = false
                ),
                DailyChallengeEntity(
                    id = "MINDFUL_${todayStr}",
                    dateString = todayStr,
                    title = "Intentional Pause",
                    description = "Take at least 3 intentional reflections before opening apps.",
                    targetValue = 3,
                    currentValue = 0,
                    completed = false
                )
            )
            dailyChallengeDao.insertChallenges(initialChallenges)
        } else {
            // Update progress on existing challenges
            val todayUsageMinutes = (usageRepository.getTodayTotalScreenTimeMillis() / (1000 * 60)).toInt()
            val focusCount = focusRepository.getTodayFocusMinutes() / 25

            for (ch in existing) {
                when {
                    ch.id.startsWith("TARGET") -> {
                        val met = todayUsageMinutes <= ch.targetValue
                        dailyChallengeDao.updateProgress(ch.id, todayUsageMinutes, met)
                    }
                    ch.id.startsWith("FOCUS") -> {
                        val met = focusCount >= ch.targetValue
                        dailyChallengeDao.updateProgress(ch.id, focusCount, met)
                    }
                }
            }
        }
    }
}
