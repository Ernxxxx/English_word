package com.example.englishword

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.englishword.ads.AdManager
import com.example.englishword.data.local.InitialDataSeeder
import com.example.englishword.data.repository.SettingsRepository
import com.example.englishword.notification.NotificationScheduler
import com.example.englishword.util.CrashReporter
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Application class for the English Word Learning App.
 * Annotated with @HiltAndroidApp to enable Hilt dependency injection.
 */
@HiltAndroidApp
class EnglishWordApp : Application(), Configuration.Provider {

    @Inject
    lateinit var initialDataSeeder: InitialDataSeeder

    @Inject
    lateinit var adManager: AdManager

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var notificationScheduler: NotificationScheduler

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var crashReporter: CrashReporter

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        // Initialize AdMob SDK on background thread
        applicationScope.launch(Dispatchers.IO) {
            adManager.initialize()
        }

        // Seed initial data if the database is empty
        applicationScope.launch {
            initialDataSeeder.seedIfNeeded()
        }

        // Initialize notifications based on saved settings
        applicationScope.launch {
            initializeNotifications()
        }
    }

    private suspend fun initializeNotifications() {
        try {
            val isEnabled = settingsRepository.isNotificationEnabled().first()
            if (isEnabled) {
                val time = settingsRepository.getNotificationTime().first()
                notificationScheduler.scheduleFromTimeString(time)
            }
        } catch (e: Exception) {
            crashReporter.recordException(e)
        }
    }
}
