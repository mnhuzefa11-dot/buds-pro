package com.budspro.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * DAO for the new `collections` table. Additive only — no existing DAO was
 * modified in a breaking way.
 */
@Dao
interface CollectionDao {

    @Query("SELECT * FROM collections ORDER BY createdAt DESC")
    fun getAll(): Flow<List<CollectionItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(collection: CollectionItem)

    @Query("SELECT * FROM collections WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): CollectionItem?

    @Query("SELECT * FROM collections ORDER BY createdAt DESC")
    suspend fun getAllOnce(): List<CollectionItem>

    @Query("UPDATE collections SET name = :name WHERE id = :id")
    suspend fun updateName(id: String, name: String)

    @Query("UPDATE collections SET coverImagePath = :path WHERE id = :id")
    suspend fun updateCover(id: String, path: String?)

    @Query("DELETE FROM collections WHERE id = :id")
    suspend fun deleteById(id: String)
}
