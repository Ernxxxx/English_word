package com.example.englishword.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "study_results",
    foreignKeys = [
        ForeignKey(
            entity = StudySession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Word::class,
            parentColumns = ["id"],
            childColumns = ["wordId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["sessionId"]),
        Index(value = ["wordId"]),
        Index(value = ["answeredAt"])
    ]
)
data class StudyResult(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val wordId: Long,
    val isCorrect: Boolean,
    val responseTimeMs: Long? = null,
    val answeredAt: Long = System.currentTimeMillis()
)
