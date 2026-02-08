package com.example.englishword.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "study_records",
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
        Index(value = ["reviewedAt"]),
        Index(value = ["sessionId", "wordId"])
    ]
)
data class StudyRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val wordId: Long,
    val result: Int, // 0=forgot, 1=hard, 2=easy
    val reviewedAt: Long = System.currentTimeMillis(),
    val responseTimeMs: Long = 0L // Time in milliseconds the user took to respond
)
