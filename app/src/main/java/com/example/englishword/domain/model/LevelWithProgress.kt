package com.example.englishword.domain.model

/**
 * Data class representing a Level with its learning progress.
 * Combines Level information with word count and mastery statistics.
 */
data class LevelWithProgress(
    /**
     * The level information.
     */
    val level: Level,

    /**
     * Total number of words in this level.
     */
    val wordCount: Int,

    /**
     * Number of words that have been mastered (reached max SRS level).
     */
    val masteredCount: Int
) {
    /**
     * Gets the progress as a fraction (0.0 to 1.0).
     */
    val progressFraction: Float
        get() = if (wordCount == 0) 0f else masteredCount.toFloat() / wordCount

    /**
     * Gets the progress as a percentage (0-100).
     */
    val progressPercent: Int
        get() = if (wordCount == 0) 0 else (masteredCount * 100) / wordCount

    /**
     * Gets the number of words still being learned.
     */
    val learningCount: Int
        get() = wordCount - masteredCount

    /**
     * Checks if all words in this level are mastered.
     */
    val isCompleted: Boolean
        get() = wordCount > 0 && masteredCount >= wordCount

    /**
     * Checks if this level has no words.
     */
    val isEmpty: Boolean
        get() = wordCount == 0

    /**
     * Gets a formatted progress string (e.g., "25/100").
     */
    val progressText: String
        get() = "$masteredCount/$wordCount"

    /**
     * Gets a formatted percentage string (e.g., "25%").
     */
    val percentText: String
        get() = "$progressPercent%"
}

/**
 * Represents a level/category of words (e.g., "TOEFL", "GRE", "Beginner").
 */
data class Level(
    /**
     * Unique identifier for the level.
     */
    val id: Long,

    /**
     * Display name of the level.
     */
    val name: String,

    /**
     * Optional description of the level.
     */
    val description: String = "",

    /**
     * Display order for sorting levels.
     */
    val displayOrder: Int = 0,

    /**
     * Optional icon resource name or URL.
     */
    val iconName: String? = null,

    /**
     * Whether this level is unlocked/available.
     */
    val isUnlocked: Boolean = true,

    /**
     * Parent level ID for hierarchical structure.
     * null means this is a parent level (e.g., 中学1年).
     */
    val parentId: Long? = null
) {
    /**
     * Whether this is a parent level (has no parent).
     */
    val isParent: Boolean
        get() = parentId == null
}

/**
 * Represents a parent level with its child levels and combined progress.
 */
data class ParentLevelWithChildren(
    /**
     * The parent level.
     */
    val parentLevel: LevelWithProgress,

    /**
     * Child levels (units) with their progress.
     */
    val children: List<LevelWithProgress>,

    /**
     * Whether the children are currently expanded/visible.
     */
    val isExpanded: Boolean = false
) {
    /**
     * Total word count across all children.
     */
    val totalWordCount: Int
        get() = children.sumOf { it.wordCount }

    /**
     * Total mastered count across all children.
     */
    val totalMasteredCount: Int
        get() = children.sumOf { it.masteredCount }

    /**
     * Combined progress fraction.
     */
    val progressFraction: Float
        get() = if (totalWordCount == 0) 0f else totalMasteredCount.toFloat() / totalWordCount

    /**
     * Combined progress percentage.
     */
    val progressPercent: Int
        get() = if (totalWordCount == 0) 0 else (totalMasteredCount * 100) / totalWordCount
}
