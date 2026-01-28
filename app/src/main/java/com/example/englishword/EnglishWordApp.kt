package com.example.englishword

import android.app.Application
import com.example.englishword.ads.AdManager
import com.example.englishword.ads.InterstitialAdManager
import com.example.englishword.data.local.InitialDataSeeder
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Application class for the English Word Learning App.
 * Annotated with @HiltAndroidApp to enable Hilt dependency injection.
 */
@HiltAndroidApp
class EnglishWordApp : Application() {

    @Inject
    lateinit var initialDataSeeder: InitialDataSeeder

    @Inject
    lateinit var adManager: AdManager

    @Inject
    lateinit var interstitialAdManager: InterstitialAdManager

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        // Initialize AdMob SDK
        adManager.initialize()

        // Seed initial data if the database is empty
        applicationScope.launch {
            initialDataSeeder.seedIfNeeded()
        }

        // Pre-load interstitial ad
        interstitialAdManager.loadAd()
    }
}
