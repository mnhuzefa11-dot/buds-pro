package com.budspro.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.budspro.app.data.CollectionItem
import com.budspro.app.data.GameItem
import com.budspro.app.ui.components.DeleteConfirmDialog
import com.budspro.app.ui.components.IllustratedEmptyState
import com.budspro.app.ui.components.CollectionCard
import com.budspro.app.ui.components.LibraryGameCard
import com.budspro.app.ui.components.ShimmerGrid
import com.budspro.app.ui.components.TextInputDialog

/** Grid of all collections. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionsScreen(
    collections: List<CollectionItem>,
    itemsByCollection: Map<String, List<GameItem>>,
    isLoading: Boolean,
    hapticsEnabled: Boolean,
    onOpenCollection: (CollectionItem) -> Unit,
    onRequestCreate: () -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var sheetTarget by remember { mutableStateOf<CollectionItem?>(null) }
    var renameTarget by remember { mutableStateOf<CollectionItem?>(null) }
    var deleteTarget by remember { mutableStateOf<CollectionItem?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(visible = isLoading, enter = fadeIn(), exit = fadeOut()) {
            ShimmerGrid(itemCount = 4)
        }

        AnimatedVisibility(visible = !isLoading, enter = fadeIn(), exit = fadeOut()) {
            if (collections.isEmpty()) {
                IllustratedEmptyState(
                    icon = Icons.Filled.CreateNewFolder,
                    title = "No collections yet",
                    message = "Group your content into collections like \"Biology\" or \"Math Games\", then jump straight to what you need.",
                    actionLabel = "Create a collection",
                    onAction = onRequestCreate
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 110.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(collections, key = { it.id }) { c ->
                        CollectionCard(
                            collection = c,
                            items = itemsByCollection[c.id].orEmpty(),
                            onClick = { onOpenCollection(c) },
                            onLongPress = { sheetTarget = c },
                            hapticsEnabled = hapticsEnabled
                        )
                    }
                }
            }
        }
    }

    sheetTarget?.let { target ->
        ModalBottomSheet(
            onDismissRequest = { sheetTarget = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                Text(
                    text = target.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { renameTarget = target; sheetTarget = null }) {
                        Icon(Icons.Filled.DriveFileRenameOutline, contentDescription = "Rename")
                    }
                    Text("Rename", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.weight(1f))
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { deleteTarget = target; sheetTarget = null }) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                    Text(
                        "Delete collection",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Deleting a collection keeps every item — they simply move back to your library.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        }
    }

    renameTarget?.let { target ->
        TextInputDialog(
            title = "Rename collection",
            label = "Name",
            initialValue = target.name,
            confirmLabel = "Save",
            onDismiss = { renameTarget = null },
            onConfirm = { onRename(target.id, it) }
        )
    }

    deleteTarget?.let { target ->
        DeleteConfirmDialog(
            title = target.name,
            onDismiss = { deleteTarget = null },
            onConfirm = { onDelete(target.id) }
        )
    }
}

/** Contents of a single collection. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionDetailScreen(
    collection: CollectionItem?,
    items: List<GameItem>,
    hapticsEnabled: Boolean,
    onBack: () -> Unit,
    onOpen: (GameItem, androidx.compose.ui.geometry.Rect) -> Unit,
    onLongPress: (GameItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 12.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = collection?.name ?: "Collection",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (items.size == 1) "1 item" else "${items.size} items",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (items.isEmpty()) {
            IllustratedEmptyState(
                icon = Icons.Filled.FolderOpen,
                title = "This collection is empty",
                message = "Long press any item in your library and choose \"Add to Collection\" to file it here."
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 110.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    LibraryGameCard(
                        item = item,
                        onOpen = { rect -> onOpen(item, rect) },
                        onLongPress = { onLongPress(item) },
                        hapticsEnabled = hapticsEnabled
                    )
                }
            }
        }
    }
}
