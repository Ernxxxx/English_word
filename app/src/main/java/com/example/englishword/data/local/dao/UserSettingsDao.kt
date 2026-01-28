package com.example.englishword.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.englishword.data.local.entity.UserSettings
import kotlinx.coroutines.flow.Flow

@Dao
interface UserSettingsDao {

    @Query("SELECT * FROM user_settings")
    fun getAllSettings(): Flow<List<UserSettings>>

    @Query("SELECT * FROM user_settings")
    suspend fun getAllSettingsSync(): List<UserSettings>

    @Query("SELECT * FROM user_settings WHERE `key` = :key")
    fun getSettingByKey(key: String): Flow<UserSettings?>

    @Query("SELECT * FROM user_settings WHERE `key` = :key")
    suspend fun getSettingByKeySync(key: String): UserSettings?

    @Query("SELECT value FROM user_settings WHERE `key` = :key")
    suspend fun getValue(key: String): String?

    @Query("SELECT value FROM user_settings WHERE `key` = :key")
    fun getValueFlow(key: String): Flow<String?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(setting: UserSettings)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(settings: List<UserSettings>)

    @Delete
    suspend fun delete(setting: UserSettings)

    @Query("DELETE FROM user_settings WHERE `key` = :key")
    suspend fun deleteByKey(key: String)

    @Query("DELETE FROM user_settings")
    suspend fun deleteAll()

    // Convenience methods for common settings
    suspend fun setValue(key: String, value: String) {
        insert(UserSettings(key = key, value = value))
    }

    suspend fun setIntValue(key: String, value: Int) {
        insert(UserSettings(key = key, value = value.toString()))
    }

    suspend fun setBooleanValue(key: String, value: Boolean) {
        insert(UserSettings(key = key, value = value.toString()))
    }

    suspend fun getIntValue(key: String, defaultValue: Int = 0): Int {
        return getValue(key)?.toIntOrNull() ?: defaultValue
    }

    suspend fun getBooleanValue(key: String, defaultValue: Boolean = false): Boolean {
        return getValue(key)?.toBooleanStrictOrNull() ?: defaultValue
    }
}
