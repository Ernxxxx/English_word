package com.example.englishword.ads

import android.content.Context
import com.example.englishword.data.repository.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing Ad-related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object AdModule {

    /**
     * Provides the AdManager singleton.
     */
    @Provides
    @Singleton
    fun provideAdManager(
        @ApplicationContext context: Context,
        settingsRepository: SettingsRepository
    ): AdManager {
        return AdManager(context, settingsRepository)
    }

    /**
     * Provides the InterstitialAdManager singleton.
     */
    @Provides
    @Singleton
    fun provideInterstitialAdManager(
        @ApplicationContext context: Context,
        settingsRepository: SettingsRepository
    ): InterstitialAdManager {
        return InterstitialAdManager(context, settingsRepository)
    }
}
