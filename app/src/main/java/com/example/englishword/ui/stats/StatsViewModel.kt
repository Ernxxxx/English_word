package com.example.englishword.ui.stats

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.englishword.data.local.dao.WordDao
import com.example.englishword.data.repository.StudyRepository
import com.example.englishword.data.repository.WordRepository
import com.example.englishword.util.SrsCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Data class representing a single day's study count for charts and heatmaps.
 *
 * @param date The date string in "yyyy-MM-dd" format
 * @param count Number of words studied on that day
 */
data class DailyStudyData(
    val date: String,
    val count: Int
)

/**
 * Data class representing the word count at a specific mastery level.
 *
 * @param level The mastery level (0-5)
 * @param count Number of words at this level
 * @param label Human-readable label for the level
 */
data class MasteryLevel(
    val level: Int,
    val count: Int,
    val label: String
)

/**
 * UI state for the Statistics screen.
 */
/**
 * Simple word summary for detail dialogs.
 */
data class WordSummary(
    val english: String,
    val japanese: String,
    val masteryLevel: Int
)

data class StatsUiState(
    val weeklyData: List<DailyStudyData> = emptyList(),
    val monthlyData: List<DailyStudyData> = emptyList(),
    val currentStreak: Int = 0,
    val maxStreak: Int = 0,
    val totalWordsStudied: Int = 0,
    val totalWordsMastered: Int = 0,
    val totalWordsAcquired: Int = 0,
    val rememberedNewWords: Int = 0,
    val rememberedReviewWords: Int = 0,
    val totalWords: Int = 0,
    val averageDaily: Float = 0f,
    val masteryDistribution: List<MasteryLevel> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val showMasteredWordsDialog: Boolean = false,
    val showTodayWordsDialog: Boolean = false,
    val showSrsExplanation: Boolean = false,
    val masteredWordsList: List<WordSummary> = emptyList(),
    val todayWordsList: List<WordSummary> = emptyList()
)

/**
 * ViewModel for the Statistics screen.
 * Aggregates data from UserStats, Words, and StudySessions to present
 * comprehensive learning statistics including streaks, weekly/monthly
 * charts, mastery distribution, and overall progress.
 */
