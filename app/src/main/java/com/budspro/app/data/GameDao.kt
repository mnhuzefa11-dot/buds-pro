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

    /**
     * Stamps the "last opened" time so the Recent tab has something to show.
     * Additive only: no schema change (lastPlayedAt already exists), so
     * previously imported items keep working exactly as before.
     */
    @Query("UPDATE games SET lastPlayedAt = :ts WHERE id = :id")
    suspend fun updateLastPlayed(id: String, ts: Long)
}
