package com.budspro.app

import android.content.Intent
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.budspro.app.data.GameItem
import com.budspro.app.ui.GameViewModel
import com.budspro.app.ui.theme.BudsProTheme
import com.budspro.app.ui.theme.colorForType
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
                    // Stamp "last opened" so the Recent tab stays accurate for
                    // every file type. The way the item is opened below is
                    // exactly the same as before.
                    viewModel.markOpened(item)

                    val intent = Intent(this, PlayerActivity::class.java)
                    intent.putExtra("gameId", item.id)
                    startActivity(intent)
                }
            }
        }
    }
}

/* -------------------------------------------------------------------------
 * Bottom navigation
 * ---------------------------------------------------------------------- */

private enum class BudsTab(val label: String, val icon: ImageVector) {
    LIBRARY("Library", Icons.Filled.List),
    SAVES("Saves", Icons.Filled.Favorite),
    RECENT("Recent", Icons.Filled.Refresh),
    SETTINGS("Settings", Icons.Filled.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudsProApp(viewModel: GameViewModel, onOpen: (GameItem) -> Unit) {
    val games by viewModel.games.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val recent by viewModel.recent.collectAsState()

    val context = LocalContext.current
    var selectedTabIndex by rememberSaveable { mutableStateOf(0) }
    val tabs = BudsTab.values()
    val selectedTab = tabs[selectedTabIndex.coerceIn(0, tabs.size - 1)]
    var pendingDelete by remember { mutableStateOf<GameItem?>(null) }

    // Real Android Storage Access Framework picker — unchanged from the
    // working version, just wired to the new Library tab.
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
        pickerLauncher.launch(
            arrayOf("text/html", "application/pdf", "application/json", "*/*")
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Buds Pro",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = subtitleFor(selectedTab, games.size, favorites.size, recent.size),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTabIndex = index },
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
            // The + import button stays on the Library tab, exactly as before.
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
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (selectedTab) {
                BudsTab.LIBRARY -> ItemGrid(
                    entries = games,
                    emptyTitle = "Your library is empty",
                    emptyMessage = "Tap Import to add an HTML, PDF or JSON file.",
                    emptyActionLabel = "Import a file",
                    onEmptyAction = launchImport,
                    onOpen = onOpen,
                    onFavorite = { viewModel.toggleFavorite(it) },
                    onDelete = { pendingDelete = it }
                )

                BudsTab.SAVES -> ItemGrid(
                    entries = favorites,
                    emptyTitle = "No saved items yet",
                    emptyMessage = "Tap the heart on any card in Library to save it here.",
                    emptyActionLabel = null,
                    onEmptyAction = {},
                    onOpen = onOpen,
                    onFavorite = { viewModel.toggleFavorite(it) },
                    onDelete = { pendingDelete = it }
                )

                BudsTab.RECENT -> RecentList(
                    entries = recent,
                    onOpen = onOpen,
                    onFavorite = { viewModel.toggleFavorite(it) }
                )

                BudsTab.SETTINGS -> SettingsTab(
                    totalCount = games.size,
                    savedCount = favorites.size,
                    recentCount = recent.size,
                    totalBytes = games.sumOf { it.fileSize }
                )
            }
        }
    }

    // Small confirmation so a mis-tap can't wipe an imported file.
    val toDelete = pendingDelete
    if (toDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete item?") },
            text = { Text("\"${toDelete.title}\" will be removed from your library and deleted from this device.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteGame(toDelete)
                    pendingDelete = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

private fun subtitleFor(tab: BudsTab, total: Int, saved: Int, recent: Int): String = when (tab) {
    BudsTab.LIBRARY -> if (total == 1) "1 item in library" else "$total items in library"
    BudsTab.SAVES -> if (saved == 1) "1 saved item" else "$saved saved items"
    BudsTab.RECENT -> if (recent == 1) "1 recently opened" else "$recent recently opened"
    BudsTab.SETTINGS -> "App info & statistics"
}

/* -------------------------------------------------------------------------
 * Library / Saves — card grid
 * ---------------------------------------------------------------------- */

@Composable
private fun ItemGrid(
    entries: List<GameItem>,
    emptyTitle: String,
    emptyMessage: String,
    emptyActionLabel: String?,
    onEmptyAction: () -> Unit,
    onOpen: (GameItem) -> Unit,
    onFavorite: (GameItem) -> Unit,
    onDelete: (GameItem) -> Unit
) {
    if (entries.isEmpty()) {
        EmptyState(emptyTitle, emptyMessage, emptyActionLabel, onEmptyAction)
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(count = entries.size, key = { index -> entries[index].id }) { index ->
            val item = entries[index]
            GameCard(
                item = item,
                onOpen = { onOpen(item) },
                onFavorite = { onFavorite(item) },
                onDelete = { onDelete(item) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameCard(
    item: GameItem,
    onOpen: () -> Unit,
    onFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    val accent = colorForType(item.type)

    Card(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth().height(196.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Coloured header strip stands in for a cover image.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .background(accent.copy(alpha = 0.22f)),
                contentAlignment = Alignment.CenterStart
            ) {
                TypeBadge(item.type, accent, Modifier.padding(start = 12.dp))
            }

            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = formatSize(item.fileSize),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.weight(1f))

                if (item.progress > 0) {
                    Text(
                        text = "${item.progress}% complete",
                        style = MaterialTheme.typography.labelSmall,
                        color = accent
                    )
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = item.progress / 100f,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                        color = accent,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onFavorite) {
                    Icon(
                        imageVector = if (item.isFavorite) Icons.Filled.Favorite
                        else Icons.Filled.FavoriteBorder,
                        contentDescription = if (item.isFavorite) "Remove from Saves" else "Save",
                        tint = if (item.isFavorite) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun TypeBadge(type: String, accent: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(accent.copy(alpha = 0.30f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = type.uppercase(Locale.getDefault()),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = accent
        )
    }
}

/* -------------------------------------------------------------------------
 * Recent
 * ---------------------------------------------------------------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecentList(
    entries: List<GameItem>,
    onOpen: (GameItem) -> Unit,
    onFavorite: (GameItem) -> Unit
) {
    if (entries.isEmpty()) {
        EmptyState(
            title = "Nothing opened yet",
            message = "Files you open will appear here so you can jump back in quickly.",
            actionLabel = null,
            onAction = {}
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(count = entries.size, key = { index -> entries[index].id }) { index ->
            val item = entries[index]
            val accent = colorForType(item.type)
            Card(
                onClick = { onOpen(item) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(accent.copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item.type.uppercase(Locale.getDefault()).take(4),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = accent
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = relativeTime(item.lastPlayedAt) +
                                if (item.progress > 0) "  ·  ${item.progress}%" else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = { onFavorite(item) }) {
                        Icon(
                            imageVector = if (item.isFavorite) Icons.Filled.Favorite
                            else Icons.Filled.FavoriteBorder,
                            contentDescription = "Save",
                            tint = if (item.isFavorite) MaterialTheme.colorScheme.tertiary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/* -------------------------------------------------------------------------
 * Settings
 * ---------------------------------------------------------------------- */

@Composable
private fun SettingsTab(
    totalCount: Int,
    savedCount: Int,
    recentCount: Int,
    totalBytes: Long
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            InfoCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "Buds Pro",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Offline library for your HTML, PDF and JSON files",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                    listOf("HTML", "PDF", "JSON").forEach { t ->
                        TypeBadge(t, colorForType(t))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Imported files are copied into this app's private storage, so they keep working offline.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun InfoCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun ThinDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/* -------------------------------------------------------------------------
 * Shared bits
 * ---------------------------------------------------------------------- */

@Composable
private fun EmptyState(
    title: String,
    message: String,
    actionLabel: String?,
    onAction: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null) {
                Spacer(Modifier.height(12.dp))
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
