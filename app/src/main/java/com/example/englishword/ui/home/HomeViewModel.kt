package com.example.englishword.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.englishword.data.local.entity.Level
import com.example.englishword.data.repository.LevelRepository
import com.example.englishword.ads.AdManager
import com.example.englishword.data.repository.SettingsRepository
import com.example.englishword.data.repository.StudyRepository
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
import kotlinx.coroutines.flow.first
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
                    settingsRepository.getDailyGoal()
                ) { parentLevels, todayStats, isPremium, dailyGoal ->
                    CombinedData(parentLevels, todayStats?.studiedCount ?: 0, todayStats?.streak ?: 0, isPremium, dailyGoal)
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
                        // Build hierarchical structure
                        val parentLevelsWithChildren = data.levels.map { parentLevel ->
                            buildParentWithChildren(parentLevel)
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
     */
    private suspend fun buildParentWithChildren(parentLevel: Level): ParentLevelWithChildren {
        val childLevels = levelRepository.getChildLevelsSync(parentLevel.id)
        val childrenWithProgress = childLevels.map { childLevel ->
            loadLevelProgress(childLevel)
        }

        val parentWithProgress = loadLevelProgress(parentLevel)

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
     */
    private suspend fun loadLevelProgress(level: Level): LevelWithProgress {
        val wordCount = wordRepository.getWordCountByLevel(level.id).first()
        val masteredCount = wordRepository.getMasteredCountByLevel(level.id).first()

        return LevelWithProgress(
            level = com.example.englishword.domain.model.Level(
                id = level.id,
                name = level.name,
                displayOrder = level.orderIndex,
                parentId = level.parentId
            ),
            wordCount = wordCount,
            masteredCount = masteredCount
        )
    }

    /**
     * Show the add level dialog.
     */
    fun showAddLevelDialog() {
        val currentState = _uiState.value
        if (!currentState.canAddLevel) {
            viewModelScope.launch {
                _events.emit(HomeEvent.NavigateToPremium)
            }
            return
        }
        _uiState.update { it.copy(showAddLevelDialog = true) }
    }

    /**
     * Hide the add level dialog.
     */
    fun hideAddLevelDialog() {
        _uiState.update { it.copy(showAddLevelDialog = false) }
    }

    /**
     * Add a new level.
     * Returns true if successful.
     */
    fun addLevel(name: String) {
        if (name.isBlank()) {
            viewModelScope.launch {
                _events.emit(HomeEvent.ShowError("Level name cannot be empty"))
            }
            return
        }

        val currentState = _uiState.value
        if (!currentState.canAddLevel) {
            viewModelScope.launch {
                _events.emit(HomeEvent.NavigateToPremium)
            }
            return
        }

        viewModelScope.launch {
            try {
                // Check if level with same name exists
                if (levelRepository.levelExists(name)) {
                    _events.emit(HomeEvent.ShowError("A level with this name already exists"))
                    return@launch
                }

                val levelId = levelRepository.insertLevelWithAutoOrder(name)
                if (levelId > 0) {
                    hideAddLevelDialog()
                    // Data will be refreshed automatically through Flow
                } else {
                    _events.emit(HomeEvent.ShowError("Failed to create level"))
                }
            } catch (e: Exception) {
                _events.emit(HomeEvent.ShowError(e.message ?: "Failed to create level"))
            }
        }
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

    /**
     * Data class for combining multiple Flow results.
     */
    private data class CombinedData(
        val levels: List<Level>,
        val todayStudiedCount: Int,
        val streak: Int,
        val isPremium: Boolean,
        val dailyGoal: Int
    )
}
