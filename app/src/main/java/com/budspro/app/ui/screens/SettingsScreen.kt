package com.budspro.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.budspro.app.data.AppTheme
import com.budspro.app.data.DefaultView
import com.budspro.app.ui.components.formatBytes

/**
 * Settings: theme, default view, storage, cache, backup and about.
 */
@Composable
fun SettingsScreen(
    theme: AppTheme,
    defaultView: DefaultView,
    hapticsEnabled: Boolean,
    storageBytes: Long,
    cacheBytes: Long,
    itemCount: Int,
    collectionCount: Int,
    appVersion: String,
    onThemeChange: (AppTheme) -> Unit,
    onDefaultViewChange: (DefaultView) -> Unit,
    onHapticsChange: (Boolean) -> Unit,
    onClearCache: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showClearCache by remember { mutableStateOf(false) }
    var showImportWarning by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ---- Appearance ----
        item {
            SettingsCard(icon = Icons.Filled.Palette, title = "Appearance") {
                Text(
                    "App theme",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AppTheme.values().forEach { t ->
                        ThemeSwatch(
                            theme = t,
                            selected = theme == t,
                            onClick = { onThemeChange(t) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // ---- Default view ----
        item {
            SettingsCard(icon = Icons.Filled.GridView, title = "Library layout") {
                Text(
                    "Default view when the library opens",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ToggleOption(
                        label = "Grid",
                        selected = defaultView == DefaultView.GRID,
                        onClick = { onDefaultViewChange(DefaultView.GRID) },
                        modifier = Modifier.weight(1f)
                    )
                    ToggleOption(
                        label = "List",
                        selected = defaultView == DefaultView.LIST,
                        onClick = { onDefaultViewChange(DefaultView.LIST) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // ---- Haptics ----
        item {
            SettingsCard(icon = Icons.Filled.Vibration, title = "Haptics") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Vibrate on long press", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "A short tap when the context menu opens",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = hapticsEnabled, onCheckedChange = onHapticsChange)
                }
            }
        }

        // ---- Storage ----
        item {
            SettingsCard(icon = Icons.Filled.Storage, title = "Storage") {
                StatLine("Imported content", formatBytes(storageBytes))
                StatLine("Cache", formatBytes(cacheBytes))
                StatLine("Items", itemCount.toString())
                StatLine("Collections", collectionCount.toString())
                Spacer(Modifier.height(10.dp))
                val total = (storageBytes + cacheBytes).coerceAtLeast(1L)
                val contentFraction = (storageBytes.toFloat() / total).coerceIn(0f, 1f)
                // Hand-drawn bar rather than LinearProgressIndicator: the
                // determinate overload's signature changed between Material3
                // releases, and this renders identically on every version.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(contentFraction)
                            .height(6.dp)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { showClearCache = true },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.CleaningServices, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Clear cache")
                }
            }
        }

        // ---- Backup ----
        item {
            SettingsCard(icon = Icons.Filled.Archive, title = "Backup & restore") {
                Text(
                    "Export everything — files, covers, collections and progress — into a single ZIP you can keep anywhere.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onExportBackup,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Archive, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Export library backup (ZIP)")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showImportWarning = true },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Unarchive, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Import library from ZIP")
                }
            }
        }

        // ---- About ----
        item {
            SettingsCard(icon = Icons.Filled.Info, title = "About") {
                StatLine("App", "Buds Pro")
                StatLine("Version", appVersion)
                StatLine("Package", "com.budspro.app")
                Spacer(Modifier.height(8.dp))
                Text(
                    "An offline-first library for HTML games, PDFs, images and JSON study sets. Everything you import stays in this app's private storage — nothing is uploaded.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showClearCache) {
        AlertDialog(
            onDismissRequest = { showClearCache = false },
            icon = { Icon(Icons.Filled.CleaningServices, contentDescription = null) },
            title = { Text("Clear cache?") },
            text = {
                Text("This clears temporary files and cached cover thumbnails (${formatBytes(cacheBytes)}). Your imported content, covers and progress are not affected.")
            },
            confirmButton = {
                TextButton(onClick = { onClearCache(); showClearCache = false }) { Text("Clear") }
            },
            dismissButton = { TextButton(onClick = { showClearCache = false }) { Text("Cancel") } },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showImportWarning) {
        AlertDialog(
            onDismissRequest = { showImportWarning = false },
            icon = { Icon(Icons.Filled.Unarchive, contentDescription = null) },
            title = { Text("Import backup?") },
            text = {
                Text("Items from the backup are merged into your library. Anything with the same id is replaced by the backup's copy; everything else is left untouched.")
            },
            confirmButton = {
                TextButton(onClick = { onImportBackup(); showImportWarning = false }) { Text("Choose file") }
            },
            dismissButton = { TextButton(onClick = { showImportWarning = false }) { Text("Cancel") } },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun SettingsCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(19.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun ThemeSwatch(
    theme: AppTheme,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = when (theme) {
        AppTheme.DARK -> listOf(Color(0xFF16121F), Color(0xFFA855F7))
        AppTheme.LIGHT -> listOf(Color(0xFFFFFFFF), Color(0xFF7C3AED))
        AppTheme.AMOLED -> listOf(Color(0xFF000000), Color(0xFFA855F7))
        AppTheme.PURPLE -> listOf(Color(0xFF1E0F3C), Color(0xFFC084FC))
    }
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.verticalGradient(colors))
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                )
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = theme.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun ToggleOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (label == "Grid") Icons.Filled.GridView else Icons.Filled.ViewList,
                contentDescription = null,
                modifier = Modifier.size(17.dp),
                tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun StatLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
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
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
