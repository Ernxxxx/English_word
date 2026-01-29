package com.example.englishword.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.englishword.data.local.dao.LevelDao
import com.example.englishword.data.local.dao.StudyRecordDao
import com.example.englishword.data.local.dao.StudySessionDao
import com.example.englishword.data.local.dao.UnitUnlockDao
import com.example.englishword.data.local.dao.UserSettingsDao
import com.example.englishword.data.local.dao.UserStatsDao
import com.example.englishword.data.local.dao.WordDao
import com.example.englishword.data.local.entity.Level
import com.example.englishword.data.local.entity.StudyRecord
import com.example.englishword.data.local.entity.StudySession
import com.example.englishword.data.local.entity.UnitUnlock
import com.example.englishword.data.local.entity.UserSettings
import com.example.englishword.data.local.entity.UserStats
import com.example.englishword.data.local.entity.Word
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Level::class,
        Word::class,
        StudySession::class,
        StudyRecord::class,
        UserStats::class,
        UserSettings::class,
        UnitUnlock::class
    ],
    version = 3,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun levelDao(): LevelDao
    abstract fun wordDao(): WordDao
    abstract fun studySessionDao(): StudySessionDao
    abstract fun studyRecordDao(): StudyRecordDao
    abstract fun userStatsDao(): UserStatsDao
    abstract fun userSettingsDao(): UserSettingsDao
    abstract fun unitUnlockDao(): UnitUnlockDao

    companion object {
        private const val DATABASE_NAME = "english_word_database"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                .addCallback(DatabaseCallback())
                .fallbackToDestructiveMigration()
                .build()
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Initialize default data if needed
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialData(database)
                    }
                }
            }
        }

        private suspend fun populateInitialData(database: AppDatabase) {
            // Levels and words are now seeded by InitialDataSeeder
            // Only add default settings here
            val defaultSettings = listOf(
                UserSettings(key = "daily_goal", value = "20"),
                UserSettings(key = "notification_enabled", value = "true"),
                UserSettings(key = "notification_time", value = "09:00"),
                UserSettings(key = "dark_mode", value = "system"),
                UserSettings(key = "sound_enabled", value = "true")
            )
            database.userSettingsDao().insertAll(defaultSettings)
        }
    }
}