@HiltViewModel
class StatsViewModel @Inject constructor(
    private val studyRepository: StudyRepository,
    private val wordRepository: WordRepository,
    private val wordDao: WordDao
) : ViewModel() {

    companion object {
        private const val TAG = "StatsViewModel"
        private const val WEEKLY_DAYS = 7
        private const val MONTHLY_DAYS = 30

        /** Human-readable labels for the 3 mastery categories. */
        private val CATEGORY_LABELS = listOf(
            "未学習",    // 0: masteryLevel == 0
            "学習中",    // 1: masteryLevel 1-4
            "習得済み"   // 2: masteryLevel >= 5
        )
    }

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        loadStats()
    }

    /**
     * Load all statistics data.
     * Combines reactive Flow sources (total word count, mastered count, total studied,
     * average daily) with one-shot suspend calls (streaks, mastery distribution, recent stats).
     */
    private fun loadStats() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                // Load one-shot data in parallel via suspend calls
                val currentStreak = studyRepository.getCurrentStreak()
                val maxStreak = studyRepository.getMaxStreak()
                val masteryDistribution = buildMasteryDistribution()

                // Generate date-based lists for the last 7 and 30 days
                val weeklyDates = generateDateRange(WEEKLY_DAYS)
                val monthlyDates = generateDateRange(MONTHLY_DAYS)

                // Combine reactive Flow sources for continuous updates.
                // Keep outer combine to 4 flows to avoid vararg-array inference issues.
                val totalCountsFlow = combine(
                    wordRepository.getTotalWordCount(),
                    wordRepository.getTotalAcquiredCount()
                ) { totalWords, totalAcquired ->
                    totalWords to totalAcquired
                }
                val studySummaryFlow = combine(
                    studyRepository.getTotalWordsStudied(),
                    studyRepository.getAverageStudiedCount()
                ) { totalStudied, averageDaily ->
                    totalStudied to averageDaily
                }
                val rememberedCountsFlow = combine(
                    studyRepository.getKnownNewWordCount(),
                    studyRepository.getKnownReviewWordCount()
                ) { rememberedNewWords, rememberedReviewWords ->
                    rememberedNewWords to rememberedReviewWords
                }

                combine(
                    totalCountsFlow,
                    studySummaryFlow,
                    rememberedCountsFlow,
                    studyRepository.getRecentStats(MONTHLY_DAYS)
                ) { totalCounts, studySummary, rememberedCounts, recentStats ->
                    val (totalWords, totalAcquired) = totalCounts
                    val (totalStudied, averageDaily) = studySummary
                    val (rememberedNewWords, rememberedReviewWords) = rememberedCounts
                    // Build a lookup map from date -> studiedCount
                    val statsMap = recentStats.associate { it.date to it.studiedCount }

                    // Fill in weekly data (most recent 7 days)
                    val weeklyData = weeklyDates.map { date ->
                        DailyStudyData(
                            date = date,
                            count = statsMap[date] ?: 0
                        )
                    }

                    // Fill in monthly data (most recent 30 days, for heatmap)
                    val monthlyData = monthlyDates.map { date ->
                        DailyStudyData(
                            date = date,
                            count = statsMap[date] ?: 0
                        )
                    }

                    StatsUiState(
                        weeklyData = weeklyData,
                        monthlyData = monthlyData,
                        currentStreak = currentStreak,
                        maxStreak = maxStreak,
                        totalWordsStudied = totalStudied,
                        totalWordsMastered = totalAcquired,
                        totalWordsAcquired = totalAcquired,
                        rememberedNewWords = rememberedNewWords,
                        rememberedReviewWords = rememberedReviewWords,
                        totalWords = totalWords,
                        averageDaily = averageDaily,
                        masteryDistribution = masteryDistribution,
                        isLoading = false,
                        error = null
                    )
                }.collect { state ->
                    _uiState.value = state
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadStats failed", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "統計データの読み込みに失敗しました"
                    )
                }
            }
        }
    }

    /**
     * Build the mastery distribution as 3 categories: 未学習 / 学習中 / 習得済み.
     * Aligns with home screen progress calculation.
     */
    private suspend fun buildMasteryDistribution(): List<MasteryLevel> {
        val rawDistribution = wordRepository.getMasteryDistribution()
        val countMap = rawDistribution.associate { it.masteryLevel to it.count }

        val notStarted = countMap[0] ?: 0
        val learning = (1 until SrsCalculator.MAX_LEVEL).sumOf { countMap[it] ?: 0 }
        val mastered = countMap[SrsCalculator.MAX_LEVEL] ?: 0

        return listOf(
            MasteryLevel(level = 0, count = notStarted, label = CATEGORY_LABELS[0]),
            MasteryLevel(level = 1, count = learning, label = CATEGORY_LABELS[1]),
            MasteryLevel(level = 2, count = mastered, label = CATEGORY_LABELS[2])
        )
    }

    /**
     * Generate a list of date strings for the last [days] days (inclusive of today),
     * ordered from oldest to newest.
     */
    private fun generateDateRange(days: Int): List<String> {
        val today = LocalDate.now()
        val startDate = today.minusDays((days - 1).toLong())

        return (0 until days).map { offset ->
            startDate.plusDays(offset.toLong()).format(dateFormatter)
        }
    }

    /**
     * Refresh all statistics data.
     * Can be called from the UI to trigger a full reload.
     */
    fun refresh() {
        loadStats()
    }

    // ==================== Dialog Control ====================

    fun showSrsExplanation() {
        _uiState.update { it.copy(showSrsExplanation = true) }
    }

    fun hideSrsExplanation() {
        _uiState.update { it.copy(showSrsExplanation = false) }
    }

    fun showMasteredWordsDialog() {
        viewModelScope.launch {
            val words = wordDao.getAcquiredWordsListSync().map {
                WordSummary(english = it.english, japanese = it.japanese, masteryLevel = it.masteryLevel)
            }
            _uiState.update { it.copy(showMasteredWordsDialog = true, masteredWordsList = words) }
        }
    }

    fun hideMasteredWordsDialog() {
        _uiState.update { it.copy(showMasteredWordsDialog = false) }
    }

    fun showTodayWordsDialog() {
        viewModelScope.launch {
            val todayStart = LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            val words = wordDao.getTodayStudiedWordsSync(todayStart).map {
                WordSummary(english = it.english, japanese = it.japanese, masteryLevel = it.masteryLevel)
            }
            _uiState.update { it.copy(showTodayWordsDialog = true, todayWordsList = words) }
        }
    }

    fun hideTodayWordsDialog() {
        _uiState.update { it.copy(showTodayWordsDialog = false) }
    }
}
