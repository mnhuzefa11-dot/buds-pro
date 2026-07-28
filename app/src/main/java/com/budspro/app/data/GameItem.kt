package com.budspro.app.data

import androidx.room.ColumnInfo
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
    val isFavorite: Boolean = false,
    val coverPath: String? = null,
    val folderId: String? = null,
    val tags: String? = null,

    // ---- Added in schema v4 (additive only, all nullable / defaulted) ----
    /** Absolute path of a user picked cover image. */
    val coverImagePath: String? = null,
    /** Id of the Collection this item belongs to, or null. */
    val collectionId: String? = null,
    /** Accumulated time the item has been open, in milliseconds. */
    @ColumnInfo(defaultValue = "0")
    val totalPlayTime: Long = 0L
)

/**
 * The cover to actually render.
 *
 * Prefers the new `coverImagePath` column but transparently falls back to the
 * original `coverPath`, so items that already had a cover before this update
 * keep showing it.
 *
 * Declared as an extension property (not a member) so Room's annotation
 * processor never has to reason about it.
 */
val GameItem.effectiveCover: String?
    get() = coverImagePath?.takeIf { it.isNotBlank() } ?: coverPath?.takeIf { it.isNotBlank() }
