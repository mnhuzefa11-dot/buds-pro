package com.budspro.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import java.io.File

class StudyViewerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val gameId = intent.getStringExtra("gameId") ?: run { finish(); return }
        val fileName = intent.getStringExtra("fileName") ?: run { finish(); return }
        val imagePath = File(filesDir, "games" + File.separator + fileName).absolutePath
        setContent {
            StudyViewerScreen(
                imagePath = imagePath,
                gameId = gameId,
                onBack = { finish() }
            )
        }
    }
}
