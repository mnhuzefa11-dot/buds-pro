package com.budspro.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.budspro.app.data.AppDatabase
import com.budspro.app.data.AppTheme
import com.budspro.app.data.BudsPreferences
import com.budspro.app.data.CollectionItem
import com.budspro.app.data.DefaultView
import com.budspro.app.data.GameItem
import com.budspro.app.data.effectiveCover
import com.budspro.app.data.UserPreferencesRepository
import com.budspro.app.util.BackupManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Everything the new Library / Collections / Settings screens need.
 *
 * This is a brand new ViewModel that sits *next to* the original
 * [GameViewModel]; the original is untouched and still drives the legacy
 * screens, so nothing that worked before can break.
 */
class LibraryViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.getInstance(app)
    private val gameDao = db.gameDao()
    private val collectionDao = db.collectionDao()
    private val prefsRepo = UserPreferencesRepository(app)
    private val backupManager = BackupManager(app)

    // ------------------------------------------------------------------
    // Streams
    // ------------------------------------------------------------------

    val games: StateFlow<List<GameItem>> = gameDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val collections: StateFlow<List<CollectionItem>> = collectionDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val preferences: StateFlow<BudsPreferences> = prefsRepo.preferences
        .stateIn(viewModelScope, SharingStarted.Eagerly, BudsPreferences())

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _storageBytes = MutableStateFlow(0L)
    val storageBytes: StateFlow<Long> = _storageBytes.asStateFlow()

    private val _cacheBytes = MutableStateFlow(0L)
    val cacheBytes: StateFlow<Long> = _cacheBytes.asStateFlow()

    init {
        viewModelScope.launch {
            // Short, deliberate shimmer window so the first frame is never an
            // empty flash. Real data replaces it as soon as Room emits.
            delay(450)
            _isLoading.value = false
        }
        refreshStorageStats()
    }

    fun consumeStatusMessage() {
        _statusMessage.value = null
    }

    fun postStatus(message: String) {
        _statusMessage.value = message
    }

    /** Pull-to-refresh: recomputes on-disk sizes and re-syncs file metadata. */
    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            withContext(Dispatchers.IO) {
                val context = getApplication<Application>()
                val gamesDir = File(context.filesDir, "games")
                gameDao.getAllOnce().forEach { item ->
                    val f = File(gamesDir, item.fileName)
                    if (f.exists() && f.length() != item.fileSize) {
                        gameDao.update(item.copy(fileSize = f.length()))
                    }
                }
            }
            refreshStorageStats()
            delay(350)
            _isRefreshing.value = false
        }
    }

    // ------------------------------------------------------------------
    // Item actions (long-press context menu)
    // ------------------------------------------------------------------

    fun setCover(itemId: String, sourceUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val coversDir = File(context.filesDir, "covers").apply { mkdirs() }
            val coverFile = File(coversDir, "$itemId.jpg")
            runCatching {
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    coverFile.outputStream().use { output -> input.copyTo(output) }
                }
            }.onFailure {
                _statusMessage.value = "Could not read that image"
                return@launch
            }
            if (!coverFile.exists()) return@launch
            // Write both columns so old and new UI agree on the cover.
            gameDao.updateCoverImagePath(itemId, coverFile.absolutePath)
            gameDao.updateCover(itemId, coverFile.absolutePath)
            _statusMessage.value = "Cover updated"
        }
    }

    fun clearCover(itemId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val item = gameDao.getById(itemId) ?: return@launch
            item.effectiveCover?.let { path ->
                val f = File(path)
                if (f.exists()) f.delete()
            }
            gameDao.updateCoverImagePath(itemId, null)
            gameDao.updateCover(itemId, null)
            _statusMessage.value = "Cover removed"
        }
    }

    fun rename(itemId: String, newTitle: String) {
        val clean = newTitle.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            gameDao.updateTitle(itemId, clean)
            _statusMessage.value = "Renamed"
        }
    }

    fun toggleFavorite(item: GameItem) {
        viewModelScope.launch(Dispatchers.IO) {
            gameDao.updateFavorite(item.id, !item.isFavorite)
        }
    }

    fun delete(item: GameItem) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val file = File(File(context.filesDir, "games"), item.fileName)
            if (file.exists()) file.delete()
            item.effectiveCover?.let { path ->
                val f = File(path)
                if (f.exists()) f.delete()
            }
            gameDao.deleteById(item.id)
            refreshStorageStats()
            _statusMessage.value = "\"${item.title}\" deleted"
        }
    }

    fun markOpened(item: GameItem) {
        viewModelScope.launch(Dispatchers.IO) {
            gameDao.updateLastPlayed(item.id, System.currentTimeMillis())
        }
    }

    fun addPlayTime(itemId: String, deltaMs: Long) {
        if (deltaMs <= 0L) return
        viewModelScope.launch(Dispatchers.IO) { gameDao.addPlayTime(itemId, deltaMs) }
    }

    // ------------------------------------------------------------------
    // Collections
    // ------------------------------------------------------------------

    fun createCollection(name: String, onCreated: ((String) -> Unit)? = null) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val id = UUID.randomUUID().toString()
            collectionDao.insert(
                CollectionItem(id = id, name = clean, createdAt = System.currentTimeMillis())
            )
            withContext(Dispatchers.Main) { onCreated?.invoke(id) }
            _statusMessage.value = "Collection \"$clean\" created"
        }
    }

    fun renameCollection(id: String, name: String) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) { collectionDao.updateName(id, clean) }
    }

    /** Deletes the collection only — the items inside are kept and unassigned. */
    fun deleteCollection(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            gameDao.clearCollection(id)
            collectionDao.deleteById(id)
            _statusMessage.value = "Collection deleted"
        }
    }

    fun setItemCollection(itemId: String, collectionId: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            gameDao.updateCollection(itemId, collectionId)
            _statusMessage.value = if (collectionId == null) "Removed from collection" else "Added to collection"
        }
    }

    // ------------------------------------------------------------------
    // Preferences
    // ------------------------------------------------------------------

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch { prefsRepo.setTheme(theme) }
    }

    fun setDefaultView(view: DefaultView) {
        viewModelScope.launch { prefsRepo.setDefaultView(view) }
    }

    fun setHaptics(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setHaptics(enabled) }
    }

    // ------------------------------------------------------------------
    // Storage / cache / backup
    // ------------------------------------------------------------------

    fun refreshStorageStats() {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            _storageBytes.value = BackupManager.dirSize(File(context.filesDir, "games")) +
                BackupManager.dirSize(File(context.filesDir, "covers"))
            _cacheBytes.value = BackupManager.dirSize(context.cacheDir) +
                BackupManager.dirSize(context.codeCacheDir)
        }
    }

    fun clearCache() {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            context.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
            refreshStorageStats()
            _statusMessage.value = "Cache cleared"
        }
    }

    /** Writes a full library ZIP (database rows + files) to [destination]. */
    fun exportBackup(destination: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = backupManager.export(
                destination = destination,
                items = gameDao.getAllOnce(),
                collections = collectionDao.getAllOnce()
            )
            _statusMessage.value = result.fold(
                onSuccess = { "Backup exported" },
                onFailure = { "Export failed: ${it.message}" }
            )
        }
    }

    /** Restores a library ZIP produced by [exportBackup]. Merges, never wipes. */
    fun importBackup(source: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = backupManager.import(source)
            result.onSuccess { payload ->
                payload.collections.forEach { collectionDao.insert(it) }
                payload.items.forEach { gameDao.insert(it) }
                refreshStorageStats()
            }
            _statusMessage.value = result.fold(
                onSuccess = { "Restored ${it.items.size} items" },
                onFailure = { "Import failed: ${it.message}" }
            )
        }
    }
}
