package com.budspro.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.budspro.app.data.AppDatabase
import com.budspro.app.data.GameItem
import com.budspro.app.data.StudyAnnotation
import com.budspro.app.ui.components.ZoomableImage
import com.budspro.app.ui.components.rememberZoomState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

/**
 * Study viewer: a zoomable image with pinned notes.
 *
 * Fixes in this revision (no behaviour was removed):
 *  - the image can be pinch-zoomed, double-tap zoomed and panned;
 *  - a single touch no longer pops the "add note" dialog by accident —
 *    notes are placed deliberately via "Add note" mode or by tapping an
 *    existing marker to read it;
 *  - markers are anchored to the image itself, so they stay on the right
 *    spot while you zoom and pan instead of drifting away.
 */
@Composable
fun StudyViewerScreen(
    imagePath: String,
    gameId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val annotationDao = AppDatabase.getInstance(context.applicationContext).studyAnnotationDao()
    val annotations by annotationDao.getByGameId(gameId).collectAsState(initial = emptyList())
    val density = LocalDensity.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    val zoom = rememberZoomState(maxScale = 8f)
    var placingNote by remember { mutableStateOf(false) }
    var notesVisible by remember { mutableStateOf(true) }
    var pendingPoint by remember { mutableStateOf<Offset?>(null) }
    var newAnnotationText by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<StudyAnnotation?>(null) }

    val statusPadding = WindowInsets.statusBars.asPaddingValues()
    val navPadding = WindowInsets.navigationBars.asPaddingValues()

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        ZoomableImage(
            model = imagePath,
            contentDescription = "Study image",
            modifier = Modifier.fillMaxSize(),
            state = zoom,
            onTap = { point ->
                if (placingNote) {
                    val ratio = zoom.containerToImageRatio(point)
                    if (ratio != null) {
                        pendingPoint = ratio
                        placingNote = false
                    }
                }
            }
        ) {
            if (notesVisible) {
                annotations.forEach { ann ->
                    val pos = zoom.imageRatioToContainer(ann.xRatio, ann.yRatio)
                    val markerPx = with(density) { 22.dp.toPx() }
                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    (pos.x - markerPx / 2f).toInt(),
                                    (pos.y - markerPx / 2f).toInt()
                                )
                            }
                            .size(22.dp)
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.primary)
                            .border(2.dp, Color.White, RoundedCornerShape(50))
                            .clickable { selected = ann }
                    )
                }
            }
        }

        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(Brush.verticalGradient(listOf(Color(0xCC000000), Color.Transparent)))
                .padding(top = statusPadding.calculateTopPadding())
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                text = if (annotations.isEmpty()) "Study" else "${annotations.size} notes",
                color = Color.White,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            IconButton(onClick = { notesVisible = !notesVisible }) {
                Icon(
                    if (notesVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                    contentDescription = if (notesVisible) "Hide notes" else "Show notes",
                    tint = Color.White
                )
            }
        }

        // Hint shown while waiting for the user to pick a spot.
        AnimatedVisibility(
            visible = placingNote,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xE6000000)
            ) {
                Text(
                    text = "Tap the image to place your note",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }

        ExtendedFloatingActionButton(
            onClick = {
                if (placingNote) {
                    placingNote = false
                } else {
                    placingNote = true
                }
            },
            text = { Text(if (placingNote) "Cancel" else "Add note") },
            icon = {
                Icon(
                    if (placingNote) Icons.Filled.Add else Icons.Filled.NoteAdd,
                    contentDescription = null
                )
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp)
                .padding(bottom = navPadding.calculateBottomPadding() + 24.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    }

    // New note dialog — only after the user chose a point.
    pendingPoint?.let { point ->
        AlertDialog(
            onDismissRequest = { pendingPoint = null; newAnnotationText = "" },
            title = { Text("New note") },
            text = {
                OutlinedTextField(
                    value = newAnnotationText,
                    onValueChange = { newAnnotationText = it },
                    label = { Text("Note text") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    enabled = newAnnotationText.isNotBlank(),
                    onClick = {
                        val ann = StudyAnnotation(
                            id = UUID.randomUUID().toString(),
                            gameId = gameId,
                            text = newAnnotationText.trim(),
                            xRatio = point.x,
                            yRatio = point.y,
                            createdAt = System.currentTimeMillis()
                        )
                        CoroutineScope(Dispatchers.IO).launch { annotationDao.insert(ann) }
                        pendingPoint = null
                        newAnnotationText = ""
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { pendingPoint = null; newAnnotationText = "" }) { Text("Cancel") }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Reading / deleting an existing note.
    selected?.let { ann ->
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text("Note") },
            text = { Text(ann.text) },
            confirmButton = {
                TextButton(onClick = { selected = null }) { Text("Close") }
            },
            dismissButton = {
                TextButton(onClick = {
                    scope.launch(Dispatchers.IO) { annotationDao.deleteById(ann.id) }
                    selected = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
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
