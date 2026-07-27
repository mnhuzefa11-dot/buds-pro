package com.budspro.app

import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pointer.pointerInput
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.PaddingValues
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.budspro.app.data.AppDatabase
import com.budspro.app.data.GameItem
import com.budspro.app.data.StudyAnnotation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

@Composable
fun StudyViewerScreen(
    imagePath: String,
    gameId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val annotationDao = AppDatabase.getInstance(context.applicationContext).studyAnnotationDao()
    val annotations by annotationDao.getByGameId(gameId).collectAsState(initial = emptyList())

    var scale by remember { mutableStateOf(1f) }
    var showAddDialog by remember { mutableStateOf(false) }
    var newAnnotationText by remember { mutableStateOf("") }
    var tapPosition by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Image layer with tap detection and zoom
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.changes.size == 1 && event.changes[0].pressed) {
                                tapPosition = event.changes[0].position
                                showAddDialog = true
                            }
                        }
                    }
                }
        ) {
            AsyncImage(
                model = imagePath,
                contentDescription = "Study image",
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale
                    ),
                contentScale = ContentScale.Fit
            )

            // Annotation bubbles
            annotations.forEach { ann ->
                val xPos = (ann.xRatio * 300).dp
                val yPos = (ann.yRatio * 400).dp
                Box(
                    modifier = Modifier
                        .padding(top = yPos, start = xPos)
                        .size(12.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFFFF5722))
                )
            }
        }

        // Top controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 12.dp, end = 12.dp)
                .align(Alignment.TopCenter),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { scale = (scale * 1.2f).coerceAtMost(5f) }) {
                Icon(Icons.Filled.ZoomIn, contentDescription = "Zoom in", tint = Color.White)
            }
            IconButton(onClick = { scale = (scale / 1.2f).coerceAtLeast(0.5f) }) {
                Icon(Icons.Filled.ZoomOut, contentDescription = "Zoom out", tint = Color.White)
            }
        }

        // Add note button
        ExtendedFloatingActionButton(
            onClick = {
                tapPosition = androidx.compose.ui.geometry.Offset(200f, 300f)
                showAddDialog = true
            },
            text = { Text("Add Note") },
            icon = { Icon(Icons.Filled.Add, contentDescription = "Add note") },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false; newAnnotationText = "" },
            title = { Text("Study Note") },
            text = {
                TextField(
                    value = newAnnotationText,
                    onValueChange = { newAnnotationText = it },
                    label = { Text("Note text") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newAnnotationText.isNotBlank()) {
                        val ann = StudyAnnotation(
                            id = UUID.randomUUID().toString(),
                            gameId = gameId,
                            text = newAnnotationText,
                            xRatio = tapPosition.x / 300f,
                            yRatio = tapPosition.y / 400f,
                            createdAt = System.currentTimeMillis()
                        )
                        CoroutineScope(Dispatchers.IO).launch {
                            annotationDao.insert(ann)
                        }
                    }
                    showAddDialog = false
                    newAnnotationText = ""
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false; newAnnotationText = "" }) { Text("Cancel") }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyTabContent(
    games: List<GameItem>,
    onOpenStudy: (GameItem) -> Unit
) {
    val imageGames = games.filter { it.type == "image" }
    var studySearchQuery by rememberSaveable { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        SearchBar(
            query = studySearchQuery,
            onQueryChange = { studySearchQuery = it },
            onSearch = {},
            active = false,
            onActiveChange = {},
            placeholder = { Text("Search study images & notes") },
            colors = SearchBarDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
        ) {}

        val filtered = imageGames.filter {
            it.title.contains(studySearchQuery, ignoreCase = true) ||
                (it.tags?.contains(studySearchQuery, ignoreCase = true) == true)
        }

        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No study images", style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Import images and tag them to organize study content.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
            ) {
                items(filtered.size, key = { filtered[it].id }) { index ->
                    StudyCard(
                        item = filtered[index],
                        onOpen = { onOpenStudy(filtered[index]) }
                    )
                }
            }
        }
    }
}

@Composable
private fun StudyCard(
    item: GameItem,
    onOpen: () -> Unit
) {
    val accent = com.budspro.app.ui.theme.colorForType(item.type)
    val context = LocalContext.current
    val filePath = File(context.filesDir, "games/${item.fileName}").absolutePath

    Card(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth().height(196.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier.fillMaxWidth().height(80.dp).background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = filePath,
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Simple tag badge
                if (item.type == "image") {
                    Box(
                        modifier = Modifier.padding(start = 12.dp, top = 8.dp).background(Color(0xFF22D3EE).copy(alpha = 0.9f)).padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(text = item.type.uppercase(), style = MaterialTheme.typography.labelSmall, color = Color.Black, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    }
                }
            }
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth()) {
                Text(text = item.title, style = MaterialTheme.typography.titleSmall, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(4.dp))
                if (!item.tags.isNullOrBlank()) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        item.tags!!.split(",").map { it.trim() }.filter { it.isNotBlank() }.take(3).forEach { tag ->
                            Box(
                                modifier = Modifier.padding(end = 4.dp, bottom = 2.dp).background(MaterialTheme.colorScheme.primaryContainer).padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(text = tag, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = "Study image · Tap to annotate", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
