package com.example.englishword.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Database migrations for English Word app.
 *
 * Version History:
 * - Version 1: Initial release with levels, words, study_sessions, study_records, user_stats, user_settings
 * - Version 2: No schema changes (app updates)
 * - Version 3: Added unit_unlocks table for free tier monetization
 */
object Migrations {

    /**
     * Migration from version 1 to 2.
     * No schema changes - this is a placeholder for future reference.
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // No schema changes in version 2
            // This migration exists to provide a safe upgrade path
        }
    }

    /**
     * Migration from version 2 to 3.
     * Adds unit_unlocks table for tracking ad-based unit unlocks.
     */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Create unit_unlocks table
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS `unit_unlocks` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `levelId` INTEGER NOT NULL,
                    `unlockUntil` INTEGER NOT NULL,
                    FOREIGN KEY(`levelId`) REFERENCES `levels`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
            """.trimIndent())

            // Create index on levelId
            database.execSQL("""
                CREATE UNIQUE INDEX IF NOT EXISTS `index_unit_unlocks_levelId` ON `unit_unlocks` (`levelId`)
            """.trimIndent())
        }
    }

    /**
     * Migration from version 1 to 3 (skip version 2).
     * Combines all changes for users upgrading from initial release.
     */
    val MIGRATION_1_3 = object : Migration(1, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Apply all migrations from 1 to 3
            MIGRATION_1_2.migrate(database)
            MIGRATION_2_3.migrate(database)
        }
    }

    /**
     * Get all migrations for the database builder.
     */
    fun getAllMigrations(): Array<Migration> {
        return arrayOf(
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_1_3
        )
    }
}
