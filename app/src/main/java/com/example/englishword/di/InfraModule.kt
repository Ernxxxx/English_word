package com.example.englishword.di

import com.example.englishword.BuildConfig
import com.example.englishword.util.CrashReporter
import com.example.englishword.util.DebugCrashReporter
import com.example.englishword.util.ProductionCrashReporter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object InfraModule {
    @Provides
    @Singleton
    fun provideCrashReporter(
        debug: DebugCrashReporter,
        production: ProductionCrashReporter
    ): CrashReporter {
        return if (BuildConfig.DEBUG) debug else production
    }
}
