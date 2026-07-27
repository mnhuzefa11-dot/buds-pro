package com.budspro.app

import android.os.Bundle
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.File

class ImageViewerActivity : ComponentActivity() {
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
