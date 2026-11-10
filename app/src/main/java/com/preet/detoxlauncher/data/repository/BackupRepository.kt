package com.preet.detoxlauncher.data.repository

import android.content.Context
import android.net.Uri
import com.preet.detoxlauncher.data.local.DetoxDatabase
import com.preet.detoxlauncher.data.local.entity.AppItemEntity
import com.preet.detoxlauncher.data.local.entity.CategoryEntity
import com.preet.detoxlauncher.data.local.entity.DetoxRuleEntity
import com.preet.detoxlauncher.data.local.entity.DetoxStreakEntity
import com.preet.detoxlauncher.data.local.entity.FocusSessionEntity
import com.preet.detoxlauncher.data.preferences.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

data class BackupResult(
    val success: Boolean,
    val message: String
)

class BackupRepository(
    private val context: Context,
    private val database: DetoxDatabase,
    private val userPreferences: UserPreferences
) {

    /**
     * Serializes all user configurations, rules, categories, streaks, and history into a versioned JSON string.
     */
    suspend fun createBackupJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("version", 1)
        root.put("appName", "Preet's Detox Launcher")
        root.put("timestamp", System.currentTimeMillis())

        // 1. Settings
        val settings = userPreferences.settingsFlow.first()
        val settingsObj = JSONObject().apply {
            put("isPureAmoled", settings.isPureAmoled)
            put("accentColorIndex", settings.accentColorIndex)
            put("clockSize", settings.clockSize)
            put("isClockVisible", settings.isClockVisible)
            put("isDateVisible", settings.isDateVisible)
            put("motivationalMessage", settings.motivationalMessage)
            put("dailyScreenTimeTargetMinutes", settings.dailyScreenTimeTargetMinutes)
            put("showUsageSummaryCard", settings.showUsageSummaryCard)
            put("activeIconPack", settings.activeIconPack ?: "")
            put("isLightMode", settings.isLightMode)
        }
        root.put("settings", settingsObj)

        // 2. Rules
        val rules = database.detoxRuleDao().getAllRules().first()
        val rulesArray = JSONArray()
        for (r in rules) {
            val rObj = JSONObject().apply {
                put("packageName", r.packageName)
                put("mode", r.mode)
                put("dailyLimitMinutes", r.dailyLimitMinutes)
                put("scheduleStartHour", r.scheduleStartHour)
                put("scheduleStartMinute", r.scheduleStartMinute)
                put("scheduleEndHour", r.scheduleEndHour)
                put("scheduleEndMinute", r.scheduleEndMinute)
                put("activeDaysString", r.activeDaysString)
                put("requiresIntentionalPrompt", r.requiresIntentionalPrompt)
                put("delaySeconds", r.delaySeconds)
            }
            rulesArray.put(rObj)
        }
        root.put("detoxRules", rulesArray)

        // 3. Categories
        val categories = database.categoryDao().getAllCategories().first()
        val catArray = JSONArray()
        for (c in categories) {
            val cObj = JSONObject().apply {
                put("name", c.name)
                put("displayOrder", c.displayOrder)
            }
            catArray.put(cObj)
        }
        root.put("categories", catArray)

        // 4. Streaks
        val streaks = database.detoxStreakDao().getAllHistory().first()
        val streakArray = JSONArray()
        for (s in streaks) {
            val sObj = JSONObject().apply {
                put("dateString", s.dateString)
                put("goalMinutes", s.goalMinutes)
                put("actualMinutes", s.actualMinutes)
                put("goalMet", s.goalMet)
                put("unlockCount", s.unlockCount)
            }
            streakArray.put(sObj)
        }
        root.put("streaks", streakArray)

        root.toString(2)
    }

    /**
     * Exports backup JSON directly to the SAF URI selected by the user.
     */
    suspend fun exportBackupToUri(uri: Uri): BackupResult = withContext(Dispatchers.IO) {
        try {
            val json = createBackupJson()
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(json)
                }
            }
            BackupResult(true, "Backup successfully exported.")
        } catch (e: Exception) {
            BackupResult(false, "Export failed: ${e.localizedMessage}")
        }
    }

    /**
     * Validates and restores user configurations from the selected SAF URI.
     */
    suspend fun importBackupFromUri(uri: Uri): BackupResult = withContext(Dispatchers.IO) {
        try {
            val stringBuilder = StringBuilder()
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line: String? = reader.readLine()
                    while (line != null) {
                        stringBuilder.append(line)
                        line = reader.readLine()
                    }
                }
            }

            val jsonStr = stringBuilder.toString()
            val root = JSONObject(jsonStr)

            // Validate schema version
            val version = root.optInt("version", -1)
            if (version < 1) {
                return@withContext BackupResult(false, "Incompatible or corrupt backup file (unsupported version).")
            }

            // Restore Settings
            if (root.has("settings")) {
                val s = root.getJSONObject("settings")
                userPreferences.setPureAmoled(s.optBoolean("isPureAmoled", true))
                userPreferences.setAccentColorIndex(s.optInt("accentColorIndex", 0))
                userPreferences.setClockSize(s.optString("clockSize", "LARGE"))
                userPreferences.setClockVisible(s.optBoolean("isClockVisible", true))
                userPreferences.setDateVisible(s.optBoolean("isDateVisible", true))
                userPreferences.setMotivationalMessage(s.optString("motivationalMessage", "Less scrolling. More living."))
                userPreferences.setDailyScreenTimeTarget(s.optInt("dailyScreenTimeTargetMinutes", 120))
                userPreferences.setShowUsageSummary(s.optBoolean("showUsageSummaryCard", true))
                val pack = s.optString("activeIconPack")
                userPreferences.setActiveIconPack(if (pack.isBlank()) null else pack)
            }

            // Restore Rules
            if (root.has("detoxRules")) {
                val rulesArray = root.getJSONArray("detoxRules")
                for (i in 0 until rulesArray.length()) {
                    val r = rulesArray.getJSONObject(i)
                    database.detoxRuleDao().upsertRule(
                        DetoxRuleEntity(
                            packageName = r.getString("packageName"),
                            mode = r.getString("mode"),
                            dailyLimitMinutes = r.optInt("dailyLimitMinutes", 0),
                            scheduleStartHour = r.optInt("scheduleStartHour", 22),
                            scheduleStartMinute = r.optInt("scheduleStartMinute", 0),
                            scheduleEndHour = r.optInt("scheduleEndHour", 7),
                            scheduleEndMinute = r.optInt("scheduleEndMinute", 0),
                            activeDaysString = r.optString("activeDaysString", "1,2,3,4,5,6,7"),
                            requiresIntentionalPrompt = r.optBoolean("requiresIntentionalPrompt", false),
                            delaySeconds = r.optInt("delaySeconds", 0)
                        )
                    )
                }
            }

            // Restore Categories
            if (root.has("categories")) {
                val catArray = root.getJSONArray("categories")
                for (i in 0 until catArray.length()) {
                    val c = catArray.getJSONObject(i)
                    database.categoryDao().insertCategory(
                        CategoryEntity(
                            name = c.getString("name"),
                            displayOrder = c.optInt("displayOrder", i)
                        )
                    )
                }
            }

            // Restore Streaks
            if (root.has("streaks")) {
                val streakArray = root.getJSONArray("streaks")
                for (i in 0 until streakArray.length()) {
                    val s = streakArray.getJSONObject(i)
                    database.detoxStreakDao().recordDayResult(
                        DetoxStreakEntity(
                            dateString = s.getString("dateString"),
                            goalMinutes = s.getInt("goalMinutes"),
                            actualMinutes = s.getInt("actualMinutes"),
                            goalMet = s.getBoolean("goalMet"),
                            unlockCount = s.optInt("unlockCount", 0)
                        )
                    )
                }
            }

            BackupResult(true, "Configurations and wellbeing data successfully restored!")
        } catch (e: Exception) {
            BackupResult(false, "Restoration failed: ${e.localizedMessage}")
        }
    }
}
