package com.example.englishword.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tracks unit unlock status for free users.
 * Premium users have all units unlocked permanently.
 * Free users can unlock units by watching ads (3 hours).
 */
@Entity(
    tableName = "unit_unlocks",
    foreignKeys = [
        ForeignKey(
            entity = Level::class,
            parentColumns = ["id"],
            childColumns = ["levelId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["levelId"], unique = true)]
)
data class UnitUnlock(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val levelId: Long,
    val unlockUntil: Long // Timestamp when unlock expires (0 = locked)
) {
    fun isUnlocked(): Boolean {
        return unlockUntil > System.currentTimeMillis()
    }
}
