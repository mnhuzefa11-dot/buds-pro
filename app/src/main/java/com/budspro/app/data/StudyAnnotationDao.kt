package com.budspro.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyAnnotationDao {
    @Query("SELECT * FROM study_annotations WHERE gameId = :gameId ORDER BY createdAt DESC")
    fun getByGameId(gameId: String): Flow<List<StudyAnnotation>>

    @Insert
    suspend fun insert(annotation: StudyAnnotation)

    @Query("DELETE FROM study_annotations WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM study_annotations WHERE gameId = :gameId")
    suspend fun deleteByGameId(gameId: String)

    // Added for library backup/restore (additive only).

    @Query("SELECT * FROM study_annotations")
    suspend fun getAllOnce(): List<StudyAnnotation>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(annotation: StudyAnnotation)

}
