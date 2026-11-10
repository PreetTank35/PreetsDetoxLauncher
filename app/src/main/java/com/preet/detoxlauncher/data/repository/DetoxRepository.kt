package com.preet.detoxlauncher.data.repository

import com.preet.detoxlauncher.data.local.dao.DetoxRuleDao
import com.preet.detoxlauncher.data.local.dao.IntentionalLogDao
import com.preet.detoxlauncher.data.local.entity.DetoxRuleEntity
import com.preet.detoxlauncher.data.local.entity.IntentionalLogEntity
import com.preet.detoxlauncher.data.model.DetoxRule
import com.preet.detoxlauncher.data.model.IntentionalReason
import com.preet.detoxlauncher.data.model.RestrictionMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale

sealed class LaunchDecision {
    data object Allow : LaunchDecision()
    data class RequireIntentionalPrompt(val delaySeconds: Int) : LaunchDecision()
    data class RequireDelay(val delaySeconds: Int) : LaunchDecision()
    data class SoftBlock(val currentUsageMinutes: Int, val limitMinutes: Int) : LaunchDecision()
    data class StrictBlock(val currentUsageMinutes: Int, val limitMinutes: Int, val resetTimeFormatted: String) : LaunchDecision()
    data class ScheduledBlock(val windowString: String) : LaunchDecision()
}

class DetoxRepository(
    private val detoxRuleDao: DetoxRuleDao,
    private val intentionalLogDao: IntentionalLogDao,
    private val usageRepository: UsageRepository
) {

    fun getAllRules(): Flow<List<DetoxRule>> {
        return detoxRuleDao.getAllRules().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    fun getRuleForPackage(packageName: String): Flow<DetoxRule?> {
        return detoxRuleDao.getRuleForPackage(packageName).map { it?.toDomainModel() }
    }

    suspend fun saveRule(rule: DetoxRule) = withContext(Dispatchers.IO) {
        val entity = DetoxRuleEntity(
            packageName = rule.packageName,
            mode = rule.mode.name,
            dailyLimitMinutes = rule.dailyLimitMinutes,
            scheduleStartHour = rule.scheduleStartHour,
            scheduleStartMinute = rule.scheduleStartMinute,
            scheduleEndHour = rule.scheduleEndHour,
            scheduleEndMinute = rule.scheduleEndMinute,
            activeDaysString = rule.activeDays.joinToString(","),
            requiresIntentionalPrompt = rule.requiresIntentionalPrompt,
            delaySeconds = rule.delaySeconds
        )
        detoxRuleDao.upsertRule(entity)
    }

    suspend fun deleteRule(packageName: String) = withContext(Dispatchers.IO) {
        detoxRuleDao.deleteRule(packageName)
    }

    suspend fun logIntentionalOpen(packageName: String, reason: IntentionalReason) = withContext(Dispatchers.IO) {
        intentionalLogDao.insertLog(
            IntentionalLogEntity(
                packageName = packageName,
                reason = reason.name,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    fun getRecentIntentionalLogs(): Flow<List<IntentionalLogEntity>> {
        return intentionalLogDao.getRecentLogs()
    }

    /**
     * Determines whether an application launch should be allowed, delayed, prompted, or blocked.
     */
    suspend fun evaluateAppLaunch(packageName: String): LaunchDecision = withContext(Dispatchers.IO) {
        val ruleEntity = detoxRuleDao.getRuleForPackageSync(packageName) ?: return@withContext LaunchDecision.Allow
        val rule = ruleEntity.toDomainModel()

        // 1. Check Scheduled Restriction
        if (rule.mode == RestrictionMode.SCHEDULED) {
            val now = Calendar.getInstance()
            val currentDay = now.get(Calendar.DAY_OF_WEEK)
            if (rule.activeDays.contains(currentDay)) {
                val currentMinuteOfDay = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
                val startMinuteOfDay = rule.scheduleStartHour * 60 + rule.scheduleStartMinute
                val endMinuteOfDay = rule.scheduleEndHour * 60 + rule.scheduleEndMinute

                val inWindow = if (startMinuteOfDay <= endMinuteOfDay) {
                    currentMinuteOfDay in startMinuteOfDay..endMinuteOfDay
                } else {
                    // Window crosses midnight (e.g. 22:00 to 07:00)
                    currentMinuteOfDay >= startMinuteOfDay || currentMinuteOfDay <= endMinuteOfDay
                }

                if (inWindow) {
                    val windowFormatted = String.format(
                        Locale.getDefault(),
                        "%02d:%02d - %02d:%02d",
                        rule.scheduleStartHour,
                        rule.scheduleStartMinute,
                        rule.scheduleEndHour,
                        rule.scheduleEndMinute
                    )
                    return@withContext LaunchDecision.ScheduledBlock(windowFormatted)
                }
            }
        }

        // 2. Check Daily Usage Limits (Strict or Soft)
        if (rule.dailyLimitMinutes > 0 && (rule.mode == RestrictionMode.STRICT || rule.mode == RestrictionMode.SOFT)) {
            val usedMillis = usageRepository.getAppScreenTimeToday(packageName)
            val usedMinutes = (usedMillis / (1000 * 60)).toInt()

            if (usedMinutes >= rule.dailyLimitMinutes) {
                if (rule.mode == RestrictionMode.STRICT) {
                    return@withContext LaunchDecision.StrictBlock(
                        currentUsageMinutes = usedMinutes,
                        limitMinutes = rule.dailyLimitMinutes,
                        resetTimeFormatted = "Midnight (12:00 AM)"
                    )
                } else {
                    return@withContext LaunchDecision.SoftBlock(
                        currentUsageMinutes = usedMinutes,
                        limitMinutes = rule.dailyLimitMinutes
                    )
                }
            }
        }

        // 3. Check Intentional Prompt Requirement
        if (rule.requiresIntentionalPrompt) {
            return@withContext LaunchDecision.RequireIntentionalPrompt(delaySeconds = rule.delaySeconds)
        }

        // 4. Check Standalone Delay Requirement
        if (rule.delaySeconds > 0) {
            return@withContext LaunchDecision.RequireDelay(delaySeconds = rule.delaySeconds)
        }

        LaunchDecision.Allow
    }

    private fun DetoxRuleEntity.toDomainModel(): DetoxRule {
        val days = try {
            activeDaysString.split(",")
                .mapNotNull { it.trim().toIntOrNull() }
                .toSet()
        } catch (e: Exception) {
            setOf(1, 2, 3, 4, 5, 6, 7)
        }

        return DetoxRule(
            packageName = packageName,
            mode = try { RestrictionMode.valueOf(mode) } catch (e: Exception) { RestrictionMode.OFF },
            dailyLimitMinutes = dailyLimitMinutes,
            scheduleStartHour = scheduleStartHour,
            scheduleStartMinute = scheduleStartMinute,
            scheduleEndHour = scheduleEndHour,
            scheduleEndMinute = scheduleEndMinute,
            activeDays = days,
            requiresIntentionalPrompt = requiresIntentionalPrompt,
            delaySeconds = delaySeconds
        )
    }
}
