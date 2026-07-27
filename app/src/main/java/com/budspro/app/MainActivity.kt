package com.budspro.app

import android.content.Intent
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.budspro.app.data.Folder
import com.budspro.app.data.GameItem
import com.budspro.app.ui.GameViewModel
import com.budspro.app.ui.theme.BudsProTheme
import com.budspro.app.ui.theme.colorForType
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val viewModel: GameViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BudsProTheme {
                BudsProApp(viewModel) { item ->
                    viewModel.markOpened(item)
                    if (item.type == "image") {
                        val intent = Intent(this, ImageViewerActivity::class.java)
                        intent.putExtra("imagePath", File(filesDir, "games/${item.fileName}").absolutePath)
                        startActivity(intent)
                    } else {
                        val intent = Intent(this, PlayerActivity::class.java)
                        intent.putExtra("gameId", item.id)
                        startActivity(intent)
                    }
                }
            }
        }
    }
}

private enum class BudsTab(val label: String, val icon: ImageVector) {
    LIBRARY("Library", Icons.Filled.List),
    SAVES("Saves", Icons.Filled.Favorite),
    RECENT("Recent", Icons.Filled.Refresh),
    STUDY("Study", Icons.Filled.Search),
    SETTINGS("Settings", Icons.Filled.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudsProApp(viewModel: GameViewModel, onOpen: (GameItem) -> Unit) {
    val games by viewModel.games.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val recent by viewModel.recent.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val context = LocalContext.current

    var selectedTabIndex by rememberSaveable { mutableStateOf(0) }
    val tabs = BudsTab.values()
    val selectedTab = tabs[selectedTabIndex.coerceIn(0, tabs.size - 1)]
    var pendingDelete by remember { mutableStateOf<GameItem?>(null) }
    var selectedFolder by rememberSaveable { mutableStateOf<String?>(null) }

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isGrid by rememberSaveable { mutableStateOf(true) }
    var sortOption by rememberSaveable { mutableStateOf("date") }

    val pickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        uris.forEach { uri ->
            var name = "untitled"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && idx >= 0) name = cursor.getString(idx)
            }
            viewModel.importFile(uri, name)
        }
    }

    val launchImport: () -> Unit = {
        pickerLauncher.launch(arrayOf("text/html", "application/pdf", "application/json", "image/jpeg", "image/png", "image/webp", "*/*"))
    }

    val coverPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.setCoverImage(pendingCoverItemId ?: return@let, it) }
    }
    var pendingCoverItemId by remember { mutableStateOf<String?>(null) }

    val folderNameDialog = remember { mutableStateOf<String?>(null) }
    val renameFolderDialog = remember { mutableStateOf<Folder?>(null) }
    var tagEditItem by remember { mutableStateOf<GameItem?>(null) }
    var tagEditValue by remember { mutableStateOf("") }
    var progressEditItem by remember { mutableStateOf<GameItem?>(null) }
    var progressEditValue by remember { mutableStateOf(0) }

    val libraryEntries = if (selectedFolder != null) {
        games.filter { it.folderId == selectedFolder }
    } else {
        games.filter { it.folderId == null }
    }.filter {
        it.title.contains(searchQuery, ignoreCase = true)
    }.sortedWith(compareBy(
        { when (sortOption) { "name" -> it.title; else -> "" } },
        { when (sortOption) { "date" -> -it.addedAt; else -> 0L } },
        { when (sortOption) { "recent" -> -(it.lastPlayedAt ?: 0L); else -> 0L } }
    ))

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text("Buds Pro", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(subtitleFor(selectedTab, libraryEntries.size, favorites.size, recent.size), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface, titleContentColor = MaterialTheme.colorScheme.onSurface)
                )
                if (selectedTab == BudsTab.LIBRARY && selectedFolder == null) {
                    SearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        onSearch = {},
                        active = false,
                        onActiveChange = {},
                        placeholder = { Text("Search library") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        colors = SearchBarDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {}
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTabIndex = index; selectedFolder = null },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            if (selectedTab == BudsTab.LIBRARY) {
                ExtendedFloatingActionButton(
                    onClick = launchImport,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    icon = { Icon(Icons.Filled.Add, contentDescription = "Import") },
                    text = { Text("Import") }
                )
            }
        }
    ) { padding ->
        AnimatedContent(targetState = selectedTab, transitionSpec = { fadeIn() with fadeOut() }, label = "tab") { tab ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                when (tab) {
                    BudsTab.LIBRARY -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            if (selectedFolder != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 4.dp)
                                ) {
                                    IconButton(onClick = { selectedFolder = null }) { Icon(Icons.Filled.List, contentDescription = "Back") }
                                    Text("Back to Library", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                            // Folders row at top when not inside folder
                            if (selectedFolder == null) {
                                LazyRow(modifier = Modifier.padding(top = 4.dp, bottom = 4.dp), contentPadding = PaddingValues(horizontal = 12.dp)) {
                                    items(folders.size) { idx ->
                                        val f = folders[idx]
                                        Card(
                                            onClick = { selectedFolder = f.id },
                                            modifier = Modifier.padding(end = 8.dp).width(120.dp).height(56.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp)) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(text = f.name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                                    Text(text = "Folder", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                IconButton(onClick = { renameFolderDialog.value = f }) {
                                                    Text("✏", style = MaterialTheme.typography.bodySmall)
                                                }
                                            }
                                        }
                                    }
                                    item {
                                        Card(onClick = { folderNameDialog.value = "" }, modifier = Modifier.padding(end = 8.dp).width(120.dp).height(56.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                Text("+ New Folder", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                            }
                                        }
                                    }
                                }
                            }

                            // Sorting and view toggle
                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                TextButton(onClick = { sortOption = if (sortOption == "date") "name" else if (sortOption == "name") "recent" else "date" }) {
                                    Text("Sort: ${sortOption.replaceFirstChar { it.uppercase(Locale.getDefault()) }}")
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                IconButton(onClick = { isGrid = !isGrid }) {
                                    Icon(if (isGrid) Icons.Filled.ViewModule else Icons.Filled.List, contentDescription = "Toggle view")
                                }
                            }

                            if (libraryEntries.isEmpty()) {
                                EmptyState("Library is empty", "Import files to get started.", "Import", launchImport)
                            } else if (isGrid) {
                                LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    items(count = libraryEntries.size, key = { libraryEntries[it].id }) { index ->
                                        val item = libraryEntries[index]
                                        GameCard(
                                            item = item,
                                            onOpen = { onOpen(item) },
                                            onFavorite = { viewModel.toggleFavorite(item) },
                                            onDelete = { pendingDelete = item },
                                            onCover = { pendingCoverItemId = item.id; coverPicker.launch(arrayOf("image/*")) },
                                            onTags = { tagEditItem = item; tagEditValue = item.tags ?: "" },
                                            onMove = {},
                                            onRename = {},
                                            coverAvailable = !item.coverPath.isNullOrBlank(),
                                            viewModel = viewModel,
                                            selectedFolder = selectedFolder
                                        )
                                    }
                                }
                            } else {
                                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(count = libraryEntries.size, key = { libraryEntries[it].id }) { index ->
                                        val item = libraryEntries[index]
                                        ListItemRow(
                                            item = item,
                                            onOpen = { onOpen(item) },
                                            onFavorite = { viewModel.toggleFavorite(item) },
                                            onDelete = { pendingDelete = item }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    BudsTab.SAVES -> {
                        val saved = favorites.filter { it.folderId == null || selectedFolder == it.folderId }.filter { it.title.contains(searchQuery, ignoreCase = true) }
                        if (saved.isEmpty()) EmptyState("No saved items", "Tap the heart to save items.", null, {})
                        else LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 96.dp)) {
                            items(count = saved.size, key = { saved[it].id }) { index ->
                                val item = saved[index]
                                GameCard(item = item, onOpen = { onOpen(item) }, onFavorite = { viewModel.toggleFavorite(item) }, onDelete = { pendingDelete = item }, onCover = {}, onTags = { tagEditItem = item; tagEditValue = item.tags ?: "" }, onMove = {}, onRename = {}, coverAvailable = !item.coverPath.isNullOrBlank(), viewModel = viewModel, selectedFolder = selectedFolder)
                            }
                        }
                    }
                    BudsTab.RECENT -> {
                        val r = recent.filter { it.title.contains(searchQuery, ignoreCase = true) }
                        if (r.isEmpty()) EmptyState("Nothing opened yet", "Open files to see them here.", null, {})
                        else RecentList(entries = r, onOpen = onOpen, onFavorite = { viewModel.toggleFavorite(it) })
                    }
                    BudsTab.SETTINGS -> SettingsTab(totalCount = games.size, savedCount = favorites.size, recentCount = recent.size, totalBytes = games.sumOf { it.fileSize })
                    BudsTab.STUDY -> StudyTabContent(games = games) { item ->
                        val intent = Intent(context, StudyViewerActivity::class.java)
                        intent.putExtra("gameId", item.id)
                        intent.putExtra("fileName", item.fileName)
                        context.startActivity(intent)
                    }
                }
            }
        }
    }

    val toDelete = pendingDelete
    if (toDelete != null) {
        AlertDialog(onDismissRequest = { pendingDelete = null }, title = { Text("Delete item?") }, text = { Text("\"${toDelete.title}\" will be removed from your library and deleted from this device.") }, confirmButton = { TextButton(onClick = { viewModel.deleteGame(toDelete); pendingDelete = null }) { Text("Delete", color = MaterialTheme.colorScheme.error) } }, dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } }, containerColor = MaterialTheme.colorScheme.surface)
    }

    folderNameDialog.value?.let { name ->
        AlertDialog(onDismissRequest = { folderNameDialog.value = null }, title = { Text("New folder name") }, text = {
            TextField(value = name, onValueChange = { folderNameDialog.value = it }, label = { Text("Name") })
        }, confirmButton = { TextButton(onClick = { viewModel.createFolder(name); folderNameDialog.value = null }) { Text("Create") } }, dismissButton = { TextButton(onClick = { folderNameDialog.value = null }) { Text("Cancel") } })
    }
    renameFolderDialog.value?.let { folder ->
        var name by remember { mutableStateOf(folder.name) }
        AlertDialog(onDismissRequest = { renameFolderDialog.value = null }, title = { Text("Rename folder") }, text = {
            TextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
        }, confirmButton = { TextButton(onClick = { viewModel.renameFolder(folder.id, name); renameFolderDialog.value = null }) { Text("Save") } }, dismissButton = { TextButton(onClick = { renameFolderDialog.value = null }) { Text("Cancel") } })
    }

    tagEditItem?.let { item ->
        AlertDialog(onDismissRequest = { tagEditItem = null; tagEditValue = "" }, title = { Text("Tags for \"${item.title}\"") }, text = {
            TextField(value = tagEditValue, onValueChange = { tagEditValue = it }, label = { Text("Tags (comma-separated)") })
        }, confirmButton = { TextButton(onClick = { viewModel.updateTags(item.id, tagEditValue); tagEditItem = null; tagEditValue = "" }) { Text("Save") } }, dismissButton = { TextButton(onClick = { tagEditItem = null; tagEditValue = "" }) { Text("Cancel") } })
    }

    progressEditItem?.let { item ->
        AlertDialog(onDismissRequest = { progressEditItem = null; progressEditValue = 0 }, title = { Text("Progress for \"${item.title}\"") }, text = {
            TextField(value = progressEditValue.toString(), onValueChange = { progressEditValue = it.toIntOrNull() ?: 0 }, label = { Text("Percent (0-100)") })
        }, confirmButton = { TextButton(onClick = { viewModel.updateProgressValue(item.id, progressEditValue.coerceIn(0, 100)); progressEditItem = null; progressEditValue = 0 }) { Text("Save") } }, dismissButton = { TextButton(onClick = { progressEditItem = null; progressEditValue = 0 }) { Text("Cancel") } })
    }
}

