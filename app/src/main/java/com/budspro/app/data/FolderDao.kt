package com.budspro.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders ORDER BY createdAt DESC")
    fun getAll(): Flow<List<Folder>>

    @Insert
    suspend fun insert(folder: Folder)

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM folders WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): Folder?

    @Query("UPDATE folders SET name = :name WHERE id = :id")
    suspend fun updateName(id: String, name: String)
}
