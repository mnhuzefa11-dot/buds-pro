package com.budspro.app

import android.os.Bundle
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.budspro.app.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class ImageViewerActivity : ComponentActivity() {

    /**
     * Play-time tracking (added in v2). Additive only — the original image
     * loading below is unchanged. The optional "gameId" extra is used purely
     * for the stats; when it is absent the viewer behaves exactly as before.
     */
    private var resumedAtMs: Long = 0L

    override fun onResume() {
        super.onResume()
        resumedAtMs = System.currentTimeMillis()
    }

    override fun onPause() {
        super.onPause()
        val startedAt = resumedAtMs
        resumedAtMs = 0L
        val id = intent.getStringExtra("gameId") ?: return
        if (startedAt <= 0L) return
        val delta = System.currentTimeMillis() - startedAt
        if (delta <= 0L || delta > 6L * 60L * 60L * 1000L) return
        val dao = AppDatabase.getInstance(applicationContext).gameDao()
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { dao.addPlayTime(id, delta) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val imagePath = intent.getStringExtra("imagePath") ?: run { finish(); return }
        val imageView = ImageView(this)
        imageView.setImageURI(android.net.Uri.fromFile(File(imagePath)))
        imageView.scaleType = ImageView.ScaleType.FIT_CENTER
        imageView.setBackgroundColor(0xFF0B0710.toInt())
        setContentView(imageView)
    }
}
