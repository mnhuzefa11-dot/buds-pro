package com.budspro.app.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE games ADD COLUMN tags TEXT DEFAULT NULL")
        db.execSQL("CREATE TABLE IF NOT EXISTS study_annotations (id TEXT NOT NULL PRIMARY KEY, gameId TEXT NOT NULL, text TEXT NOT NULL, xRatio REAL NOT NULL, yRatio REAL NOT NULL, createdAt INTEGER NOT NULL)")
    }
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE games ADD COLUMN coverPath TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE games ADD COLUMN folderId TEXT DEFAULT NULL")
        db.execSQL("CREATE TABLE IF NOT EXISTS folders (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, createdAt INTEGER NOT NULL)")
    }
}
