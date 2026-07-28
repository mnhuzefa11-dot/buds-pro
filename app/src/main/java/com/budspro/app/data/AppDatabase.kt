package com.budspro.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

import com.budspro.app.data.MIGRATION_1_2
import com.budspro.app.data.MIGRATION_1_4
import com.budspro.app.data.MIGRATION_2_3
import com.budspro.app.data.MIGRATION_3_4

@Database(
    entities = [GameItem::class, Folder::class, StudyAnnotation::class, CollectionItem::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
    abstract fun folderDao(): FolderDao
    abstract fun studyAnnotationDao(): StudyAnnotationDao
    abstract fun collectionDao(): CollectionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "buds_pro.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_1_4)
                    .build().also { INSTANCE = it }
            }
        }

        /** Used by the backup/restore flow to force a reload after a restore. */
        fun closeAndClear() {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
            }
        }
    }
}
