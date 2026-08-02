package com.budspro.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.budspro.app.ui.theme.BudsProTheme
import java.io.File

class StudyViewerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val gameId = intent.getStringExtra("gameId") ?: run { finish(); return }
        val fileName = intent.getStringExtra("fileName") ?: run { finish(); return }
        val file = File(filesDir, "games" + File.separator + fileName)
        if (!file.exists()) {
            Toast.makeText(this, "That file is missing from your library", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContent {
            BudsProTheme {
                StudyViewerScreen(
                    imagePath = file.absolutePath,
                    gameId = gameId,
                    onBack = { finish() }
                )
            }
        }
    }
}
