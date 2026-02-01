package com.example.englishword.notification

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules daily reminder notifications using WorkManager.
 */
@Singleton
class NotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val workManager = WorkManager.getInstance(context)

    /**
     * Schedule a daily reminder at the specified time.
     *
     * @param hour Hour of the day (0-23)
     * @param minute Minute of the hour (0-59)
     */
    fun scheduleDailyReminder(hour: Int, minute: Int) {
        // Calculate initial delay
        val initialDelay = calculateInitialDelay(hour, minute)

        // Create periodic work request (repeat every 24 hours)
        val reminderRequest = PeriodicWorkRequestBuilder<ReminderWorker>(
            24, TimeUnit.HOURS
        )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()

        // Enqueue the work, replacing any existing work
        workManager.enqueueUniquePeriodicWork(
            ReminderWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            reminderRequest
        )
    }

    /**
     * Cancel the daily reminder.
     */
    fun cancelDailyReminder() {
        workManager.cancelUniqueWork(ReminderWorker.WORK_NAME)
    }

    /**
     * Calculate the delay until the next occurrence of the specified time.
     */
    private fun calculateInitialDelay(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If the target time has already passed today, schedule for tomorrow
        if (target.timeInMillis <= now.timeInMillis) {
            target.add(Calendar.DAY_OF_MONTH, 1)
        }

        return target.timeInMillis - now.timeInMillis
    }

    /**
     * Parse time string (HH:mm) and schedule reminder.
     */
    fun scheduleFromTimeString(timeString: String) {
        val parts = timeString.split(":")
        if (parts.size == 2) {
            val hour = parts[0].toIntOrNull() ?: 9
            val minute = parts[1].toIntOrNull() ?: 0
            scheduleDailyReminder(hour, minute)
        }
    }
}
