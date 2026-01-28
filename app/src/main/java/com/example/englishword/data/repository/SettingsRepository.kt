package com.example.englishword.data.repository

import com.example.englishword.data.local.dao.UserSettingsDao
import com.example.englishword.data.local.entity.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for user settings, premium state, and onboarding management.
 * Provides reactive data access through Flow and suspend functions for write operations.
 */
@Singleton
class SettingsRepository @Inject constructor(
    private val userSettingsDao: UserSettingsDao
) {

    // ==================== Settings Keys ====================

    companion object {
        // General Settings
        const val KEY_DAILY_GOAL = "daily_goal"
        const val KEY_DARK_MODE = "dark_mode"
        const val KEY_SOUND_ENABLED = "sound_enabled"
        const val KEY_VIBRATION_ENABLED = "vibration_enabled"
        const val KEY_LANGUAGE = "language"

        // Notification Settings
        const val KEY_NOTIFICATION_ENABLED = "notification_enabled"
        const val KEY_NOTIFICATION_TIME = "notification_time"

        // Premium Settings
        const val KEY_IS_PREMIUM = "is_premium"
        const val KEY_PREMIUM_PURCHASE_TOKEN = "premium_purchase_token"
        const val KEY_PREMIUM_EXPIRES_AT = "premium_expires_at"
        const val KEY_PREMIUM_SKU = "premium_sku"

        // Onboarding Settings
        const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        const val KEY_FIRST_LAUNCH = "first_launch"
        const val KEY_APP_VERSION_CODE = "app_version_code"

        // Study Settings
        const val KEY_SELECTED_LEVEL_ID = "selected_level_id"
        const val KEY_WORDS_PER_SESSION = "words_per_session"
        const val KEY_AUTO_PLAY_AUDIO = "auto_play_audio"
        const val KEY_SHOW_EXAMPLES = "show_examples"
        const val KEY_STUDY_DIRECTION_REVERSED = "study_direction_reversed" // true = 日→英

        // Default Values
        const val DEFAULT_DAILY_GOAL = 20
        const val DEFAULT_WORDS_PER_SESSION = 10
        const val DEFAULT_NOTIFICATION_TIME = "09:00"
        const val DARK_MODE_SYSTEM = "system"
        const val DARK_MODE_LIGHT = "light"
        const val DARK_MODE_DARK = "dark"
    }

    // ==================== Generic Settings Operations ====================

    /**
     * Get all settings.
     */
    fun getAllSettings(): Flow<List<UserSettings>> {
        return userSettingsDao.getAllSettings()
            .catch { e ->
                emit(emptyList())
            }
    }

    /**
     * Get a setting by key.
     */
    fun getSetting(key: String): Flow<UserSettings?> {
        return userSettingsDao.getSettingByKey(key)
            .catch { e ->
                emit(null)
            }
    }

    /**
     * Get a string value.
     */
    fun getStringValue(key: String): Flow<String?> {
        return userSettingsDao.getValueFlow(key)
            .catch { e ->
                emit(null)
            }
    }

    /**
     * Get a string value synchronously.
     */
    suspend fun getStringValueSync(key: String): String? {
        return try {
            userSettingsDao.getValue(key)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Set a string value.
     */
    suspend fun setStringValue(key: String, value: String): Boolean {
        return try {
            userSettingsDao.insert(UserSettings(key = key, value = value))
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get an integer value.
     */
    fun getIntValue(key: String, defaultValue: Int): Flow<Int> {
        return userSettingsDao.getValueFlow(key)
            .map { it?.toIntOrNull() ?: defaultValue }
            .catch { e ->
                emit(defaultValue)
            }
    }

    /**
     * Get an integer value synchronously.
     */
    suspend fun getIntValueSync(key: String, defaultValue: Int): Int {
        return try {
            userSettingsDao.getValue(key)?.toIntOrNull() ?: defaultValue
        } catch (e: Exception) {
            defaultValue
        }
    }

    /**
     * Set an integer value.
     */
    suspend fun setIntValue(key: String, value: Int): Boolean {
        return setStringValue(key, value.toString())
    }

    /**
     * Get a boolean value.
     */
    fun getBooleanValue(key: String, defaultValue: Boolean): Flow<Boolean> {
        return userSettingsDao.getValueFlow(key)
            .map { it?.toBooleanStrictOrNull() ?: defaultValue }
            .catch { e ->
                emit(defaultValue)
            }
    }

    /**
     * Get a boolean value synchronously.
     */
    suspend fun getBooleanValueSync(key: String, defaultValue: Boolean): Boolean {
        return try {
            userSettingsDao.getValue(key)?.toBooleanStrictOrNull() ?: defaultValue
        } catch (e: Exception) {
            defaultValue
        }
    }

    /**
     * Set a boolean value.
     */
    suspend fun setBooleanValue(key: String, value: Boolean): Boolean {
        return setStringValue(key, value.toString())
    }

    /**
     * Get a long value.
     */
    fun getLongValue(key: String, defaultValue: Long): Flow<Long> {
        return userSettingsDao.getValueFlow(key)
            .map { it?.toLongOrNull() ?: defaultValue }
            .catch { e ->
                emit(defaultValue)
            }
    }

    /**
     * Get a long value synchronously.
     */
    suspend fun getLongValueSync(key: String, defaultValue: Long): Long {
        return try {
            userSettingsDao.getValue(key)?.toLongOrNull() ?: defaultValue
        } catch (e: Exception) {
            defaultValue
        }
    }

    /**
     * Set a long value.
     */
    suspend fun setLongValue(key: String, value: Long): Boolean {
        return setStringValue(key, value.toString())
    }

    /**
     * Delete a setting.
     */
    suspend fun deleteSetting(key: String): Boolean {
        return try {
            userSettingsDao.deleteByKey(key)
            true
        } catch (e: Exception) {
            false
        }
    }

    // ==================== Daily Goal ====================

    /**
     * Get daily word goal.
     */
    fun getDailyGoal(): Flow<Int> {
        return getIntValue(KEY_DAILY_GOAL, DEFAULT_DAILY_GOAL)
    }

    /**
     * Get daily goal synchronously.
     */
    suspend fun getDailyGoalSync(): Int {
        return getIntValueSync(KEY_DAILY_GOAL, DEFAULT_DAILY_GOAL)
    }

    /**
     * Set daily word goal.
     */
    suspend fun setDailyGoal(goal: Int): Boolean {
        return setIntValue(KEY_DAILY_GOAL, goal)
    }

    // ==================== Dark Mode ====================

    /**
     * Get dark mode setting.
     * Returns "system", "light", or "dark".
     */
    fun getDarkMode(): Flow<String> {
        return userSettingsDao.getValueFlow(KEY_DARK_MODE)
            .map { it ?: DARK_MODE_SYSTEM }
            .catch { e ->
                emit(DARK_MODE_SYSTEM)
            }
    }

    /**
     * Set dark mode setting.
     */
    suspend fun setDarkMode(mode: String): Boolean {
        return setStringValue(KEY_DARK_MODE, mode)
    }

    // ==================== Sound & Vibration ====================

    /**
     * Check if sound is enabled.
     */
    fun isSoundEnabled(): Flow<Boolean> {
        return getBooleanValue(KEY_SOUND_ENABLED, true)
    }

    /**
     * Set sound enabled state.
     */
    suspend fun setSoundEnabled(enabled: Boolean): Boolean {
        return setBooleanValue(KEY_SOUND_ENABLED, enabled)
    }

    /**
     * Check if vibration is enabled.
     */
    fun isVibrationEnabled(): Flow<Boolean> {
        return getBooleanValue(KEY_VIBRATION_ENABLED, true)
    }

    /**
     * Set vibration enabled state.
     */
    suspend fun setVibrationEnabled(enabled: Boolean): Boolean {
        return setBooleanValue(KEY_VIBRATION_ENABLED, enabled)
    }

    // ==================== Notifications ====================

    /**
     * Check if notifications are enabled.
     */
    fun isNotificationEnabled(): Flow<Boolean> {
        return getBooleanValue(KEY_NOTIFICATION_ENABLED, true)
    }

    /**
     * Set notification enabled state.
     */
    suspend fun setNotificationEnabled(enabled: Boolean): Boolean {
        return setBooleanValue(KEY_NOTIFICATION_ENABLED, enabled)
    }

    /**
     * Get notification time (format: "HH:mm").
     */
    fun getNotificationTime(): Flow<String> {
        return userSettingsDao.getValueFlow(KEY_NOTIFICATION_TIME)
            .map { it ?: DEFAULT_NOTIFICATION_TIME }
            .catch { e ->
                emit(DEFAULT_NOTIFICATION_TIME)
            }
    }

    /**
     * Set notification time.
     */
    suspend fun setNotificationTime(time: String): Boolean {
        return setStringValue(KEY_NOTIFICATION_TIME, time)
    }

    // ==================== Premium State ====================

    /**
     * Check if user has premium.
     */
    fun isPremium(): Flow<Boolean> {
        return getBooleanValue(KEY_IS_PREMIUM, false)
    }

    /**
     * Check if user has premium synchronously.
     */
    suspend fun isPremiumSync(): Boolean {
        return getBooleanValueSync(KEY_IS_PREMIUM, false)
    }

    /**
     * Set premium state.
     */
    suspend fun setPremium(isPremium: Boolean): Boolean {
        return setBooleanValue(KEY_IS_PREMIUM, isPremium)
    }

    /**
     * Save premium purchase information.
     */
    suspend fun savePremiumPurchase(
        purchaseToken: String,
        sku: String = "premium_monthly",
        expiresAt: Long? = null
    ): Boolean {
        return try {
            setBooleanValue(KEY_IS_PREMIUM, true)
            setStringValue(KEY_PREMIUM_PURCHASE_TOKEN, purchaseToken)
            setStringValue(KEY_PREMIUM_SKU, sku)
            expiresAt?.let { setLongValue(KEY_PREMIUM_EXPIRES_AT, it) }
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get premium purchase token.
     */
    suspend fun getPremiumPurchaseToken(): String? {
        return getStringValueSync(KEY_PREMIUM_PURCHASE_TOKEN)
    }

    /**
     * Get premium expiration time.
     */
    suspend fun getPremiumExpiresAt(): Long? {
        val value = getLongValueSync(KEY_PREMIUM_EXPIRES_AT, -1L)
        return if (value == -1L) null else value
    }

    /**
     * Check if premium has expired.
     */
    suspend fun isPremiumExpired(): Boolean {
        val expiresAt = getPremiumExpiresAt() ?: return false
        return System.currentTimeMillis() > expiresAt
    }

    /**
     * Clear premium state (e.g., after refund or expiration).
     */
    suspend fun clearPremium(): Boolean {
        return try {
            setBooleanValue(KEY_IS_PREMIUM, false)
            deleteSetting(KEY_PREMIUM_PURCHASE_TOKEN)
            deleteSetting(KEY_PREMIUM_EXPIRES_AT)
            deleteSetting(KEY_PREMIUM_SKU)
            true
        } catch (e: Exception) {
            false
        }
    }

    // ==================== Onboarding ====================

    /**
     * Check if onboarding is completed.
     */
    fun isOnboardingCompleted(): Flow<Boolean> {
        return getBooleanValue(KEY_ONBOARDING_COMPLETED, false)
    }

    /**
     * Check if onboarding is completed synchronously.
     */
    suspend fun isOnboardingCompletedSync(): Boolean {
        return getBooleanValueSync(KEY_ONBOARDING_COMPLETED, false)
    }

    /**
     * Set onboarding completed state.
     */
    suspend fun setOnboardingCompleted(completed: Boolean): Boolean {
        return setBooleanValue(KEY_ONBOARDING_COMPLETED, completed)
    }

    /**
     * Check if this is the first launch.
     */
    suspend fun isFirstLaunch(): Boolean {
        return getBooleanValueSync(KEY_FIRST_LAUNCH, true)
    }

    /**
     * Mark that the app has been launched.
     */
    suspend fun markAppLaunched(): Boolean {
        return setBooleanValue(KEY_FIRST_LAUNCH, false)
    }

    // ==================== Study Settings ====================

    /**
     * Get selected level ID.
     */
    fun getSelectedLevelId(): Flow<Long?> {
        return userSettingsDao.getValueFlow(KEY_SELECTED_LEVEL_ID)
            .map { it?.toLongOrNull() }
            .catch { e ->
                emit(null)
            }
    }

    /**
     * Get selected level ID synchronously.
     */
    suspend fun getSelectedLevelIdSync(): Long? {
        return getStringValueSync(KEY_SELECTED_LEVEL_ID)?.toLongOrNull()
    }

    /**
     * Set selected level ID.
     */
    suspend fun setSelectedLevelId(levelId: Long): Boolean {
        return setLongValue(KEY_SELECTED_LEVEL_ID, levelId)
    }

    /**
     * Get words per session.
     */
    fun getWordsPerSession(): Flow<Int> {
        return getIntValue(KEY_WORDS_PER_SESSION, DEFAULT_WORDS_PER_SESSION)
    }

    /**
     * Set words per session.
     */
    suspend fun setWordsPerSession(count: Int): Boolean {
        return setIntValue(KEY_WORDS_PER_SESSION, count)
    }

    /**
     * Check if auto-play audio is enabled.
     */
    fun isAutoPlayAudioEnabled(): Flow<Boolean> {
        return getBooleanValue(KEY_AUTO_PLAY_AUDIO, true)
    }

    /**
     * Set auto-play audio enabled state.
     */
    suspend fun setAutoPlayAudioEnabled(enabled: Boolean): Boolean {
        return setBooleanValue(KEY_AUTO_PLAY_AUDIO, enabled)
    }

    /**
     * Check if examples should be shown.
     */
    fun isShowExamplesEnabled(): Flow<Boolean> {
        return getBooleanValue(KEY_SHOW_EXAMPLES, true)
    }

    /**
     * Set show examples enabled state.
     */
    suspend fun setShowExamplesEnabled(enabled: Boolean): Boolean {
        return setBooleanValue(KEY_SHOW_EXAMPLES, enabled)
    }

    /**
     * Check if study direction is reversed (Japanese→English).
     */
    fun isStudyDirectionReversed(): Flow<Boolean> {
        return getBooleanValue(KEY_STUDY_DIRECTION_REVERSED, false)
    }

    /**
     * Check if study direction is reversed synchronously.
     */
    suspend fun isStudyDirectionReversedSync(): Boolean {
        return getBooleanValueSync(KEY_STUDY_DIRECTION_REVERSED, false)
    }

    /**
     * Set study direction reversed state.
     */
    suspend fun setStudyDirectionReversed(reversed: Boolean): Boolean {
        return setBooleanValue(KEY_STUDY_DIRECTION_REVERSED, reversed)
    }

    // ==================== Reset Operations ====================

    /**
     * Reset all settings to defaults.
     */
    suspend fun resetAllSettings(): Boolean {
        return try {
            userSettingsDao.deleteAll()

            // Re-initialize default settings
            initializeDefaultSettings()

            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Initialize default settings.
     */
    suspend fun initializeDefaultSettings(): Boolean {
        return try {
            val defaults = listOf(
                UserSettings(key = KEY_DAILY_GOAL, value = DEFAULT_DAILY_GOAL.toString()),
                UserSettings(key = KEY_NOTIFICATION_ENABLED, value = "true"),
                UserSettings(key = KEY_NOTIFICATION_TIME, value = DEFAULT_NOTIFICATION_TIME),
                UserSettings(key = KEY_DARK_MODE, value = DARK_MODE_SYSTEM),
                UserSettings(key = KEY_SOUND_ENABLED, value = "true"),
                UserSettings(key = KEY_VIBRATION_ENABLED, value = "true"),
                UserSettings(key = KEY_IS_PREMIUM, value = "false"),
                UserSettings(key = KEY_ONBOARDING_COMPLETED, value = "false"),
                UserSettings(key = KEY_WORDS_PER_SESSION, value = DEFAULT_WORDS_PER_SESSION.toString()),
                UserSettings(key = KEY_AUTO_PLAY_AUDIO, value = "true"),
                UserSettings(key = KEY_SHOW_EXAMPLES, value = "true")
            )
            userSettingsDao.insertAll(defaults)
            true
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * Data class representing user preferences.
 */
data class UserPreferences(
    val dailyGoal: Int = SettingsRepository.DEFAULT_DAILY_GOAL,
    val darkMode: String = SettingsRepository.DARK_MODE_SYSTEM,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val notificationEnabled: Boolean = true,
    val notificationTime: String = SettingsRepository.DEFAULT_NOTIFICATION_TIME,
    val isPremium: Boolean = false,
    val onboardingCompleted: Boolean = false,
    val wordsPerSession: Int = SettingsRepository.DEFAULT_WORDS_PER_SESSION,
    val autoPlayAudio: Boolean = true,
    val showExamples: Boolean = true
)
