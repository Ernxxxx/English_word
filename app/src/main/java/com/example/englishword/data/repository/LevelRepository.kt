package com.example.englishword.data.repository

import androidx.room.withTransaction
import com.example.englishword.data.local.AppDatabase
import com.example.englishword.data.local.dao.LevelDao
import com.example.englishword.data.local.dao.WordDao
import com.example.englishword.data.local.entity.Level
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for Level CRUD operations.
 * Provides reactive data access through Flow and suspend functions for write operations.
 */
@Singleton
class LevelRepository @Inject constructor(
    private val database: AppDatabase,
    private val levelDao: LevelDao,
    private val wordDao: WordDao
) {

    /**
     * Get all levels ordered by orderIndex.
     */
    fun getAllLevels(): Flow<List<Level>> {
        return levelDao.getAllLevels()
            .catch { e ->
                emit(emptyList())
            }
    }

    /**
     * Get parent levels only (中学1年, 中学2年, 中学3年 etc.)
     */
    fun getParentLevels(): Flow<List<Level>> {
        return levelDao.getParentLevels()
            .catch { e ->
                emit(emptyList())
            }
    }

    /**
     * Get parent levels synchronously.
     */
    suspend fun getParentLevelsSync(): List<Level> {
        return try {
            levelDao.getParentLevelsSync()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Get child levels for a parent (Unit 1, Unit 2 etc.)
     */
    fun getChildLevels(parentId: Long): Flow<List<Level>> {
        return levelDao.getChildLevels(parentId)
            .catch { e ->
                emit(emptyList())
            }
    }

    /**
     * Get child levels synchronously.
     */
    suspend fun getChildLevelsSync(parentId: Long): List<Level> {
        return try {
            levelDao.getChildLevelsSync(parentId)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Get a level by its ID.
     */
    fun getLevelById(levelId: Long): Flow<Level?> {
        return levelDao.getLevelById(levelId)
            .catch { e ->
                emit(null)
            }
    }

    /**
     * Get a level by ID synchronously.
     */
    suspend fun getLevelByIdSync(levelId: Long): Level? {
        return try {
            levelDao.getLevelByIdSync(levelId)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get total count of levels.
     */
    fun getLevelCount(): Flow<Int> {
        return levelDao.getLevelCount()
            .catch { e ->
                emit(0)
            }
    }

    /**
     * Get levels with their word counts.
     */
    fun getLevelsWithWordCounts(): Flow<List<LevelWithCount>> {
        return levelDao.getAllLevels()
            .map { levels ->
                levels.map { level ->
                    LevelWithCount(
                        level = level,
                        wordCount = 0, // Will be populated by the UI layer if needed
                        masteredCount = 0
                    )
                }
            }
            .catch { e ->
                emit(emptyList())
            }
    }

    /**
     * Insert a new level.
     * Returns the ID of the inserted level, or -1 on error.
     */
    suspend fun insertLevel(level: Level): Long {
        return try {
            levelDao.insert(level)
        } catch (e: Exception) {
            -1L
        }
    }

    /**
     * Insert a new level with auto-generated orderIndex.
     */
    suspend fun insertLevelWithAutoOrder(name: String): Long {
        return try {
            val maxIndex = levelDao.getMaxOrderIndex() ?: -1
            val newLevel = Level(
                name = name,
                orderIndex = maxIndex + 1
            )
            levelDao.insert(newLevel)
        } catch (e: Exception) {
            -1L
        }
    }

    /**
     * Insert multiple levels.
     * Returns the list of inserted IDs.
     */
    suspend fun insertLevels(levels: List<Level>): List<Long> {
        return try {
            levelDao.insertAll(levels)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Update an existing level.
     * Returns true on success, false on error.
     */
    suspend fun updateLevel(level: Level): Boolean {
        return try {
            levelDao.update(level)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Update the name of a level.
     */
    suspend fun updateLevelName(levelId: Long, newName: String): Boolean {
        return try {
            val level = levelDao.getLevelByIdSync(levelId) ?: return false
            levelDao.update(level.copy(name = newName))
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Reorder levels by updating their orderIndex values.
     */
    suspend fun reorderLevels(levelIds: List<Long>): Boolean {
        return try {
            database.withTransaction {
                levelIds.forEachIndexed { index, levelId ->
                    val level = levelDao.getLevelByIdSync(levelId)
                    level?.let {
                        levelDao.update(it.copy(orderIndex = index))
                    }
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Delete a level by its ID.
     * This will also delete all words in the level due to CASCADE.
     * Returns true on success, false on error.
     */
    suspend fun deleteLevel(levelId: Long): Boolean {
        return try {
            levelDao.deleteById(levelId)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Delete a level.
     */
    suspend fun deleteLevel(level: Level): Boolean {
        return try {
            levelDao.delete(level)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Delete all levels.
     * Returns true on success, false on error.
     */
    suspend fun deleteAllLevels(): Boolean {
        return try {
            levelDao.deleteAll()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if a level with the given name exists.
     */
    suspend fun levelExists(name: String): Boolean {
        return try {
            levelDao.getLevelByName(name) != null
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * Data class representing a level with word counts.
 */
data class LevelWithCount(
    val level: Level,
    val wordCount: Int,
    val masteredCount: Int
) {
    val progressPercent: Float
        get() = if (wordCount > 0) (masteredCount.toFloat() / wordCount) * 100f else 0f
}
