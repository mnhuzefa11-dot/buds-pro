package com.budspro.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_annotations")
data class StudyAnnotation(
    @PrimaryKey val id: String,
    val gameId: String,
    val text: String,
    val xRatio: Float,
    val yRatio: Float,
    val createdAt: Long
)
