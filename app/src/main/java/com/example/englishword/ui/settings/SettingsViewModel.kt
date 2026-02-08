package com.example.englishword.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.englishword.data.repository.SettingsRepository
import com.example.englishword.notification.NotificationScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for Settings screen
 */
data class SettingsUiState(
    val isPremium: Boolean = false,
    val dailyGoal: Int = 20,
    val isNotificationEnabled: Boolean = true,
    val notificationTime: String = "09:00",
    val darkMode: String = SettingsRepository.DARK_MODE_SYSTEM,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val isStudyDirectionReversed: Boolean = false,
    val isQuizMode: Boolean = false,
    val appVersion: String = "1.0.0",
    val isLoading: Boolean = true,
    val error: String? = null
) {
    val darkModeDisplayText: String
        get() = when (darkMode) {
            SettingsRepository.DARK_MODE_SYSTEM -> "システム設定に従う"
            SettingsRepository.DARK_MODE_LIGHT -> "ライトモード"
            SettingsRepository.DARK_MODE_DARK -> "ダークモード"
            else -> "システム設定に従う"
        }
}

/**
 * Events for Settings screen
 */
sealed class SettingsEvent {
    data class DailyGoalChanged(val goal: Int) : SettingsEvent()
    data class NotificationEnabledChanged(val enabled: Boolean) : SettingsEvent()
    data class NotificationTimeChanged(val time: String) : SettingsEvent()
    data class DarkModeChanged(val mode: String) : SettingsEvent()
    data class SoundEnabledChanged(val enabled: Boolean) : SettingsEvent()
    data class VibrationEnabledChanged(val enabled: Boolean) : SettingsEvent()
    data class StudyDirectionReversedChanged(val reversed: Boolean) : SettingsEvent()
    data class StudyModeChanged(val isQuizMode: Boolean) : SettingsEvent()
    object ClearError : SettingsEvent()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val notificationScheduler: NotificationScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                // Combine first 5 flows
                val firstGroup = combine(
                    settingsRepository.isPremium(),
                    settingsRepository.getDailyGoal(),
                    settingsRepository.isNotificationEnabled(),
                    settingsRepository.getNotificationTime(),
                    settingsRepository.getDarkMode()
                ) { isPremium, dailyGoal, notifEnabled, notifTime, darkMode ->
                    listOf(isPremium, dailyGoal, notifEnabled, notifTime, darkMode)
                }

                // Combine remaining flows
                val secondGroup = combine(
                    settingsRepository.isSoundEnabled(),
                    settingsRepository.isVibrationEnabled(),
                    settingsRepository.isStudyDirectionReversed(),
                    settingsRepository.isQuizMode()
                ) { soundEnabled, vibrationEnabled, isReversed, isQuizMode ->
                    listOf(soundEnabled, vibrationEnabled, isReversed, isQuizMode)
                }

                // Combine both groups
                combine(firstGroup, secondGroup) { first, second ->
                    @Suppress("UNCHECKED_CAST")
                    SettingsUiState(
                        isPremium = first[0] as Boolean,
                        dailyGoal = first[1] as Int,
                        isNotificationEnabled = first[2] as Boolean,
                        notificationTime = first[3] as String,
                        darkMode = first[4] as String,
                        soundEnabled = second[0] as Boolean,
                        vibrationEnabled = second[1] as Boolean,
                        isStudyDirectionReversed = second[2] as Boolean,
                        isQuizMode = second[3] as Boolean,
                        isLoading = false
                    )
                }.collectLatest { state ->
                    _uiState.value = state
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = "設定の読み込みに失敗しました")
                }
            }
        }
    }

    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.DailyGoalChanged -> {
                updateDailyGoal(event.goal)
            }
            is SettingsEvent.NotificationEnabledChanged -> {
                updateNotificationEnabled(event.enabled)
            }
            is SettingsEvent.NotificationTimeChanged -> {
                updateNotificationTime(event.time)
            }
            is SettingsEvent.DarkModeChanged -> {
                updateDarkMode(event.mode)
            }
            is SettingsEvent.SoundEnabledChanged -> {
                updateSoundEnabled(event.enabled)
            }
            is SettingsEvent.VibrationEnabledChanged -> {
                updateVibrationEnabled(event.enabled)
            }
            is SettingsEvent.StudyDirectionReversedChanged -> {
                updateStudyDirectionReversed(event.reversed)
            }
            is SettingsEvent.StudyModeChanged -> {
                updateStudyMode(event.isQuizMode)
            }
            SettingsEvent.ClearError -> {
                _uiState.update { it.copy(error = null) }
            }
        }
    }

    private fun updateDailyGoal(goal: Int) {
        viewModelScope.launch {
            try {
                val success = settingsRepository.setDailyGoal(goal)
                if (!success) {
                    _uiState.update { it.copy(error = "保存に失敗しました") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "保存に失敗しました") }
            }
        }
    }

    private fun updateNotificationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                val success = settingsRepository.setNotificationEnabled(enabled)
                if (success) {
                    // Schedule or cancel notifications based on setting
                    if (enabled) {
                        val time = _uiState.value.notificationTime
                        notificationScheduler.scheduleFromTimeString(time)
                    } else {
                        notificationScheduler.cancelDailyReminder()
                    }
                } else {
                    _uiState.update { it.copy(error = "保存に失敗しました") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "保存に失敗しました") }
            }
        }
    }

    private fun updateNotificationTime(time: String) {
        viewModelScope.launch {
            try {
                val success = settingsRepository.setNotificationTime(time)
                if (success) {
                    // Reschedule notification with new time if enabled
                    if (_uiState.value.isNotificationEnabled) {
                        notificationScheduler.scheduleFromTimeString(time)
                    }
                } else {
                    _uiState.update { it.copy(error = "保存に失敗しました") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "保存に失敗しました") }
            }
        }
    }

    private fun updateDarkMode(mode: String) {
        viewModelScope.launch {
            try {
                val success = settingsRepository.setDarkMode(mode)
                if (!success) {
                    _uiState.update { it.copy(error = "保存に失敗しました") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "保存に失敗しました") }
            }
        }
    }

    private fun updateSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                val success = settingsRepository.setSoundEnabled(enabled)
                if (!success) {
                    _uiState.update { it.copy(error = "保存に失敗しました") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "保存に失敗しました") }
            }
        }
    }

    private fun updateVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                val success = settingsRepository.setVibrationEnabled(enabled)
                if (!success) {
                    _uiState.update { it.copy(error = "保存に失敗しました") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "保存に失敗しました") }
            }
        }
    }

    private fun updateStudyDirectionReversed(reversed: Boolean) {
        viewModelScope.launch {
            try {
                val success = settingsRepository.setStudyDirectionReversed(reversed)
                if (!success) {
                    _uiState.update { it.copy(error = "保存に失敗しました") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "保存に失敗しました") }
            }
        }
    }

    private fun updateStudyMode(isQuizMode: Boolean) {
        viewModelScope.launch {
            try {
                val mode = if (isQuizMode) {
                    SettingsRepository.STUDY_MODE_QUIZ
                } else {
                    SettingsRepository.STUDY_MODE_FLASHCARD
                }
                val success = settingsRepository.setStudyMode(mode)
                if (!success) {
                    _uiState.update { it.copy(error = "保存に失敗しました") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "保存に失敗しました") }
            }
        }
    }
}
