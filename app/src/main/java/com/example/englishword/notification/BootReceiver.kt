package com.example.englishword.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.englishword.data.repository.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Receiver that reschedules notifications after device reboot.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var notificationScheduler: NotificationScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Reschedule notifications after reboot
            CoroutineScope(Dispatchers.IO).launch {
                val isEnabled = settingsRepository.isNotificationEnabled().first()
                if (isEnabled) {
                    val time = settingsRepository.getNotificationTime().first()
                    notificationScheduler.scheduleFromTimeString(time)
                }
            }
        }
    }
}
