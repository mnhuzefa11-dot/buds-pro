package com.budspro.app

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.content.FileProvider
import androidx.webkit.WebViewAssetLoader
import com.budspro.app.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class PlayerActivity : ComponentActivity() {

    /**
     * Play-time tracking (added in v2).
     *
     * Implemented purely in onResume/onPause so the original onCreate logic —
     * including the WebViewAssetLoader setup that makes localStorage persist —
     * is completely untouched.
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
        // Ignore absurd values (e.g. device slept for hours on this screen).
        if (delta <= 0L || delta > 6L * 60L * 60L * 1000L) return
        val dao = AppDatabase.getInstance(applicationContext).gameDao()
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { dao.addPlayTime(id, delta) }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val gameId = intent.getStringExtra("gameId")
        if (gameId == null) { finish(); return }

        val dao = AppDatabase.getInstance(applicationContext).gameDao()

        CoroutineScope(Dispatchers.IO).launch {
            val item = dao.getById(gameId) ?: run { finish(); return@launch }
            val gamesDir = File(filesDir, "games")
            val file = File(gamesDir, item.fileName)

            when (item.type) {
                "pdf" -> {
                    // Hand off to the system's own PDF viewer — reliable,
                    // no custom renderer needed for this MVP.
                    runOnUiThread {
                        val uri = FileProvider.getUriForFile(
                            this@PlayerActivity, "$packageName.fileprovider", file
                        )
                        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, "application/pdf")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        startActivity(viewIntent)
                        finish()
                    }
                }
                "json" -> {
                    // MVP viewer — proper flashcard/Sets rendering comes next.
                    val text = file.readText()
                    runOnUiThread { showJsonViewer(text) }
                }
                else -> {
                    // HTML game — served over a real local origin so that
                    // localStorage is a genuine OS-backed file on disk.
                    // It survives app restarts automatically. No shims needed.
                    val assetLoader = WebViewAssetLoader.Builder()
                        .setDomain("appassets.androidplatform.net")
                        .addPathHandler(
                            "/games/",
                            WebViewAssetLoader.InternalStoragePathHandler(this@PlayerActivity, gamesDir)
                        )
                        .build()

                    runOnUiThread {
                        val webView = WebView(this@PlayerActivity)
                        setContentView(webView)

                        webView.settings.javaScriptEnabled = true
                        webView.settings.domStorageEnabled = true
                        webView.settings.databaseEnabled = true
                        webView.settings.cacheMode = WebSettings.LOAD_DEFAULT
                        webView.settings.allowFileAccess = false
                        webView.settings.allowContentAccess = false

                        val prefs = getSharedPreferences("buds_scroll", MODE_PRIVATE)
                        val savedScroll = prefs.getInt(gameId, 0)
                        val scrollClient = object : WebViewClient() {
                            override fun shouldInterceptRequest(
                                view: WebView,
                                request: WebResourceRequest
                            ) = assetLoader.shouldInterceptRequest(request.url)

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                val currentY = view?.scrollY ?: 0
                                prefs.edit().putInt(gameId, currentY).apply()
                            }
                        }
                        webView.webViewClient = scrollClient

                        webView.loadUrl("https://appassets.androidplatform.net/games/${item.fileName}")
                        if (savedScroll > 0) {
                            webView.postDelayed({
                                webView.evaluateJavascript("window.scrollTo(0, $savedScroll)") { }
                            }, 800)
                        }

                        CoroutineScope(Dispatchers.IO).launch {
                            dao.updateProgress(item.id, item.progress, System.currentTimeMillis())
                        }
                    }
                }
            }
        }
    }

    private fun showJsonViewer(text: String) {
        val tv = TextView(this)
        tv.text = text
        tv.setPadding(32, 32, 32, 32)
        tv.textSize = 14f
        // Cosmetic only (the app theme is now dark): pin the colours so the
        // JSON is always light-on-dark and never unreadable. Viewer logic
        // itself is unchanged.
        tv.setTextColor(0xFFF3F0FA.toInt())
        val scroll = ScrollView(this)
        scroll.setBackgroundColor(0xFF0B0710.toInt())
        scroll.addView(tv)
        setContentView(scroll)
    }
}
