package com.example.englishword.di

import com.example.englishword.util.CrashReporter
import com.example.englishword.util.DebugCrashReporter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class InfraModule {
    @Binds
    @Singleton
    abstract fun bindCrashReporter(impl: DebugCrashReporter): CrashReporter
}
