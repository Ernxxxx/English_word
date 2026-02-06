package com.example.englishword.ui.home

import com.example.englishword.domain.model.LevelWithProgress
import com.example.englishword.domain.model.ParentLevelWithChildren

/**
 * UI State for the Home screen.
 * Contains all data needed to render the home screen.
 */
data class HomeUiState(
    /**
     * Whether data is currently loading.
     */
    val isLoading: Boolean = true,

    /**
     * List of levels with their progress information.
     * Kept for backward compatibility.
     */
    val levels: List<LevelWithProgress> = emptyList(),

    /**
     * Hierarchical levels (parent levels with their children).
     */
    val parentLevels: List<ParentLevelWithChildren> = emptyList(),

    /**
     * Number of words studied today.
     */
    val todayStudiedCount: Int = 0,

    /**
     * Current streak in days.
     */
    val streak: Int = 0,

    /**
     * Whether the user has premium subscription.
     */
    val isPremium: Boolean = false,

    /**
     * Error message to display, if any.
     */
    val error: String? = null,

    /**
     * Whether the delete confirmation dialog is shown.
     */
    val showDeleteDialog: Boolean = false,

    /**
     * The level selected for deletion.
     */
    val levelToDelete: LevelWithProgress? = null,

    /**
     * Daily goal for words to study.
     */
    val dailyGoal: Int = 20,

    /**
     * Today's review count (for free users).
     */
    val todayReviewCount: Int = 0,

    /**
     * Whether to show the ad unlock dialog.
     */
    val showUnlockDialog: Boolean = false,

    /**
     * The level ID to unlock (when showing unlock dialog).
     */
    val levelToUnlock: Long? = null
) {
    /**
     * Progress towards daily goal as a fraction (0.0 to 1.0).
     */
    val dailyProgressFraction: Float
        get() = if (dailyGoal > 0) (todayStudiedCount.toFloat() / dailyGoal).coerceAtMost(1f) else 0f

    /**
     * Whether the daily goal has been achieved.
     */
    val isDailyGoalAchieved: Boolean
        get() = todayStudiedCount >= dailyGoal

    /**
     * Total word count across all levels.
     */
    val totalWordCount: Int
        get() = parentLevels.sumOf { it.totalWordCount }

    /**
     * Total mastered word count across all levels.
     */
    val totalMasteredCount: Int
        get() = parentLevels.sumOf { it.totalMasteredCount }

    /**
     * Maximum daily review count for free users.
     */
    val freeDailyReviewLimit: Int
        get() = 10

    /**
     * Remaining reviews for today (free users).
     */
    val remainingReviews: Int
        get() = if (isPremium) Int.MAX_VALUE else (freeDailyReviewLimit - todayReviewCount).coerceAtLeast(0)

    /**
     * Whether the user can review more today.
     */
    val canReviewMore: Boolean
        get() = isPremium || todayReviewCount < freeDailyReviewLimit
}

/**
 * Events that can occur on the Home screen.
 */
sealed class HomeEvent {
    data class NavigateToStudy(val levelId: Long) : HomeEvent()
    data class NavigateToWordList(val levelId: Long) : HomeEvent()
    data object NavigateToSettings : HomeEvent()
    data object NavigateToPremium : HomeEvent()
    data class ShowError(val message: String) : HomeEvent()
    data class ShowRewardedAd(val levelId: Long) : HomeEvent()
    data class UnitUnlocked(val levelId: Long) : HomeEvent()
}
