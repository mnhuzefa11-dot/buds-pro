package com.budspro.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.budspro.app.data.CollectionItem
import com.budspro.app.data.GameItem
import com.budspro.app.data.effectiveCover
import com.budspro.app.ui.theme.badgeLabelForType
import com.budspro.app.ui.theme.colorForType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Every action the long-press sheet can raise. */
sealed interface ItemAction {
    data object Open : ItemAction
    data object ChangeCover : ItemAction
    data object Rename : ItemAction
    data object AddToCollection : ItemAction
    data object RemoveFromCollection : ItemAction
    data object ToggleFavorite : ItemAction
    data object Delete : ItemAction
    data object Share : ItemAction
    data object ViewInfo : ItemAction
}

/**
 * Long-press context menu for a library item.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemContextSheet(
    item: GameItem,
    onDismiss: () -> Unit,
    onAction: (ItemAction) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val accent = colorForType(item.type)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 12.dp)
        ) {
            // Header preview
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                ) {
                    CoverArt(item = item, accent = accent, modifier = Modifier.fillMaxSize())
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${badgeLabelForType(item.type)}  ·  ${formatBytes(item.fileSize)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            SheetDivider()
            Spacer(Modifier.height(4.dp))

            SheetRow(Icons.Filled.PlayArrow, if (item.type == "html") "Play" else "Open") {
                onAction(ItemAction.Open)
            }
            SheetRow(
                Icons.Filled.Image,
                if (item.effectiveCover.isNullOrBlank()) "Add Cover" else "Change Cover"
            ) { onAction(ItemAction.ChangeCover) }
            SheetRow(Icons.Filled.DriveFileRenameOutline, "Rename") {
                onAction(ItemAction.Rename)
            }
            if (item.collectionId != null) {
                SheetRow(Icons.Filled.Folder, "Change Collection") {
                    onAction(ItemAction.AddToCollection)
                }
                SheetRow(
                    Icons.Filled.Folder,
                    "Remove from Collection",
                    tint = MaterialTheme.colorScheme.error,
                    textColor = MaterialTheme.colorScheme.error
                ) {
                    onAction(ItemAction.RemoveFromCollection)
                }
            } else {
                SheetRow(Icons.Filled.Folder, "Add to Collection") {
                    onAction(ItemAction.AddToCollection)
                }
            }
            SheetRow(
                if (item.isFavorite) Icons.Filled.FavoriteBorder else Icons.Filled.Favorite,
                if (item.isFavorite) "Unfavorite" else "Mark as Favorite",
                tint = if (item.isFavorite) null else MaterialTheme.colorScheme.tertiary
            ) { onAction(ItemAction.ToggleFavorite) }
            SheetRow(Icons.Filled.Share, "Share file") { onAction(ItemAction.Share) }
            SheetRow(Icons.Filled.Info, "View Info") { onAction(ItemAction.ViewInfo) }

            Spacer(Modifier.height(4.dp))
            SheetDivider()
            Spacer(Modifier.height(4.dp))

            SheetRow(
                Icons.Filled.Delete,
                "Delete",
                tint = MaterialTheme.colorScheme.error,
                textColor = MaterialTheme.colorScheme.error
            ) { onAction(ItemAction.Delete) }
        }
    }
}

@Composable
private fun SheetRow(
    icon: ImageVector,
    label: String,
    tint: Color? = null,
    textColor: Color? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint ?: MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(18.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = textColor ?: MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SheetDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    )
}

// ----------------------------------------------------------------------
// Dialogs used together with the sheet
// ----------------------------------------------------------------------

@Composable
fun RenameDialog(
    initialValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename item") },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                label = { Text("Title") }
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(value); onDismiss() },
                enabled = value.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun DeleteConfirmDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Filled.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text("Delete \"$title\"?") },
        text = {
            Text("This removes the item from your library and deletes the file from this device. This cannot be undone.")
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun ItemInfoDialog(item: GameItem, filePath: String, onDismiss: () -> Unit) {
    val dateFmt = remember { SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Info, contentDescription = null) },
        title = { Text(item.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                InfoRow("Type", badgeLabelForType(item.type))
                InfoRow("File size", formatBytes(item.fileSize))
                InfoRow("Date added", dateFmt.format(Date(item.addedAt)))
                InfoRow(
                    "Last opened",
                    item.lastPlayedAt?.let { dateFmt.format(Date(it)) } ?: "Never"
                )
                InfoRow("Progress", "${item.progress}%")
                InfoRow("Time spent", formatDuration(item.totalPlayTime))
                InfoRow("Favorite", if (item.isFavorite) "Yes" else "No")
                InfoRow("File name", item.fileName)
                InfoRow("Location", filePath)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(96.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

/** Picker used by "Add to Collection". */
@Composable
fun CollectionPickerDialog(
    collections: List<CollectionItem>,
    currentCollectionId: String?,
    onDismiss: () -> Unit,
    onSelect: (String?) -> Unit,
    onCreateNew: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to Collection") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                PickerRow(
                    label = "No collection",
                    selected = currentCollectionId == null,
                    onClick = { onSelect(null); onDismiss() }
                )
                collections.forEach { c ->
                    PickerRow(
                        label = c.name,
                        selected = currentCollectionId == c.id,
                        onClick = { onSelect(c.id); onDismiss() }
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDismiss(); onCreateNew() }
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
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun PickerRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(6.dp))
        Text(text = label, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun TextInputDialog(
    title: String,
    label: String,
    initialValue: String = "",
    confirmLabel: String = "Create",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                label = { Text(label) }
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(value); onDismiss() },
                enabled = value.isNotBlank()
            ) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp)
    )
}

fun formatDuration(ms: Long): String {
    if (ms <= 0L) return "None yet"
    val totalMinutes = ms / 60_000L
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        totalMinutes > 0 -> "${totalMinutes}m"
        else -> "<1m"
    }
}
