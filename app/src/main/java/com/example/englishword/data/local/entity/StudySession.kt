package com.example.englishword.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "study_sessions",
    foreignKeys = [
        ForeignKey(
            entity = Level::class,
            parentColumns = ["id"],
            childColumns = ["levelId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["levelId"]),
        Index(value = ["startedAt"]),
        Index(value = ["completedAt"])
    ]
)
data class StudySession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val levelId: Long,
    val startedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val wordCount: Int = 0,
    val masteredCount: Int = 0,
    // Session progress fields (added in v4)
    val currentIndex: Int = 0,
    val knownCount: Int = 0,
    val againCount: Int = 0,
    val laterCount: Int = 0,
    val wordIds: String = "",         // Comma-separated word IDs
    val laterQueueIds: String = "",   // Comma-separated later queue word IDs
    val isReversed: Boolean = false   // Study direction
) {
    /**
     * Check if this session is in progress (not completed).
     */
    val isInProgress: Boolean
        get() = completedAt == null && wordIds.isNotEmpty()

    /**
     * Parse word IDs from comma-separated string.
     */
    fun getWordIdList(): List<Long> {
        return if (wordIds.isEmpty()) emptyList()
        else wordIds.split(",").mapNotNull { it.toLongOrNull() }
    }

    /**
     * Parse later queue word IDs from comma-separated string.
     */
    fun getLaterQueueIdList(): List<Long> {
        return if (laterQueueIds.isEmpty()) emptyList()
        else laterQueueIds.split(",").mapNotNull { it.toLongOrNull() }
    }

    companion object {
        /**
         * Convert list of word IDs to comma-separated string.
         */
        fun wordIdsToString(wordIds: List<Long>): String {
            return wordIds.joinToString(",")
        }
    }
}
