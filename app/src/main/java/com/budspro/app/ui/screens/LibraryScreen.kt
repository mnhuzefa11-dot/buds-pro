package com.budspro.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.budspro.app.data.CollectionItem
import com.budspro.app.data.DefaultView
import com.budspro.app.data.GameItem
import com.budspro.app.ui.components.BudsPullToRefresh
import com.budspro.app.ui.components.IllustratedEmptyState
import com.budspro.app.ui.components.LibraryGameCard
import com.budspro.app.ui.components.LibraryListRow
import com.budspro.app.ui.components.ShimmerGrid

enum class LibraryFilter(val label: String) {
    ALL("All"),
    FAVORITES("Favorites"),
    RECENT("Recent"),
    HTML("Games"),
    PDF("PDF"),
    IMAGE("Images"),
    JSON("JSON")
}

private fun comparatorFor(sort: LibrarySort): Comparator<GameItem> = when (sort) {
    LibrarySort.DATE -> compareByDescending { it.addedAt }
    LibrarySort.NAME -> compareBy { it.title.lowercase() }
    LibrarySort.RECENT -> compareByDescending { it.lastPlayedAt ?: 0L }
    LibrarySort.SIZE -> compareByDescending { it.fileSize }
}

enum class LibrarySort(val label: String) {
    DATE("Newest"),
    NAME("Name"),
    RECENT("Last opened"),
    SIZE("Size")
}

/**
 * The main library grid/list.
 *
 * Everything is presentational — all mutations are delegated upward through
 * callbacks so the screen can be reused by the collection detail screen too.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    items: List<GameItem>,
    collections: List<CollectionItem>,
    isLoading: Boolean,
    isRefreshing: Boolean,
    defaultView: DefaultView,
    hapticsEnabled: Boolean,
    onRefresh: () -> Unit,
    onOpen: (GameItem, androidx.compose.ui.geometry.Rect) -> Unit,
    onLongPress: (GameItem) -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier
) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(LibraryFilter.ALL) }
    var sort by rememberSaveable { mutableStateOf(LibrarySort.DATE) }
    var collectionFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var gridMode by rememberSaveable(defaultView) { mutableStateOf(defaultView == DefaultView.GRID) }

    val visible = remember(items, query, filter, sort, collectionFilter) {
        items
            .asSequence()
            .filter { it.title.contains(query, ignoreCase = true) }
            .filter { collectionFilter == null || it.collectionId == collectionFilter }
            .filter { item ->
                when (filter) {
                    LibraryFilter.ALL -> true
                    LibraryFilter.FAVORITES -> item.isFavorite
                    LibraryFilter.RECENT -> item.lastPlayedAt != null
                    LibraryFilter.HTML -> item.type.equals("html", true)
                    LibraryFilter.PDF -> item.type.equals("pdf", true)
                    LibraryFilter.IMAGE -> item.type.equals("image", true)
                    LibraryFilter.JSON -> item.type.equals("json", true)
                }
            }
            .sortedWith(comparatorFor(sort))
            .toList()
    }

    Column(modifier = modifier.fillMaxSize()) {

        // ---- Search ----
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            placeholder = { Text("Search your library") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
        )

        // ---- Filter chips ----
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 4.dp)
        ) {
            items(LibraryFilter.values().toList()) { f ->
                FilterChip(
                    selected = filter == f,
                    onClick = { filter = f },
                    label = { Text(f.label) },
                    shape = RoundedCornerShape(16.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        // ---- Collection filter ----
        if (collections.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 6.dp)
            ) {
                item {
                    FilterChip(
                        selected = collectionFilter == null,
                        onClick = { collectionFilter = null },
                        label = { Text("All collections") },
                        shape = RoundedCornerShape(16.dp)
                    )
                }
                items(collections) { c ->
                    FilterChip(
                        selected = collectionFilter == c.id,
                        onClick = {
                            collectionFilter = if (collectionFilter == c.id) null else c.id
                        },
                        label = { Text(c.name) },
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }
        }

        // ---- Sort + view toggle ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (visible.size == 1) "1 item" else "${visible.size} items",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = {
                val all = LibrarySort.values()
                sort = all[(all.indexOf(sort) + 1) % all.size]
            }) {
                Icon(Icons.Filled.SortByAlpha, contentDescription = "Sort: ${sort.label}")
            }
            Text(
                text = sort.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = { gridMode = !gridMode }) {
                Icon(
                    imageVector = if (gridMode) Icons.Filled.GridView else Icons.Filled.ViewList,
                    contentDescription = "Toggle grid or list"
                )
            }
        }

        // ---- Content ----
        BudsPullToRefresh(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize()
        ) {
            LibraryContent(
                visible = visible,
                isLoading = isLoading,
                query = query,
                gridMode = gridMode,
                hapticsEnabled = hapticsEnabled,
                onOpen = onOpen,
                onLongPress = onLongPress,
                onImport = onImport,
                onClearSearch = { query = ""; filter = LibraryFilter.ALL }
            )
        }
    }
}

/**
 * The shimmer / empty-state / grid / list portion of the library.
 *
 * Deliberately a top-level composable with no layout receiver: when these
 * `AnimatedVisibility` calls sat directly inside [LibraryScreen]'s `Column`,
 * Kotlin resolved them to the `ColumnScope` overload, which cannot be reached
 * through the `BoxScope` lambda of [BudsPullToRefresh]. Hoisting them here
 * makes the plain (receiver-less) overload the only candidate, and the two
 * states still cross-fade on top of each other exactly as intended.
 */
@Composable
private fun LibraryContent(
    visible: List<GameItem>,
    isLoading: Boolean,
    query: String,
    gridMode: Boolean,
    hapticsEnabled: Boolean,
    onOpen: (GameItem, androidx.compose.ui.geometry.Rect) -> Unit,
    onLongPress: (GameItem) -> Unit,
    onImport: () -> Unit,
    onClearSearch: () -> Unit
) {
    AnimatedVisibility(visible = isLoading, enter = fadeIn(), exit = fadeOut()) {
        ShimmerGrid()
    }

    AnimatedVisibility(visible = !isLoading, enter = fadeIn(), exit = fadeOut()) {
        when {
            visible.isEmpty() && query.isNotBlank() -> IllustratedEmptyState(
                icon = Icons.Filled.Search,
                title = "No matches",
                message = "Nothing in your library matches \"$query\". Try a different search or clear the filters.",
                actionLabel = "Clear search",
                onAction = onClearSearch
            )

            visible.isEmpty() -> IllustratedEmptyState(
                icon = Icons.Filled.LibraryBooks,
                title = "Your library is waiting",
                message = "Import HTML games, PDFs, images or JSON sets and they'll live here — fully offline, always yours.",
                actionLabel = "Import files",
                onAction = onImport
            )

            gridMode -> LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 110.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(visible, key = { it.id }) { item ->
                    LibraryGameCard(
                        item = item,
                        onOpen = { rect -> onOpen(item, rect) },
                        onLongPress = { onLongPress(item) },
                        hapticsEnabled = hapticsEnabled
                    )
                }
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 110.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(visible, key = { it.id }) { item ->
                    LibraryListRow(
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
