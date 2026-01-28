package com.example.englishword.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "levels",
    indices = [
        Index(value = ["orderIndex"]),
        Index(value = ["parentId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = Level::class,
            parentColumns = ["id"],
            childColumns = ["parentId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Level(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val orderIndex: Int,
    val parentId: Long? = null, // null = 親カテゴリ（中学1年など）
    val createdAt: Long = System.currentTimeMillis()
)
