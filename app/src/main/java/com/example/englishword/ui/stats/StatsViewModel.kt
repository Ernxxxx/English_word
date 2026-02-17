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
import kotlinx.coroutines.Job
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
    val weekOffset: Int = 0,
    val weekLabel: String = "",
    val canGoBackWeek: Boolean = true,
    val canGoForwardWeek: Boolean = false,
    val monthOffset: Int = 0,
    val canGoBackMonth: Boolean = true,
    val canGoForwardMonth: Boolean = false,
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
        /** Last N days for heatmap/stats data (covers ~3 months for navigation). */
        private const val MONTHLY_DAYS = 93

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

    private var loadJob: Job? = null
    private var currentStatsMap: Map<String, Int> = emptyMap()

    /** Max weeks back (limited by MONTHLY_DAYS data). */
    private val maxWeekBack = -(MONTHLY_DAYS / WEEKLY_DAYS - 1)
    /** Max months back for heatmap navigation. */
    private val maxMonthBack = -2

    /** Internal snapshot from combine lambda (data fields only, no dialog state). */
    private data class DataSnapshot(
        val weeklyData: List<DailyStudyData>,
        val monthlyData: List<DailyStudyData>,
        val weekOffset: Int,
        val weekLabel: String,
        val canGoBackWeek: Boolean,
        val canGoForwardWeek: Boolean,
        val monthOffset: Int,
        val canGoBackMonth: Boolean,
        val canGoForwardMonth: Boolean,
        val currentStreak: Int,
        val maxStreak: Int,
        val totalWordsStudied: Int,
        val totalWordsMastered: Int,
        val totalWordsAcquired: Int,
        val rememberedNewWords: Int,
        val rememberedReviewWords: Int,
        val totalWords: Int,
        val averageDaily: Float,
        val masteryDistribution: List<MasteryLevel>
    )

    init {
        loadStats()
    }

    /**
     * Load all statistics data.
     * Combines reactive Flow sources (total word count, mastered count, total studied,
     * average daily) with one-shot suspend calls (streaks, mastery distribution, recent stats).
     */
    private fun loadStats() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                // Load one-shot data in parallel via suspend calls
                val currentStreak = studyRepository.getCurrentStreak()
                val maxStreak = studyRepository.getMaxStreak()
                val masteryDistribution = buildMasteryDistribution()

                // Generate date-based list for the last 30 days (heatmap)
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
                    currentStatsMap = statsMap

                    // Compute weekly data with current offset
                    val currentOffset = _uiState.value.weekOffset
                    val (weeklyData, weekLabel) = computeWeeklyData(currentOffset, statsMap)

                    // Fill in monthly data (most recent 30 days, for heatmap)
                    val monthlyData = monthlyDates.map { date ->
                        DailyStudyData(
                            date = date,
                            count = statsMap[date] ?: 0
                        )
                    }

                    val currentMonthOffset = _uiState.value.monthOffset

                    // Return a data-only tuple; actual state merge happens in collect
                    DataSnapshot(
                        weeklyData = weeklyData,
                        monthlyData = monthlyData,
                        weekOffset = currentOffset,
                        weekLabel = weekLabel,
                        canGoBackWeek = currentOffset > maxWeekBack,
                        canGoForwardWeek = currentOffset < 0,
                        monthOffset = currentMonthOffset,
                        canGoBackMonth = currentMonthOffset > maxMonthBack,
                        canGoForwardMonth = currentMonthOffset < 0,
                        currentStreak = currentStreak,
                        maxStreak = maxStreak,
                        totalWordsStudied = totalStudied,
                        totalWordsMastered = totalAcquired,
                        totalWordsAcquired = totalAcquired,
                        rememberedNewWords = rememberedNewWords,
                        rememberedReviewWords = rememberedReviewWords,
                        totalWords = totalWords,
                        averageDaily = averageDaily,
                        masteryDistribution = masteryDistribution
                    )
                }.collect { snapshot ->
                    _uiState.update { current ->
                        current.copy(
                            weeklyData = snapshot.weeklyData,
                            monthlyData = snapshot.monthlyData,
                            weekOffset = snapshot.weekOffset,
                            weekLabel = snapshot.weekLabel,
                            canGoBackWeek = snapshot.canGoBackWeek,
                            canGoForwardWeek = snapshot.canGoForwardWeek,
                            monthOffset = snapshot.monthOffset,
                            canGoBackMonth = snapshot.canGoBackMonth,
                            canGoForwardMonth = snapshot.canGoForwardMonth,
                            currentStreak = snapshot.currentStreak,
                            maxStreak = snapshot.maxStreak,
                            totalWordsStudied = snapshot.totalWordsStudied,
                            totalWordsMastered = snapshot.totalWordsMastered,
                            totalWordsAcquired = snapshot.totalWordsAcquired,
                            rememberedNewWords = snapshot.rememberedNewWords,
                            rememberedReviewWords = snapshot.rememberedReviewWords,
                            totalWords = snapshot.totalWords,
                            averageDaily = snapshot.averageDaily,
                            masteryDistribution = snapshot.masteryDistribution,
                            isLoading = false,
                            error = null
                        )
                    }
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
        val mastered = countMap.entries
            .filter { it.key >= SrsCalculator.MAX_LEVEL }
            .sumOf { it.value }

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
     * Compute weekly data for the given offset from the stats map.
     * offset = 0 means current week, -1 = last week, etc.
     * Returns (weeklyData, label) pair.
     */
    private fun computeWeeklyData(
        offset: Int,
        statsMap: Map<String, Int>
    ): Pair<List<DailyStudyData>, String> {
        val today = LocalDate.now()
        val endDate = today.plusDays((offset * WEEKLY_DAYS).toLong())
        val startDate = endDate.minusDays((WEEKLY_DAYS - 1).toLong())

        val data = (0 until WEEKLY_DAYS).map { i ->
            val date = startDate.plusDays(i.toLong())
            DailyStudyData(
                date = date.format(dateFormatter),
                count = statsMap[date.format(dateFormatter)] ?: 0
            )
        }

        val label = "${startDate.monthValue}/${startDate.dayOfMonth} - ${endDate.monthValue}/${endDate.dayOfMonth}"
        return data to label
    }

    /**
     * Shift the weekly chart view by [delta] weeks (-1 = back, +1 = forward).
     */
    fun shiftWeek(delta: Int) {
        val currentOffset = _uiState.value.weekOffset
        val newOffset = (currentOffset + delta).coerceIn(maxWeekBack, 0)
        if (newOffset == currentOffset) return

        val (weeklyData, weekLabel) = computeWeeklyData(newOffset, currentStatsMap)
        _uiState.update {
            it.copy(
                weekOffset = newOffset,
                weeklyData = weeklyData,
                weekLabel = weekLabel,
                canGoBackWeek = newOffset > maxWeekBack,
                canGoForwardWeek = newOffset < 0
            )
        }
    }

    /**
     * Shift the monthly heatmap view by [delta] months (-1 = back, +1 = forward).
     */
    fun shiftMonth(delta: Int) {
        val currentOffset = _uiState.value.monthOffset
        val newOffset = (currentOffset + delta).coerceIn(maxMonthBack, 0)
        if (newOffset == currentOffset) return

        _uiState.update {
            it.copy(
                monthOffset = newOffset,
                canGoBackMonth = newOffset > maxMonthBack,
                canGoForwardMonth = newOffset < 0
            )
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
