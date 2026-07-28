package com.budspro.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.budspro.app.data.GameItem
import com.budspro.app.ui.components.CollectionPickerDialog
import com.budspro.app.ui.components.DeleteConfirmDialog
import com.budspro.app.ui.components.HeroOpenOverlay
import com.budspro.app.ui.components.ItemAction
import com.budspro.app.ui.components.ItemContextSheet
import com.budspro.app.ui.components.ItemInfoDialog
import com.budspro.app.ui.components.RenameDialog
import com.budspro.app.ui.components.TextInputDialog
import com.budspro.app.ui.screens.CollectionDetailScreen
import com.budspro.app.ui.screens.CollectionsScreen
import com.budspro.app.ui.screens.LibraryScreen
import com.budspro.app.ui.screens.SettingsScreen
import java.io.File

/** Bottom navigation destinations. */
enum class BudsDestination(val route: String, val label: String, val icon: ImageVector) {
    LIBRARY("library", "Library", Icons.Filled.LibraryBooks),
    COLLECTIONS("collections", "Collections", Icons.Filled.Folder),
    SETTINGS("settings", "Settings", Icons.Filled.Settings)
}

private const val COLLECTION_DETAIL_ROUTE = "collection/{collectionId}"
private const val STUDY_ROUTE = "study"

