package com.example.englishword.ui.study

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.englishword.ads.AdManager
import com.example.englishword.data.local.dao.StudySessionDao
import com.example.englishword.data.repository.StudyRepository
import com.example.englishword.ui.navigation.NavArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Sealed class for study result UI state.
 * Independent from StudyUiState to decouple result screen from study session lifecycle.
 */
sealed class StudyResultUiState {
    data object Loading : StudyResultUiState()

    data class Success(
        val sessionId: Long,
        val levelId: Long,
        val totalCount: Int,
        val knownCount: Int,
        val againCount: Int,
        val laterCount: Int,
        val streak: Int
    ) : StudyResultUiState()

    data class Error(val message: String) : StudyResultUiState()
}

/**
 * ViewModel for StudyResultScreen.
 * Loads session results from DB using sessionId, making the result screen
 * resilient to process death and configuration changes.
 */
@HiltViewModel
class StudyResultViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val studySessionDao: StudySessionDao,
    private val studyRepository: StudyRepository,
    val adManager: AdManager
) : ViewModel() {

    private val sessionId: Long = savedStateHandle[NavArgs.SESSION_ID] ?: 0L

    private val _uiState = MutableStateFlow<StudyResultUiState>(StudyResultUiState.Loading)
    val uiState: StateFlow<StudyResultUiState> = _uiState.asStateFlow()

    init {
        loadSessionResult()
    }

    private fun loadSessionResult() {
        viewModelScope.launch {
            try {
                val session = studySessionDao.getSessionByIdSync(sessionId)
                if (session == null) {
                    _uiState.value = StudyResultUiState.Error("セッションが見つかりません")
                    return@launch
                }
                val streak = studyRepository.getCurrentStreak()
                _uiState.value = StudyResultUiState.Success(
                    sessionId = session.id,
                    levelId = session.levelId,
                    totalCount = session.wordCount,
                    knownCount = session.knownCount,
                    againCount = session.againCount,
                    laterCount = session.laterCount,
                    streak = streak
                )
            } catch (e: Exception) {
                _uiState.value = StudyResultUiState.Error("結果の読み込みに失敗しました")
            }
        }
    }
}
