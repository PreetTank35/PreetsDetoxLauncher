package com.preet.detoxlauncher

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.preet.detoxlauncher.data.local.DetoxDatabase
import com.preet.detoxlauncher.data.preferences.UserPreferences
import com.preet.detoxlauncher.data.repository.AppRepository
import com.preet.detoxlauncher.data.repository.BackupRepository
import com.preet.detoxlauncher.data.repository.ChallengeRepository
import com.preet.detoxlauncher.data.repository.DetoxRepository
import com.preet.detoxlauncher.data.repository.FocusRepository
import com.preet.detoxlauncher.data.repository.IconPackRepository
import com.preet.detoxlauncher.data.repository.StreakRepository
import com.preet.detoxlauncher.data.repository.UsageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DetoxApplication : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var database: DetoxDatabase
        private set

    lateinit var userPreferences: UserPreferences
        private set

    lateinit var iconPackRepository: IconPackRepository
        private set

    lateinit var appRepository: AppRepository
        private set

    lateinit var usageRepository: UsageRepository
        private set

    lateinit var detoxRepository: DetoxRepository
        private set

    lateinit var focusRepository: FocusRepository
        private set

    lateinit var streakRepository: StreakRepository
        private set

    lateinit var challengeRepository: ChallengeRepository
        private set

    lateinit var backupRepository: BackupRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = DetoxDatabase.getInstance(this)
        userPreferences = UserPreferences(this)
        iconPackRepository = IconPackRepository(this)
        appRepository = AppRepository(this, database.appDao(), iconPackRepository)
        usageRepository = UsageRepository(this)
        detoxRepository = DetoxRepository(
            database.detoxRuleDao(),
            database.intentionalLogDao(),
            usageRepository
        )
        focusRepository = FocusRepository(this, database.focusSessionDao(), applicationScope)
        streakRepository = StreakRepository(database.detoxStreakDao(), usageRepository)
        challengeRepository = ChallengeRepository(
            database.dailyChallengeDao(),
            usageRepository,
            focusRepository
        )
        backupRepository = BackupRepository(this, database, userPreferences)

        createNotificationChannels()

        applicationScope.launch(Dispatchers.IO) {
            appRepository.refreshInstalledApps()
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_DETOX,
                "Detox Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Digital wellbeing reminders, focus timers and daily limit alerts"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    companion object {
        const val NOTIFICATION_CHANNEL_DETOX = "detox_channel_mindful"
        lateinit var instance: DetoxApplication
            private set
    }
}