/**
 * The new navigation shell: Library, Collections and Settings, with the
 * long-press context sheet, dialogs and the hero open transition wired in.
 *
 * The original [BudsProApp] composable in MainActivity is untouched and still
 * compiles; this shell simply sits in front of it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudsProShell(
    viewModel: LibraryViewModel,
    appVersion: String,
    onImportRequested: () -> Unit,
    onOpenItem: (GameItem) -> Unit,
    onOpenStudy: ((GameItem) -> Unit)? = null,
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val games by viewModel.games.collectAsState()
    val collections by viewModel.collections.collectAsState()
    val prefs by viewModel.preferences.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val storageBytes by viewModel.storageBytes.collectAsState()
    val cacheBytes by viewModel.cacheBytes.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // Long-press sheet + dialog state
    var sheetItem by remember { mutableStateOf<GameItem?>(null) }
    var renameItem by remember { mutableStateOf<GameItem?>(null) }
    var deleteItem by remember { mutableStateOf<GameItem?>(null) }
    var infoItem by remember { mutableStateOf<GameItem?>(null) }
    var collectionPickerItem by remember { mutableStateOf<GameItem?>(null) }
    var newCollectionForItem by remember { mutableStateOf<GameItem?>(null) }
    var coverTargetId by remember { mutableStateOf<String?>(null) }
    var hero by remember { mutableStateOf<Pair<GameItem, Rect>?>(null) }

    val coverPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        val target = coverTargetId
        if (uri != null && target != null) viewModel.setCover(target, uri)
        coverTargetId = null
    }

    val backupExporter = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? -> if (uri != null) viewModel.exportBackup(uri) }

    val backupImporter = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> if (uri != null) viewModel.importBackup(uri) }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeStatusMessage()
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val itemsByCollection = remember(games) { games.groupBy { it.collectionId ?: "" } }

    fun openWithHero(item: GameItem, bounds: Rect) {
        viewModel.markOpened(item)
        if (bounds.width > 0f) hero = item to bounds else onOpenItem(item)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = titleForRoute(currentRoute),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = subtitleForRoute(currentRoute, games.size, collections.size),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    if (onOpenStudy != null && currentRoute == BudsDestination.LIBRARY.route) {
                        IconButton(onClick = { navController.navigate(STUDY_ROUTE) }) {
                            Icon(Icons.Filled.MenuBook, contentDescription = "Study")
                        }
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
                BudsDestination.values().forEach { dest ->
                    val selected = currentRoute == dest.route ||
                        (dest == BudsDestination.COLLECTIONS && currentRoute == COLLECTION_DETAIL_ROUTE)
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (!selected) {
                                navController.navigate(dest.route) {
                                    popUpTo(BudsDestination.LIBRARY.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = { Icon(dest.icon, contentDescription = dest.label) },
                        label = { Text(dest.label) },
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
            if (currentRoute == BudsDestination.LIBRARY.route) {
                ExtendedFloatingActionButton(
                    onClick = onImportRequested,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    icon = { Icon(Icons.Filled.Add, contentDescription = "Import") },
                    text = { Text("Import") }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            NavHost(
                navController = navController,
                startDestination = BudsDestination.LIBRARY.route,
                enterTransition = {
                    slideInHorizontally(
                        animationSpec = tween(280),
                        initialOffsetX = { it / 8 }
                    ) + fadeIn(tween(280))
                },
                exitTransition = { fadeOut(tween(180)) },
                popEnterTransition = {
                    slideInHorizontally(
                        animationSpec = tween(280),
                        initialOffsetX = { -it / 8 }
                    ) + fadeIn(tween(280))
                },
                popExitTransition = {
                    slideOutHorizontally(
                        animationSpec = tween(220),
                        targetOffsetX = { it / 8 }
                    ) + fadeOut(tween(180))
                }
            ) {
                composable(BudsDestination.LIBRARY.route) {
                    LibraryScreen(
                        items = games,
                        collections = collections,
                        isLoading = isLoading,
                        isRefreshing = isRefreshing,
                        defaultView = prefs.defaultView,
                        hapticsEnabled = prefs.hapticsEnabled,
                        onRefresh = viewModel::refresh,
                        onOpen = { item, bounds -> openWithHero(item, bounds) },
                        onLongPress = { sheetItem = it },
                        onImport = onImportRequested
                    )
                }

                composable(BudsDestination.COLLECTIONS.route) {
                    CollectionsScreen(
                        collections = collections,
                        itemsByCollection = itemsByCollection,
                        isLoading = isLoading,
                        hapticsEnabled = prefs.hapticsEnabled,
                        onOpenCollection = { navController.navigate("collection/${it.id}") },
                        onCreate = { viewModel.createCollection(it) },
                        onRename = viewModel::renameCollection,
                        onDelete = viewModel::deleteCollection
                    )
                }

                composable(
                    route = COLLECTION_DETAIL_ROUTE,
                    arguments = listOf(navArgument("collectionId") { type = NavType.StringType })
                ) { entry ->
                    val id = entry.arguments?.getString("collectionId").orEmpty()
                    CollectionDetailScreen(
                        collection = collections.firstOrNull { it.id == id },
                        items = itemsByCollection[id].orEmpty(),
                        hapticsEnabled = prefs.hapticsEnabled,
                        onBack = { navController.popBackStack() },
                        onOpen = { item, bounds -> openWithHero(item, bounds) },
                        onLongPress = { sheetItem = it }
                    )
                }

                // The original Study tab is preserved, reachable from the
                // Library top bar so no existing capability is lost.
                composable(STUDY_ROUTE) {
                    com.budspro.app.StudyTabContent(games = games) { item ->
                        onOpenStudy?.invoke(item)
                    }
                }

                composable(BudsDestination.SETTINGS.route) {
                    LaunchedEffect(Unit) { viewModel.refreshStorageStats() }
                    SettingsScreen(
                        theme = prefs.theme,
                        defaultView = prefs.defaultView,
                        hapticsEnabled = prefs.hapticsEnabled,
                        storageBytes = storageBytes,
                        cacheBytes = cacheBytes,
                        itemCount = games.size,
                        collectionCount = collections.size,
                        appVersion = appVersion,
                        onThemeChange = viewModel::setTheme,
                        onDefaultViewChange = viewModel::setDefaultView,
                        onHapticsChange = viewModel::setHaptics,
                        onClearCache = viewModel::clearCache,
                        onExportBackup = {
                            backupExporter.launch(
                                com.budspro.app.util.BackupManager.suggestedBackupName()
                            )
                        },
                        onImportBackup = {
                            backupImporter.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
                        }
                    )
                }
            }

            hero?.let { (item, bounds) ->
                HeroOpenOverlay(
                    item = item,
                    startBounds = bounds,
                    onFinished = {
                        hero = null
                        onOpenItem(item)
                    }
                )
            }
        }
    }

    // ---------------- Long press context sheet ----------------
    sheetItem?.let { item ->
        ItemContextSheet(
            item = item,
            onDismiss = { sheetItem = null },
            onAction = { action ->
                sheetItem = null
                when (action) {
                    ItemAction.Open -> {
                        viewModel.markOpened(item)
                        onOpenItem(item)
                    }
                    ItemAction.ChangeCover -> {
                        coverTargetId = item.id
                        coverPicker.launch(arrayOf("image/*"))
                    }
                    ItemAction.Rename -> renameItem = item
                    ItemAction.AddToCollection -> collectionPickerItem = item
                    ItemAction.ToggleFavorite -> viewModel.toggleFavorite(item)
                    ItemAction.Delete -> deleteItem = item
                    ItemAction.Share -> shareItem(context, item)
                    ItemAction.ViewInfo -> infoItem = item
                }
            }
        )
    }

    renameItem?.let { item ->
        RenameDialog(
            initialValue = item.title,
            onDismiss = { renameItem = null },
            onConfirm = { viewModel.rename(item.id, it) }
        )
    }

    deleteItem?.let { item ->
        DeleteConfirmDialog(
            title = item.title,
            onDismiss = { deleteItem = null },
            onConfirm = { viewModel.delete(item) }
        )
    }

    infoItem?.let { item ->
        ItemInfoDialog(
            item = item,
            filePath = File(File(context.filesDir, "games"), item.fileName).absolutePath,
            onDismiss = { infoItem = null }
        )
    }

    collectionPickerItem?.let { item ->
        CollectionPickerDialog(
            collections = collections,
            currentCollectionId = item.collectionId,
            onDismiss = { collectionPickerItem = null },
            onSelect = { viewModel.setItemCollection(item.id, it) },
            onCreateNew = { newCollectionForItem = item }
        )
    }

    newCollectionForItem?.let { item ->
        TextInputDialog(
            title = "New collection",
            label = "Collection name",
            confirmLabel = "Create & add",
            onDismiss = { newCollectionForItem = null },
            onConfirm = { name ->
                viewModel.createCollection(name) { newId ->
                    viewModel.setItemCollection(item.id, newId)
                }
            }
        )
    }
}

private fun titleForRoute(route: String?): String = when (route) {
    BudsDestination.COLLECTIONS.route -> "Collections"
    COLLECTION_DETAIL_ROUTE -> "Collections"
    STUDY_ROUTE -> "Study"
    BudsDestination.SETTINGS.route -> "Settings"
    else -> "Buds Pro"
}

private fun subtitleForRoute(route: String?, items: Int, collections: Int): String = when (route) {
    BudsDestination.COLLECTIONS.route, COLLECTION_DETAIL_ROUTE ->
        if (collections == 1) "1 collection" else "$collections collections"
    STUDY_ROUTE -> "Annotate your images and PDFs"
    BudsDestination.SETTINGS.route -> "Theme, storage and backup"
    else -> if (items == 1) "1 item in your library" else "$items items in your library"
}

/** Shares the underlying file through the existing FileProvider authority. */
private fun shareItem(context: Context, item: GameItem) {
    runCatching {
        val file = File(File(context.filesDir, "games"), item.fileName)
        if (!file.exists()) return
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val mime = when (item.type.lowercase()) {
            "pdf" -> "application/pdf"
            "json" -> "application/json"
            "html" -> "text/html"
            "image" -> "image/*"
            else -> "*/*"
        }
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TITLE, item.title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "Share ${item.title}"))
    }
}
