package com.example.englishword.di

import android.content.Context
import androidx.room.Room
import com.example.englishword.data.local.AppDatabase
import com.example.englishword.data.local.InitialDataSeeder
import com.example.englishword.data.local.dao.LevelDao
import com.example.englishword.data.local.dao.StudyRecordDao
import com.example.englishword.data.local.dao.StudySessionDao
import com.example.englishword.data.local.dao.UserSettingsDao
import com.example.englishword.data.local.dao.UserStatsDao
import com.example.englishword.data.local.dao.WordDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private const val DATABASE_NAME = "english_word_database"

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideInitialDataSeeder(
        levelDao: LevelDao,
        wordDao: WordDao
    ): InitialDataSeeder {
        return InitialDataSeeder(levelDao, wordDao)
    }

    @Provides
    @Singleton
    fun provideLevelDao(database: AppDatabase): LevelDao {
        return database.levelDao()
    }

    @Provides
    @Singleton
    fun provideWordDao(database: AppDatabase): WordDao {
        return database.wordDao()
    }

    @Provides
    @Singleton
    fun provideStudySessionDao(database: AppDatabase): StudySessionDao {
        return database.studySessionDao()
    }

    @Provides
    @Singleton
    fun provideStudyRecordDao(database: AppDatabase): StudyRecordDao {
        return database.studyRecordDao()
    }

    @Provides
    @Singleton
    fun provideUserStatsDao(database: AppDatabase): UserStatsDao {
        return database.userStatsDao()
    }

    @Provides
    @Singleton
    fun provideUserSettingsDao(database: AppDatabase): UserSettingsDao {
        return database.userSettingsDao()
    }
}
