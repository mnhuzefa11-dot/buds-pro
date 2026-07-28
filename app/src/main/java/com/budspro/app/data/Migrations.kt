package com.budspro.app.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration history for buds_pro.db.
 *
 * IMPORTANT: MIGRATION_1_2 and MIGRATION_2_3 below are the original, already
 * shipped migrations. They are left exactly as they were so that databases in
 * the wild keep upgrading correctly. Never edit a migration that has shipped.
 *
 * The new columns requested for this release (coverImagePath, collectionId,
 * totalPlayTime) plus the new `collections` table are therefore added in a new
 * MIGRATION_3_4, because the shipped schema had already moved past version 1.
 */

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE games ADD COLUMN coverPath TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE games ADD COLUMN folderId TEXT DEFAULT NULL")
        db.execSQL("CREATE TABLE IF NOT EXISTS folders (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, createdAt INTEGER NOT NULL)")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE games ADD COLUMN tags TEXT DEFAULT NULL")
        db.execSQL("CREATE TABLE IF NOT EXISTS study_annotations (id TEXT NOT NULL PRIMARY KEY, gameId TEXT NOT NULL, text TEXT NOT NULL, xRatio REAL NOT NULL, yRatio REAL NOT NULL, createdAt INTEGER NOT NULL)")
    }
}

/**
 * v3 -> v4
 *  - games.coverImagePath TEXT
 *  - games.collectionId  TEXT
 *  - games.totalPlayTime INTEGER NOT NULL DEFAULT 0
 *  - new `collections` table (id, name, createdAt, coverImagePath)
 *
 * Existing rows are preserved; nothing is dropped or rewritten.
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE games ADD COLUMN coverImagePath TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE games ADD COLUMN collectionId TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE games ADD COLUMN totalPlayTime INTEGER NOT NULL DEFAULT 0")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS collections (" +
                "id TEXT NOT NULL PRIMARY KEY, " +
                "name TEXT NOT NULL, " +
                "createdAt INTEGER NOT NULL, " +
                "coverImagePath TEXT DEFAULT NULL)"
        )
        // Carry any cover already set on an item over to the new column so
        // nothing visually regresses after the update.
        db.execSQL("UPDATE games SET coverImagePath = coverPath WHERE coverPath IS NOT NULL")
    }
}

/**
 * Safety net for the (rare) install that is still on a pre-`folders` schema.
 * Room applies the shortest valid path, so this is only used when a direct
 * 1 -> 4 jump is the only option available.
 */
val MIGRATION_1_4 = object : Migration(1, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        MIGRATION_1_2.migrate(db)
        MIGRATION_2_3.migrate(db)
        MIGRATION_3_4.migrate(db)
    }
}
