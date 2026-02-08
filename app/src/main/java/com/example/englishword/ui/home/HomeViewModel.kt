package com.example.englishword.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.englishword.data.local.entity.Level
import com.example.englishword.data.repository.LevelRepository
import com.example.englishword.ads.AdManager
import com.example.englishword.data.repository.SettingsRepository
import com.example.englishword.data.repository.StudyRepository
import com.example.englishword.data.repository.UnlockRepository
import com.example.englishword.data.local.dao.LevelWordStats
import com.example.englishword.data.repository.WordRepository
import com.example.englishword.domain.model.LevelWithProgress
import com.example.englishword.domain.model.ParentLevelWithChildren
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Home screen.
 * Manages level list, study statistics, and premium state.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val levelRepository: LevelRepository,
    private val wordRepository: WordRepository,
    private val studyRepository: StudyRepository,
    private val settingsRepository: SettingsRepository,
    private val unlockRepository: UnlockRepository,
    val adManager: AdManager // Added: AdManager injection for ad display
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<HomeEvent>()
    val events: SharedFlow<HomeEvent> = _events.asSharedFlow()

    init {
        loadData()
    }

    /**
     * Load all data for the home screen.
     */
    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                // Combine multiple data sources
                combine(
                    levelRepository.getParentLevels(),
                    studyRepository.getTodayStats(),
                    settingsRepository.isPremium(),
                    settingsRepository.getDailyGoal(),
                    unlockRepository.getTodayReviewCountFlow()
                ) { parentLevels, todayStats, isPremium, dailyGoal, todayReviewCount ->
                    CombinedData(parentLevels, todayStats?.studiedCount ?: 0, todayStats?.streak ?: 0, isPremium, dailyGoal, todayReviewCount)
                }
                    .catch { e ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = e.message ?: "Failed to load data"
                            )
                        }
                    }
                    .collect { data ->
                        // Fetch all level word stats in a single query (fixes N+1)
                        val statsMap = wordRepository.getLevelWordStats()

                        // Build hierarchical structure
                        val parentLevelsWithChildren = data.levels.map { parentLevel ->
                            buildParentWithChildren(parentLevel, data.isPremium, statsMap)
                        }

                        // Also create flat list for backward compatibility
                        val flatLevels = parentLevelsWithChildren.flatMap { parent ->
                            parent.children
                        }

                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                levels = flatLevels,
                                parentLevels = parentLevelsWithChildren,
                                todayStudiedCount = data.todayStudiedCount,
                                streak = data.streak,
                                isPremium = data.isPremium,
                                dailyGoal = data.dailyGoal,
                                todayReviewCount = data.todayReviewCount,
                                error = null
                            )
                        }
                    }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load data"
                    )
                }
            }
        }
    }

    /**
     * Build parent level with its children and progress.
     * Uses pre-fetched statsMap to avoid N+1 queries.
     */
    private suspend fun buildParentWithChildren(
        parentLevel: Level,
        isPremium: Boolean,
        statsMap: Map<Long, LevelWordStats>
    ): ParentLevelWithChildren {
        val childLevels = levelRepository.getChildLevelsSync(parentLevel.id)
        val childrenWithProgress = childLevels.map { childLevel ->
            loadLevelProgress(childLevel, isPremium, statsMap)
        }

        val parentWithProgress = loadLevelProgress(parentLevel, isPremium, statsMap)

        return ParentLevelWithChildren(
            parentLevel = parentWithProgress,
            children = childrenWithProgress,
            isExpanded = _uiState.value.parentLevels
                .find { it.parentLevel.level.id == parentLevel.id }
                ?.isExpanded ?: false
        )
    }

    /**
     * Toggle expansion state of a parent level.
     */
    fun toggleParentExpansion(parentId: Long) {
        _uiState.update { state ->
            state.copy(
                parentLevels = state.parentLevels.map { parent ->
                    if (parent.parentLevel.level.id == parentId) {
                        parent.copy(isExpanded = !parent.isExpanded)
                    } else {
                        parent
                    }
                }
            )
        }
    }

    /**
     * Load progress information for a level.
     * Uses pre-fetched statsMap to avoid individual queries per level.
     */
    private suspend fun loadLevelProgress(
        level: Level,
        isPremium: Boolean,
        statsMap: Map<Long, LevelWordStats>
    ): LevelWithProgress {
        val stats = statsMap[level.id]
        val wordCount = stats?.wordCount ?: 0
        val masteredCount = stats?.masteredCount ?: 0

        // Check if this is a child level (unit) and needs unlock check
        val isParentLevel = level.parentId == null
        val isLocked = if (isPremium || isParentLevel) {
            false
        } else {
            !unlockRepository.isUnitUnlocked(level.id, isPremium, isParentLevel)
        }

        val remainingTime = if (isLocked) 0L else unlockRepository.getRemainingUnlockTime(level.id)

        return LevelWithProgress(
            level = com.example.englishword.domain.model.Level(
                id = level.id,
                name = level.name,
                displayOrder = level.orderIndex,
                parentId = level.parentId
            ),
            wordCount = wordCount,
            masteredCount = masteredCount,
            isLocked = isLocked,
            remainingUnlockTimeMs = remainingTime
        )
    }

    /**
     * Show delete confirmation dialog for a level.
     */
    fun showDeleteDialog(level: LevelWithProgress) {
        _uiState.update {
            it.copy(
                showDeleteDialog = true,
                levelToDelete = level
            )
        }
    }

    /**
     * Hide the delete confirmation dialog.
     */
    fun hideDeleteDialog() {
        _uiState.update {
            it.copy(
                showDeleteDialog = false,
                levelToDelete = null
            )
        }
    }

    /**
     * Delete a level.
     */
    fun deleteLevel() {
        val levelToDelete = _uiState.value.levelToDelete ?: return

        viewModelScope.launch {
            try {
                val success = levelRepository.deleteLevel(levelToDelete.level.id)
                if (success) {
                    hideDeleteDialog()
                    // Data will be refreshed automatically through Flow
                } else {
                    _events.emit(HomeEvent.ShowError("Failed to delete level"))
                }
            } catch (e: Exception) {
                _events.emit(HomeEvent.ShowError(e.message ?: "Failed to delete level"))
            }
        }
    }

    /**
     * Navigate to study screen for a level.
     */
    fun navigateToStudy(levelId: Long) {
        viewModelScope.launch {
            _events.emit(HomeEvent.NavigateToStudy(levelId))
        }
    }

    /**
     * Navigate to word list for a level.
     */
    fun navigateToWordList(levelId: Long) {
        viewModelScope.launch {
            _events.emit(HomeEvent.NavigateToWordList(levelId))
        }
    }

    /**
     * Navigate to settings screen.
     */
    fun navigateToSettings() {
        viewModelScope.launch {
            _events.emit(HomeEvent.NavigateToSettings)
        }
    }

    /**
     * Navigate to premium screen.
     */
    fun navigateToPremium() {
        viewModelScope.launch {
            _events.emit(HomeEvent.NavigateToPremium)
        }
    }

    /**
     * Refresh data.
     */
    fun refresh() {
        loadData()
    }

    /**
     * Clear error state.
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    // ==================== Ad Methods (Added) ====================

    /**
     * Check if ads should be shown (not premium).
     */
    fun shouldShowAds(): Boolean {
        return !_uiState.value.isPremium
    }

    /**
     * Get banner ad unit ID.
     */
    fun getBannerAdUnitId(): String {
        return AdManager.BANNER_AD_UNIT_ID
    }

    // ==================== Unit Unlock Methods ====================

    /**
     * Unlock a unit after watching an ad.
     * @param levelId The level ID to unlock
     */
    fun unlockUnitWithAd(levelId: Long) {
        viewModelScope.launch {
            try {
                unlockRepository.unlockUnitWithAd(levelId)
                // Refresh data to update UI
                loadData()
            } catch (e: Exception) {
                _events.emit(HomeEvent.ShowError("Failed to unlock unit: ${e.message}"))
            }
        }
    }

    /**
     * Check if a level is locked.
     */
    suspend fun isLevelLocked(levelId: Long): Boolean {
        val isPremium = _uiState.value.isPremium
        if (isPremium) return false
        return !unlockRepository.isUnitUnlocked(levelId, isPremium, false)
    }

    /**
     * Get remaining unlock time for a level.
     */
    suspend fun getRemainingUnlockTime(levelId: Long): Long {
        return unlockRepository.getRemainingUnlockTime(levelId)
    }

    /**
     * Show the unlock dialog for a locked level.
     */
    fun showUnlockDialog(levelId: Long) {
        _uiState.update { it.copy(showUnlockDialog = true, levelToUnlock = levelId) }
    }

    /**
     * Hide the unlock dialog.
     */
    fun hideUnlockDialog() {
        _uiState.update { it.copy(showUnlockDialog = false, levelToUnlock = null) }
    }

    /**
     * Request to watch ad for unlock. Emits event for UI to show rewarded ad.
     */
    fun requestWatchAdForUnlock() {
        val levelId = _uiState.value.levelToUnlock ?: return
        hideUnlockDialog()
        viewModelScope.launch {
            _events.emit(HomeEvent.ShowRewardedAd(levelId))
        }
    }

    /**
     * Handle unlock after watching ad successfully.
     */
    fun onAdWatchedForUnlock(levelId: Long) {
        viewModelScope.launch {
            try {
                unlockRepository.unlockUnitWithAd(levelId)
                _events.emit(HomeEvent.UnitUnlocked(levelId))
                // Refresh data to update UI
                loadData()
            } catch (e: Exception) {
                _events.emit(HomeEvent.ShowError("Failed to unlock unit: ${e.message}"))
            }
        }
    }

    /**
     * Data class for combining multiple Flow results.
     */
    private data class CombinedData(
        val levels: List<Level>,
        val todayStudiedCount: Int,
        val streak: Int,
        val isPremium: Boolean,
        val dailyGoal: Int,
        val todayReviewCount: Int
    )
}
