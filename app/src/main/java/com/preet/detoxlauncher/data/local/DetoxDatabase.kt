package com.preet.detoxlauncher.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.preet.detoxlauncher.data.local.dao.AppDao
import com.preet.detoxlauncher.data.local.dao.CategoryDao
import com.preet.detoxlauncher.data.local.dao.DailyChallengeDao
import com.preet.detoxlauncher.data.local.dao.DetoxRuleDao
import com.preet.detoxlauncher.data.local.dao.DetoxStreakDao
import com.preet.detoxlauncher.data.local.dao.FocusSessionDao
import com.preet.detoxlauncher.data.local.dao.IntentionalLogDao
import com.preet.detoxlauncher.data.local.entity.AppItemEntity
import com.preet.detoxlauncher.data.local.entity.CategoryEntity
import com.preet.detoxlauncher.data.local.entity.DailyChallengeEntity
import com.preet.detoxlauncher.data.local.entity.DetoxRuleEntity
import com.preet.detoxlauncher.data.local.entity.DetoxStreakEntity
import com.preet.detoxlauncher.data.local.entity.FocusSessionEntity
import com.preet.detoxlauncher.data.local.entity.IntentionalLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        AppItemEntity::class,
        DetoxRuleEntity::class,
        IntentionalLogEntity::class,
        CategoryEntity::class,
        FocusSessionEntity::class,
        DetoxStreakEntity::class,
        DailyChallengeEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class DetoxDatabase : RoomDatabase() {

    abstract fun appDao(): AppDao
    abstract fun detoxRuleDao(): DetoxRuleDao
    abstract fun intentionalLogDao(): IntentionalLogDao
    abstract fun categoryDao(): CategoryDao
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun detoxStreakDao(): DetoxStreakDao
    abstract fun dailyChallengeDao(): DailyChallengeDao

    companion object {
        @Volatile
        private var INSTANCE: DetoxDatabase? = null

        fun getInstance(context: Context): DetoxDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DetoxDatabase::class.java,
                    "detox_database.db"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                val categoryDao = getInstance(context).categoryDao()
                                val defaultCategories = listOf(
                                    CategoryEntity("General", 0),
                                    CategoryEntity("Productivity", 1),
                                    CategoryEntity("Mindfulness", 2),
                                    CategoryEntity("Communication", 3),
                                    CategoryEntity("Utilities", 4)
                                )
                                categoryDao.insertCategories(defaultCategories)
                            }
                        }
                    })
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
