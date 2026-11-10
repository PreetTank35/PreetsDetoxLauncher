package com.preet.detoxlauncher.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "detox_user_preferences")

data class LauncherSettings(
    val isPureAmoled: Boolean = true,
    val accentColorIndex: Int = 0, // 0: Emerald, 1: Cyan, 2: Gold, 3: Coral, 4: Indigo, 5: White
    val clockSize: String = "LARGE", // LARGE, MEDIUM, COMPACT
    val isClockVisible: Boolean = true,
    val isDateVisible: Boolean = true,
    val motivationalMessage: String = "Less scrolling. More living.",
    val dailyScreenTimeTargetMinutes: Int = 120,
    val showUsageSummaryCard: Boolean = true,
    val activeIconPack: String? = null,
    val emergencyOverridesRemainingToday: Int = 3,
    val isLightMode: Boolean = false
)

class UserPreferences(private val context: Context) {

    private object Keys {
        val PURE_AMOLED = booleanPreferencesKey("is_pure_amoled")
        val ACCENT_COLOR_INDEX = intPreferencesKey("accent_color_index")
        val CLOCK_SIZE = stringPreferencesKey("clock_size")
        val CLOCK_VISIBLE = booleanPreferencesKey("is_clock_visible")
        val DATE_VISIBLE = booleanPreferencesKey("is_date_visible")
        val MOTIVATIONAL_MESSAGE = stringPreferencesKey("motivational_message")
        val DAILY_SCREEN_TIME_TARGET = intPreferencesKey("daily_screen_time_target")
        val SHOW_USAGE_SUMMARY = booleanPreferencesKey("show_usage_summary")
        val ACTIVE_ICON_PACK = stringPreferencesKey("active_icon_pack")
        val EMERGENCY_OVERRIDES_USED = intPreferencesKey("emergency_overrides_used")
        val LAST_OVERRIDE_RESET_DAY = longPreferencesKey("last_override_reset_day")
        val LIGHT_MODE = booleanPreferencesKey("is_light_mode")
    }

    val settingsFlow: Flow<LauncherSettings> = context.dataStore.data.map { prefs ->
        val todayStart = getMidnightTimestamp()
        val lastReset = prefs[Keys.LAST_OVERRIDE_RESET_DAY] ?: 0L
        val overridesUsed = if (lastReset < todayStart) 0 else (prefs[Keys.EMERGENCY_OVERRIDES_USED] ?: 0)
        val remainingOverrides = (3 - overridesUsed).coerceAtLeast(0)

        LauncherSettings(
            isPureAmoled = prefs[Keys.PURE_AMOLED] ?: true,
            accentColorIndex = prefs[Keys.ACCENT_COLOR_INDEX] ?: 0,
            clockSize = prefs[Keys.CLOCK_SIZE] ?: "LARGE",
            isClockVisible = prefs[Keys.CLOCK_VISIBLE] ?: true,
            isDateVisible = prefs[Keys.DATE_VISIBLE] ?: true,
            motivationalMessage = prefs[Keys.MOTIVATIONAL_MESSAGE] ?: "Less scrolling. More living.",
            dailyScreenTimeTargetMinutes = prefs[Keys.DAILY_SCREEN_TIME_TARGET] ?: 120,
            showUsageSummaryCard = prefs[Keys.SHOW_USAGE_SUMMARY] ?: true,
            activeIconPack = prefs[Keys.ACTIVE_ICON_PACK]?.takeIf { it.isNotBlank() },
            emergencyOverridesRemainingToday = remainingOverrides,
            isLightMode = prefs[Keys.LIGHT_MODE] ?: false
        )
    }

    suspend fun setPureAmoled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.PURE_AMOLED] = enabled }
    }

    suspend fun setAccentColorIndex(index: Int) {
        context.dataStore.edit { it[Keys.ACCENT_COLOR_INDEX] = index }
    }

    suspend fun setClockSize(size: String) {
        context.dataStore.edit { it[Keys.CLOCK_SIZE] = size }
    }

    suspend fun setClockVisible(visible: Boolean) {
        context.dataStore.edit { it[Keys.CLOCK_VISIBLE] = visible }
    }

    suspend fun setDateVisible(visible: Boolean) {
        context.dataStore.edit { it[Keys.DATE_VISIBLE] = visible }
    }

    suspend fun setMotivationalMessage(message: String) {
        context.dataStore.edit { it[Keys.MOTIVATIONAL_MESSAGE] = message }
    }

    suspend fun setDailyScreenTimeTarget(minutes: Int) {
        context.dataStore.edit { it[Keys.DAILY_SCREEN_TIME_TARGET] = minutes }
    }

    suspend fun setShowUsageSummary(visible: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_USAGE_SUMMARY] = visible }
    }

    suspend fun setActiveIconPack(packPackage: String?) {
        context.dataStore.edit {
            if (packPackage.isNullOrBlank()) {
                it.remove(Keys.ACTIVE_ICON_PACK)
            } else {
                it[Keys.ACTIVE_ICON_PACK] = packPackage
            }
        }
    }

    suspend fun useEmergencyOverride(): Boolean {
        var success = false
        val todayStart = getMidnightTimestamp()
        context.dataStore.edit { prefs ->
            val lastReset = prefs[Keys.LAST_OVERRIDE_RESET_DAY] ?: 0L
            val currentUsed = if (lastReset < todayStart) 0 else (prefs[Keys.EMERGENCY_OVERRIDES_USED] ?: 0)
            if (currentUsed < 3) {
                prefs[Keys.EMERGENCY_OVERRIDES_USED] = currentUsed + 1
                prefs[Keys.LAST_OVERRIDE_RESET_DAY] = todayStart
                success = true
            }
        }
        return success
    }

    suspend fun setLightMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.LIGHT_MODE] = enabled }
    }

    private fun getMidnightTimestamp(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
