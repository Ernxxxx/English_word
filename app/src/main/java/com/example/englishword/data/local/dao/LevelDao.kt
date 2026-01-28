package com.example.englishword.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.englishword.data.local.entity.Level
import kotlinx.coroutines.flow.Flow

@Dao
interface LevelDao {

    @Query("SELECT * FROM levels ORDER BY orderIndex ASC")
    fun getAllLevels(): Flow<List<Level>>

    @Query("SELECT * FROM levels ORDER BY orderIndex ASC")
    suspend fun getAllLevelsSync(): List<Level>

    // 親カテゴリのみ取得（中学1年、中学2年など）
    @Query("SELECT * FROM levels WHERE parentId IS NULL ORDER BY orderIndex ASC")
    fun getParentLevels(): Flow<List<Level>>

    @Query("SELECT * FROM levels WHERE parentId IS NULL ORDER BY orderIndex ASC")
    suspend fun getParentLevelsSync(): List<Level>

    // 子カテゴリ取得（Unit 1, Unit 2など）
    @Query("SELECT * FROM levels WHERE parentId = :parentId ORDER BY orderIndex ASC")
    fun getChildLevels(parentId: Long): Flow<List<Level>>

    @Query("SELECT * FROM levels WHERE parentId = :parentId ORDER BY orderIndex ASC")
    suspend fun getChildLevelsSync(parentId: Long): List<Level>

    @Query("SELECT * FROM levels WHERE id = :id")
    fun getLevelById(id: Long): Flow<Level?>

    @Query("SELECT * FROM levels WHERE id = :id")
    suspend fun getLevelByIdSync(id: Long): Level?

    @Query("SELECT * FROM levels WHERE name = :name LIMIT 1")
    suspend fun getLevelByName(name: String): Level?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(level: Level): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(levels: List<Level>): List<Long>

    @Update
    suspend fun update(level: Level)

    @Delete
    suspend fun delete(level: Level)

    @Query("DELETE FROM levels WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM levels")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM levels")
    fun getLevelCount(): Flow<Int>

    @Query("SELECT MAX(orderIndex) FROM levels")
    suspend fun getMaxOrderIndex(): Int?
}
