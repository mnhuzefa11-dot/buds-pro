package com.budspro.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM games ORDER BY addedAt DESC")
    fun getAll(): Flow<List<GameItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: GameItem)

    @Update
    suspend fun update(item: GameItem)

    @Query("DELETE FROM games WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM games WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): GameItem?

    @Query("UPDATE games SET progress = :progress, lastPlayedAt = :ts WHERE id = :id")
    suspend fun updateProgress(id: String, progress: Int, ts: Long)

    @Query("UPDATE games SET isFavorite = :fav WHERE id = :id")
    suspend fun updateFavorite(id: String, fav: Boolean)

    @Query("UPDATE games SET coverPath = :path WHERE id = :id")
    suspend fun updateCover(id: String, path: String?)

    @Query("UPDATE games SET folderId = :folderId WHERE id = :id")
    suspend fun updateFolder(id: String, folderId: String?)

    @Query("UPDATE games SET title = :name WHERE id = :id")
    suspend fun updateTitle(id: String, name: String)

    @Query("UPDATE games SET tags = :tags WHERE id = :id")
    suspend fun updateTags(id: String, tags: String?)

    @Query("SELECT * FROM games WHERE folderId = :folderId")
    suspend fun getItemsInFolder(folderId: String): List<GameItem>

    @Query("SELECT * FROM games WHERE folderId = :folderId ORDER BY addedAt DESC")
    fun getByFolder(folderId: String): Flow<List<GameItem>>

    @Query("SELECT * FROM games WHERE folderId IS NULL ORDER BY addedAt DESC")
    fun getRoot(): Flow<List<GameItem>>

    /**
     * Stamps the "last opened" time so the Recent tab has something to show.
     * Additive only: no schema change (lastPlayedAt already exists), so
     * previously imported items keep working exactly as before.
     */
    @Query("UPDATE games SET lastPlayedAt = :ts WHERE id = :id")
    suspend fun updateLastPlayed(id: String, ts: Long)

    // ------------------------------------------------------------------
    // Added for Collections / new cover column / play-time tracking.
    // All additive: no existing query above was changed.
    // ------------------------------------------------------------------

    @Query("SELECT * FROM games ORDER BY addedAt DESC")
    suspend fun getAllOnce(): List<GameItem>

    @Query("UPDATE games SET coverImagePath = :path WHERE id = :id")
    suspend fun updateCoverImagePath(id: String, path: String?)

    @Query("UPDATE games SET collectionId = :collectionId WHERE id = :id")
    suspend fun updateCollection(id: String, collectionId: String?)

    @Query("SELECT * FROM games WHERE collectionId = :collectionId ORDER BY addedAt DESC")
    fun getByCollection(collectionId: String): Flow<List<GameItem>>

    @Query("SELECT * FROM games WHERE collectionId = :collectionId ORDER BY addedAt DESC")
    suspend fun getItemsInCollection(collectionId: String): List<GameItem>

    @Query("UPDATE games SET collectionId = NULL WHERE collectionId = :collectionId")
    suspend fun clearCollection(collectionId: String)

    @Query("UPDATE games SET totalPlayTime = totalPlayTime + :deltaMs WHERE id = :id")
    suspend fun addPlayTime(id: String, deltaMs: Long)
}
