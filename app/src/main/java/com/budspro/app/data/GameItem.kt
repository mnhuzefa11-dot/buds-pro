package com.budspro.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "games")
data class GameItem(
    @PrimaryKey val id: String,
    val title: String,
    val type: String,       // "html", "pdf", "json"
    val fileName: String,   // actual filename inside filesDir/games/
    val fileSize: Long,
    val addedAt: Long,
    val lastPlayedAt: Long? = null,
    val progress: Int = 0,
    val isFavorite: Boolean = false
)
