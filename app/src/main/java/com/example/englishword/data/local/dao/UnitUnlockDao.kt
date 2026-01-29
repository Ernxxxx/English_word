package com.example.englishword.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.englishword.data.local.entity.UnitUnlock
import kotlinx.coroutines.flow.Flow

@Dao
interface UnitUnlockDao {

    @Query("SELECT * FROM unit_unlocks WHERE levelId = :levelId")
    suspend fun getUnlock(levelId: Long): UnitUnlock?

    @Query("SELECT * FROM unit_unlocks WHERE levelId = :levelId")
    fun getUnlockFlow(levelId: Long): Flow<UnitUnlock?>

    @Query("SELECT * FROM unit_unlocks")
    fun getAllUnlocks(): Flow<List<UnitUnlock>>

    @Query("SELECT * FROM unit_unlocks")
    suspend fun getAllUnlocksSync(): List<UnitUnlock>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(unlock: UnitUnlock)

    @Query("UPDATE unit_unlocks SET unlockUntil = :unlockUntil WHERE levelId = :levelId")
    suspend fun updateUnlockTime(levelId: Long, unlockUntil: Long)

    @Query("DELETE FROM unit_unlocks WHERE levelId = :levelId")
    suspend fun delete(levelId: Long)

    @Query("DELETE FROM unit_unlocks")
    suspend fun deleteAll()
}
