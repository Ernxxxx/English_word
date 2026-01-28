package com.example.englishword.billing

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing billing-related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object BillingModule {

    /**
     * Provides the BillingClientWrapper singleton.
     * This wrapper handles all direct interactions with Google Play BillingClient.
     */
    @Provides
    @Singleton
    fun provideBillingClientWrapper(
        @ApplicationContext context: Context
    ): BillingClientWrapper {
        return BillingClientWrapper(context)
    }
}
