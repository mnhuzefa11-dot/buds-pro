package com.budspro.app

import android.app.WallpaperManager
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.budspro.app.data.AppDatabase
import com.budspro.app.data.CollectionItem
import com.budspro.app.data.GameItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

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
        val imagePath = intent.getStringExtra("imagePath") ?: run { finish(); return }
        val gameId = intent.getStringExtra("gameId")

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Color(0xFF0B0710),
                    surface = Color(0xFF1B1622),
                    onBackground = Color.White,
                    onSurface = Color.White
                )
            ) {
                ImageViewerScreen(
                    imagePath = imagePath,
                    gameId = gameId,
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageViewerScreen(
    imagePath: String,
    gameId: String?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context.applicationContext) }
    val scope = rememberCoroutineScope()

    var gameItem by remember { mutableStateOf<GameItem?>(null) }
    var collections by remember { mutableStateOf<List<CollectionItem>>(emptyList()) }
    var showCollectionPicker by remember { mutableStateOf(false) }

    LaunchedEffect(gameId) {
        if (gameId != null) {
            gameItem = db.gameDao().getById(gameId)
        }
        db.collectionDao().getAll().collect {
            collections = it
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = gameItem?.title ?: "Image Viewer",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    if (gameId != null) {
                        IconButton(onClick = { showCollectionPicker = true }) {
                            Icon(
                                imageVector = Icons.Filled.Folder,
                                contentDescription = "Add to Collection",
                                tint = if (gameItem?.collectionId != null) MaterialTheme.colorScheme.primary else Color.White
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            val file = File(imagePath)
                            if (file.exists()) {
                                setAsWallpaper(context, file)
                            } else {
                                Toast.makeText(context, "Image file not found", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Wallpaper,
                            contentDescription = "Set as Wallpaper",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.5f),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF0B0710)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            ZoomableImage(file = File(imagePath))
        }

        if (showCollectionPicker) {
            LocalCollectionPickerDialog(
                collections = collections,
                currentCollectionId = gameItem?.collectionId,
                onDismiss = { showCollectionPicker = false },
                onSelect = { selectedCollectionId ->
                    scope.launch(Dispatchers.IO) {
                        if (gameId != null) {
                            db.gameDao().updateCollection(gameId, selectedCollectionId)
                            gameItem = db.gameDao().getById(gameId)
                            withContext(Dispatchers.Main) {
                                val msg = if (selectedCollectionId == null) "Removed from collection" else "Added to collection"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                onCreateNewCollection = { name ->
                    scope.launch(Dispatchers.IO) {
                        val clean = name.trim()
                        if (clean.isNotEmpty()) {
                            val newId = UUID.randomUUID().toString()
                            db.collectionDao().insert(
                                CollectionItem(id = newId, name = clean, createdAt = System.currentTimeMillis())
                            )
                            if (gameId != null) {
                                db.gameDao().updateCollection(gameId, newId)
                                gameItem = db.gameDao().getById(gameId)
                            }
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Created & added to collection \"$clean\"", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun ZoomableImage(
    file: File,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        offset = if (scale == 1f) Offset.Zero else offset + offsetChange
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 3f
                        }
                    }
                )
            }
            .transformable(state = transformState),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = file,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
        )
    }
}

@Composable
fun LocalCollectionPickerDialog(
    collections: List<CollectionItem>,
    currentCollectionId: String?,
    onDismiss: () -> Unit,
    onSelect: (String?) -> Unit,
    onCreateNewCollection: (String) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("New collection") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true,
                    label = { Text("Collection name") }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onCreateNewCollection(newName)
                        showCreateDialog = false
                    },
                    enabled = newName.isNotBlank()
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Add to Collection") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(null); onDismiss() }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = currentCollectionId == null, onClick = { onSelect(null); onDismiss() })
                        Spacer(Modifier.width(12.dp))
                        Text("No collection")
                    }

                    collections.forEach { c ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(c.id); onDismiss() }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = currentCollectionId == c.id, onClick = { onSelect(c.id); onDismiss() })
                            Spacer(Modifier.width(12.dp))
                            Text(c.name)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCreateDialog = true }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "New collection",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        )
    }
}

fun setAsWallpaper(context: Context, file: File) {
    try {
        val wallpaperManager = WallpaperManager.getInstance(context)
        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
        if (bitmap != null) {
            wallpaperManager.setBitmap(bitmap)
            Toast.makeText(context, "Wallpaper set successfully!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Failed to decode image", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Error setting wallpaper: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}