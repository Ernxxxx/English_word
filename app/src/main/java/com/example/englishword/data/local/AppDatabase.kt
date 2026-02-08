package com.example.englishword.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
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
    version = 6,
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
}