private fun subtitleFor(tab: BudsTab, total: Int, saved: Int, recent: Int): String = when (tab) {
    BudsTab.LIBRARY -> if (total == 1) "1 item" else "$total items"
    BudsTab.SAVES -> if (saved == 1) "1 saved" else "$saved saved"
    BudsTab.RECENT -> if (recent == 1) "1 recent" else "$recent recent"
    BudsTab.STUDY -> "Study & annotate"
    BudsTab.SETTINGS -> "App info"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameCard(
    item: GameItem,
    onOpen: () -> Unit,
    onFavorite: () -> Unit,
    onDelete: () -> Unit,
    onCover: () -> Unit,
    onTags: () -> Unit,
    onMove: () -> Unit,
    onRename: () -> Unit,
    coverAvailable: Boolean,
    viewModel: GameViewModel,
    selectedFolder: String?
) {
    val accent = colorForType(item.type)
    Card(onClick = onOpen, modifier = Modifier.fillMaxWidth().height(196.dp), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxWidth().height(52.dp).background(if (coverAvailable && item.coverPath != null) Color.Transparent else accent.copy(alpha = 0.22f)), contentAlignment = Alignment.CenterStart) {
                if (coverAvailable && item.coverPath != null) {
                    AsyncImage(model = File(item.coverPath), contentDescription = "Cover", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
                TypeBadge(item.type, accent, Modifier.padding(start = 12.dp))
            }
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).weight(1f)) {
                Text(text = item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(text = formatSize(item.fileSize), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.weight(1f))
                if (item.progress > 0) {
                    Text(text = "${item.progress}% complete", style = MaterialTheme.typography.labelSmall, color = accent)
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(progress = item.progress / 100f, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)), color = accent, trackColor = MaterialTheme.colorScheme.surfaceVariant)
                }
            }
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onCover) { Icon(Icons.Filled.Add, contentDescription = "Change cover", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                IconButton(onClick = onTags) { Text("Tags", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                IconButton(onClick = { progressEditItem = item; progressEditValue = item.progress }) { Text("Progress", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                IconButton(onClick = onFavorite) {
                    Icon(imageVector = if (item.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, contentDescription = if (item.isFavorite) "Remove from Saves" else "Save", tint = if (item.isFavorite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}

@Composable
private fun ListItemRow(item: GameItem, onOpen: () -> Unit, onFavorite: () -> Unit, onDelete: () -> Unit) {
    val accent = colorForType(item.type)
    Card(onClick = onOpen, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(accent.copy(alpha = 0.22f)), contentAlignment = Alignment.Center) {
                Text(text = item.type.uppercase(Locale.getDefault()).take(4), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = accent)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = formatSize(item.fileSize), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onFavorite) {
                Icon(imageVector = if (item.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, contentDescription = "Save", tint = if (item.isFavorite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun TypeBadge(type: String, accent: Color, modifier: Modifier = Modifier) {
    Box(modifier = modifier.clip(RoundedCornerShape(8.dp)).background(accent.copy(alpha = 0.30f)).padding(horizontal = 10.dp, vertical = 4.dp)) {
        Text(text = type.uppercase(Locale.getDefault()), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = accent)
    }
}

@Composable
private fun RecentList(entries: List<GameItem>, onOpen: (GameItem) -> Unit, onFavorite: (GameItem) -> Unit) {
    if (entries.isEmpty()) {
        EmptyState(title = "Nothing opened yet", message = "Files you open will appear here so you can jump back in quickly.", actionLabel = null, onAction = {})
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(count = entries.size, key = { index -> entries[index].id }) { index ->
            val item = entries[index]
            val accent = colorForType(item.type)
            Card(onClick = { onOpen(item) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(accent.copy(alpha = 0.22f)), contentAlignment = Alignment.Center) {
                        Text(text = item.type.uppercase(Locale.getDefault()).take(4), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = accent)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(text = relativeTime(item.lastPlayedAt) + if (item.progress > 0) "  ·  ${item.progress}%" else "", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { onFavorite(item) }) {
                        Icon(imageVector = if (item.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, contentDescription = "Save", tint = if (item.isFavorite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoCard(content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
    }
}

@Composable
private fun ThinDivider() {
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)))
}

@Composable
private fun SectionTitle(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun EmptyState(title: String, message: String, actionLabel: String?, onAction: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            if (actionLabel != null) {
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = onAction) { Text(actionLabel) }
            }
        }
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes <= 0L -> "0 KB"
    bytes < 1024L -> "$bytes B"
    bytes < 1024L * 1024L -> "${bytes / 1024L} KB"
    else -> String.format(Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024.0))
}

private fun relativeTime(ts: Long?): String {
    if (ts == null || ts <= 0L) return "Not opened yet"
    val diff = System.currentTimeMillis() - ts
    return when {
        diff < 60_000L -> "Just now"
        diff < 3_600_000L -> "${diff / 60_000L} min ago"
        diff < 86_400_000L -> "${diff / 3_600_000L} hr ago"
        diff < 7L * 86_400_000L -> "${diff / 86_400_000L} d ago"
        else -> SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(ts))
    }
}

@Composable
private fun SettingsTab(totalCount: Int, savedCount: Int, recentCount: Int, totalBytes: Long) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            InfoCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Buds Pro", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text("Offline library for HTML, PDF, JSON and images", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        item {
            InfoCard {
                SectionTitle("Statistics")
                StatRow("Total library items", totalCount.toString())
                ThinDivider()
                StatRow("Saved items", savedCount.toString())
                ThinDivider()
                StatRow("Recent items", recentCount.toString())
                ThinDivider()
                StatRow("Storage used", formatSize(totalBytes))
            }
        }
        item {
            InfoCard {
                SectionTitle("Supported file types")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("HTML", "PDF", "JSON", "IMG").forEach { t -> TypeBadge(t, colorForType(t)) }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Imported files are copied into this app's private storage, so they keep working offline.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
