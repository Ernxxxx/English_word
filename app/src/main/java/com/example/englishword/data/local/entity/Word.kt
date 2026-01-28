package com.example.englishword.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "words",
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
        Index(value = ["english"]),
        Index(value = ["masteryLevel"]),
        Index(value = ["nextReviewAt"])
    ]
)
data class Word(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val levelId: Long,
    val english: String,
    val japanese: String,
    val exampleEn: String? = null,
    val exampleJa: String? = null,
    val masteryLevel: Int = 0, // 0-5: 0=new, 1-4=learning, 5=mastered
    val nextReviewAt: Long? = null,
    val reviewCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
