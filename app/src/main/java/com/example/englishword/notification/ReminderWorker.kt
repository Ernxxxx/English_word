package com.example.englishword.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.englishword.util.CrashReporter
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Worker that shows the daily reminder notification.
 */
@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val notificationHelper: NotificationHelper,
    private val crashReporter: CrashReporter
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "daily_reminder_work"
    }

    override suspend fun doWork(): Result {
        return try {
            // Show the reminder notification
            notificationHelper.showReminderNotification(
                title = "英単語の時間です",
                message = "今日の学習を始めましょう！毎日続けることが大切です。"
            )
            Result.success()
        } catch (e: Exception) {
            crashReporter.recordException(e)
            Result.failure()
        }
    }
}
