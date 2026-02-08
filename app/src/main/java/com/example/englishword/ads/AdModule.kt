package com.example.englishword.ads

import android.content.Context
import com.example.englishword.data.repository.SettingsRepository
import com.example.englishword.di.ApplicationScope
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
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
        settingsRepository: SettingsRepository,
        @ApplicationScope applicationScope: CoroutineScope
    ): AdManager {
        return AdManager(context, settingsRepository, applicationScope)
    }

}
