package com.budspro.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A user created Collection ("Biology", "Math Games", ...).
 *
 * Purely additive: the older `folders` table and everything that uses it is
 * untouched. Collections live in their own `collections` table added by
 * MIGRATION_3_4.
 *
 * The Kotlin class is called [CollectionItem] (not `Collection`) so it never
 * shadows `kotlin.collections.Collection` anywhere in the code base. The Room
 * table name is still `collections`, exactly as specified.
 */
@Entity(tableName = "collections")
data class CollectionItem(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long,
    val coverImagePath: String? = null
)
