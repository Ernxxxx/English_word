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
 * - Version 4: Added session progress fields to study_sessions for session recovery
 * - Version 5: Added responseTimeMs column to study_records
 * - Version 6: Added composite index on words(levelId, nextReviewAt)
 * - Version 7: Added composite index on study_records(sessionId, wordId)
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
     * Migration from version 3 to 4.
     * Adds session progress fields to study_sessions for session recovery.
     */
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Add progress tracking columns to study_sessions
            database.execSQL("ALTER TABLE study_sessions ADD COLUMN currentIndex INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE study_sessions ADD COLUMN knownCount INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE study_sessions ADD COLUMN againCount INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE study_sessions ADD COLUMN laterCount INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE study_sessions ADD COLUMN wordIds TEXT NOT NULL DEFAULT ''")
            database.execSQL("ALTER TABLE study_sessions ADD COLUMN laterQueueIds TEXT NOT NULL DEFAULT ''")
            database.execSQL("ALTER TABLE study_sessions ADD COLUMN isReversed INTEGER NOT NULL DEFAULT 0")
        }
    }

    /**
     * Migration from version 1 to 4 (skip versions 2, 3).
     * Combines all changes for users upgrading from initial release.
     */
    val MIGRATION_1_4 = object : Migration(1, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            MIGRATION_1_2.migrate(database)
            MIGRATION_2_3.migrate(database)
            MIGRATION_3_4.migrate(database)
        }
    }

    /**
     * Migration from version 2 to 4 (skip version 3).
     */
    val MIGRATION_2_4 = object : Migration(2, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            MIGRATION_2_3.migrate(database)
            MIGRATION_3_4.migrate(database)
        }
    }

    /**
     * Migration from version 4 to 5.
     * Adds responseTimeMs column to study_records for tracking user response time.
     */
    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE study_records ADD COLUMN responseTimeMs INTEGER NOT NULL DEFAULT 0")
        }
    }

    /**
     * Migration from version 3 to 5 (skip version 4).
     */
    val MIGRATION_3_5 = object : Migration(3, 5) {
        override fun migrate(database: SupportSQLiteDatabase) {
            MIGRATION_3_4.migrate(database)
            MIGRATION_4_5.migrate(database)
        }
    }

    /**
     * Migration from version 2 to 5 (skip versions 3, 4).
     */
    val MIGRATION_2_5 = object : Migration(2, 5) {
        override fun migrate(database: SupportSQLiteDatabase) {
            MIGRATION_2_3.migrate(database)
            MIGRATION_3_4.migrate(database)
            MIGRATION_4_5.migrate(database)
        }
    }

    /**
     * Migration from version 1 to 5 (skip versions 2, 3, 4).
     */
    val MIGRATION_1_5 = object : Migration(1, 5) {
        override fun migrate(database: SupportSQLiteDatabase) {
            MIGRATION_1_2.migrate(database)
            MIGRATION_2_3.migrate(database)
            MIGRATION_3_4.migrate(database)
            MIGRATION_4_5.migrate(database)
        }
    }

    /**
     * Migration from version 5 to 6.
     * Adds composite index on (levelId, nextReviewAt) for optimized review queries.
     */
    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_words_levelId_nextReviewAt` ON `words` (`levelId`, `nextReviewAt`)")
        }
    }

    /**
     * Migration from version 4 to 6 (skip version 5).
     */
    val MIGRATION_4_6 = object : Migration(4, 6) {
        override fun migrate(database: SupportSQLiteDatabase) {
            MIGRATION_4_5.migrate(database)
            MIGRATION_5_6.migrate(database)
        }
    }

    /**
     * Migration from version 3 to 6 (skip versions 4, 5).
     */
    val MIGRATION_3_6 = object : Migration(3, 6) {
        override fun migrate(database: SupportSQLiteDatabase) {
            MIGRATION_3_4.migrate(database)
            MIGRATION_4_5.migrate(database)
            MIGRATION_5_6.migrate(database)
        }
    }

    /**
     * Migration from version 2 to 6 (skip versions 3, 4, 5).
     */
    val MIGRATION_2_6 = object : Migration(2, 6) {
        override fun migrate(database: SupportSQLiteDatabase) {
            MIGRATION_2_3.migrate(database)
            MIGRATION_3_4.migrate(database)
            MIGRATION_4_5.migrate(database)
            MIGRATION_5_6.migrate(database)
        }
    }

    /**
     * Migration from version 1 to 6 (skip versions 2, 3, 4, 5).
     */
    val MIGRATION_1_6 = object : Migration(1, 6) {
        override fun migrate(database: SupportSQLiteDatabase) {
            MIGRATION_1_2.migrate(database)
            MIGRATION_2_3.migrate(database)
            MIGRATION_3_4.migrate(database)
            MIGRATION_4_5.migrate(database)
            MIGRATION_5_6.migrate(database)
        }
    }

    /**
     * Migration from version 6 to 7.
     * Adds composite index on study_records(sessionId, wordId) for optimized lookups.
     */
    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_study_records_sessionId_wordId` ON `study_records` (`sessionId`, `wordId`)")
        }
    }

    /**
     * Migration from version 5 to 7 (skip version 6).
     */
    val MIGRATION_5_7 = object : Migration(5, 7) {
        override fun migrate(database: SupportSQLiteDatabase) {
            MIGRATION_5_6.migrate(database)
            MIGRATION_6_7.migrate(database)
        }
    }

    /**
     * Migration from version 4 to 7 (skip versions 5, 6).
     */
    val MIGRATION_4_7 = object : Migration(4, 7) {
        override fun migrate(database: SupportSQLiteDatabase) {
            MIGRATION_4_5.migrate(database)
            MIGRATION_5_6.migrate(database)
            MIGRATION_6_7.migrate(database)
        }
    }

    /**
     * Migration from version 3 to 7 (skip versions 4, 5, 6).
     */
    val MIGRATION_3_7 = object : Migration(3, 7) {
        override fun migrate(database: SupportSQLiteDatabase) {
            MIGRATION_3_4.migrate(database)
            MIGRATION_4_5.migrate(database)
            MIGRATION_5_6.migrate(database)
            MIGRATION_6_7.migrate(database)
        }
    }

    /**
     * Migration from version 2 to 7 (skip versions 3, 4, 5, 6).
     */
    val MIGRATION_2_7 = object : Migration(2, 7) {
        override fun migrate(database: SupportSQLiteDatabase) {
            MIGRATION_2_3.migrate(database)
            MIGRATION_3_4.migrate(database)
            MIGRATION_4_5.migrate(database)
            MIGRATION_5_6.migrate(database)
            MIGRATION_6_7.migrate(database)
        }
    }

    /**
     * Migration from version 1 to 7 (skip versions 2, 3, 4, 5, 6).
     */
    val MIGRATION_1_7 = object : Migration(1, 7) {
        override fun migrate(database: SupportSQLiteDatabase) {
            MIGRATION_1_2.migrate(database)
            MIGRATION_2_3.migrate(database)
            MIGRATION_3_4.migrate(database)
            MIGRATION_4_5.migrate(database)
            MIGRATION_5_6.migrate(database)
            MIGRATION_6_7.migrate(database)
        }
    }

    /**
     * Get all migrations for the database builder.
     */
    fun getAllMigrations(): Array<Migration> {
        return arrayOf(
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_1_3,
            MIGRATION_3_4,
            MIGRATION_1_4,
            MIGRATION_2_4,
            MIGRATION_4_5,
            MIGRATION_3_5,
            MIGRATION_2_5,
            MIGRATION_1_5,
            MIGRATION_5_6,
            MIGRATION_4_6,
            MIGRATION_3_6,
            MIGRATION_2_6,
            MIGRATION_1_6,
            MIGRATION_6_7,
            MIGRATION_5_7,
            MIGRATION_4_7,
            MIGRATION_3_7,
            MIGRATION_2_7,
            MIGRATION_1_7
        )
    }
}
