package com.example.englishword.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "user_stats",
    indices = [
        Index(value = ["date"], unique = true)
    ]
)
data class UserStats(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String, // Format: "yyyy-MM-dd"
    val studiedCount: Int = 0,
    val streak: Int = 0
)
