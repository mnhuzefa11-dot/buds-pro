package com.budspro.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import com.budspro.app.data.AppDatabase
import com.budspro.app.ui.components.ZoomableImage
import com.budspro.app.ui.components.rememberZoomState
import com.budspro.app.ui.theme.BudsProTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

/**
 * Full screen image viewer.
 *
 * Behaviour is the same as before (open an image file from the library), with
 * the long-standing bug fixed: the image can now be pinch-zoomed, double-tap
 * zoomed and panned. Play-time tracking via the optional "gameId" extra is
 * untouched.
 */
class ImageViewerActivity : ComponentActivity() {

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
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val imagePath = intent.getStringExtra("imagePath") ?: run { finish(); return }
        val file = File(imagePath)
        if (!file.exists()) { finish(); return }

        setContent {
            BudsProTheme {
                ImageViewerContent(
                    file = file,
                    title = intent.getStringExtra("title") ?: file.name,
                    onBack = { finish() },
                    onShare = { shareImage(file) }
                )
            }
        }
    }

    private fun shareImage(file: File) {
        runCatching {
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(send, "Share image"))
        }
    }
}

@Composable
private fun ImageViewerContent(
    file: File,
    title: String,
    onBack: () -> Unit,
    onShare: () -> Unit
) {
    val zoom = rememberZoomState()
    var chromeVisible by remember { mutableStateOf(true) }
    val statusPadding = WindowInsets.statusBars.asPaddingValues()

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0B0710))) {
        ZoomableImage(
            model = file,
            contentDescription = title,
            modifier = Modifier.fillMaxSize(),
            state = zoom,
            onTap = { chromeVisible = !chromeVisible }
        )

        AnimatedVisibility(
            visible = chromeVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xCC000000), Color.Transparent)
                        )
                    )
                    .padding(top = statusPadding.calculateTopPadding())
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Text(
                    text = title,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )
                IconButton(onClick = {
                    if (zoom.isZoomed) zoom.reset()
                    else zoom.onGesture(
                        centroid = androidx.compose.ui.geometry.Offset(
                            zoom.containerSize.width / 2f,
                            zoom.containerSize.height / 2f
                        ),
                        pan = androidx.compose.ui.geometry.Offset.Zero,
                        zoom = 2f
                    )
                }) {
                    Icon(
                        if (zoom.isZoomed) Icons.Filled.ZoomOutMap else Icons.Filled.ZoomIn,
                        contentDescription = if (zoom.isZoomed) "Reset zoom" else "Zoom in",
                        tint = Color.White
                    )
                }
                IconButton(onClick = onShare) {
                    Icon(Icons.Filled.Share, contentDescription = "Share", tint = Color.White)
                }
            }
        }
    }
}
