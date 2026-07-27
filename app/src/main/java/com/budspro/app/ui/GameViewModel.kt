package com.budspro.app.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.budspro.app.data.AppDatabase
import com.budspro.app.data.Folder
import com.budspro.app.data.GameItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class GameViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.getInstance(app).gameDao()
    private val folderDao = AppDatabase.getInstance(app).folderDao()

    val games: StateFlow<List<GameItem>> = dao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val folders: StateFlow<List<Folder>> = folderDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun importFile(uri: Uri, displayName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val ext = displayName.substringAfterLast('.', "").lowercase()
            val type = when (ext) {
                "html", "htm" -> "html"
                "pdf" -> "pdf"
                "json" -> "json"
                "jpg", "jpeg", "png", "webp" -> "image"
                else -> return@launch
            }
            val id = UUID.randomUUID().toString()
            val gamesDir = File(context.filesDir, "games").apply { mkdirs() }
            val destFile = File(gamesDir, "$id.$ext")
            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output -> input.copyTo(output) }
            } ?: return@launch
            val title = displayName.substringBeforeLast('.').ifBlank { "Untitled" }
            dao.insert(
                GameItem(
                    id = id,
                    title = title,
                    type = type,
                    fileName = destFile.name,
                    fileSize = destFile.length(),
                    addedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteGame(item: GameItem) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val file = File(File(context.filesDir, "games"), item.fileName)
            if (file.exists()) file.delete()
            if (item.coverPath != null) {
                val coverFile = File(context.filesDir, item.coverPath)
                if (coverFile.exists()) coverFile.delete()
            }
            dao.deleteById(item.id)
        }
    }

    fun toggleFavorite(item: GameItem) {
        viewModelScope.launch(Dispatchers.IO) { dao.updateFavorite(item.id, !item.isFavorite) }
    }

    fun markOpened(item: GameItem) {
        viewModelScope.launch(Dispatchers.IO) { dao.updateLastPlayed(item.id, System.currentTimeMillis()) }
    }

    fun renameItem(id: String, newTitle: String) {
        viewModelScope.launch(Dispatchers.IO) { dao.updateTitle(id, newTitle) }
    }

    fun setCoverImage(itemId: String, sourceUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val coversDir = File(context.filesDir, "covers").apply { mkdirs() }
            val coverFile = File(coversDir, "$itemId.jpg")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                coverFile.outputStream().use { output -> input.copyTo(output) }
            } ?: return@launch
            dao.updateCover(itemId, coverFile.absolutePath)
        }
    }

    fun removeCover(itemId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val item = dao.getById(itemId) ?: return@launch
            if (item.coverPath != null) {
                val file = File(context.filesDir, item.coverPath)
                if (file.exists()) file.delete()
            }
            dao.updateCover(itemId, null)
        }
    }

    fun moveToFolder(itemId: String, folderId: String?) {
        viewModelScope.launch(Dispatchers.IO) { dao.updateFolder(itemId, folderId) }
    }

    fun createFolder(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            folderDao.insert(Folder(id = UUID.randomUUID().toString(), name = name, createdAt = System.currentTimeMillis()))
        }
    }

    fun renameFolder(id: String, name: String) {
        viewModelScope.launch(Dispatchers.IO) { folderDao.updateName(id, name) }
    }

    fun deleteFolder(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val items = dao.getItemsInFolder(id)
            items.forEach { dao.updateFolder(it.id, null) }
            folderDao.deleteById(id)
        }
    }

    val favorites: StateFlow<List<GameItem>> = games
        .map { it.filter { item -> item.isFavorite } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recent: StateFlow<List<GameItem>> = games
        .map { it.filter { item -> item.lastPlayedAt != null }.sortedByDescending { item -> item.lastPlayedAt ?: 0L } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateTags(id: String, tags: String?) {
        viewModelScope.launch(Dispatchers.IO) { dao.updateTags(id, tags) }
    }

    fun updateProgressValue(id: String, progress: Int) {
        viewModelScope.launch(Dispatchers.IO) { dao.updateProgress(id, progress, System.currentTimeMillis()) }
    }

    private val annotationDao = AppDatabase.getInstance(app).studyAnnotationDao()

    fun getAnnotations(gameId: String): Flow<List<com.budspro.app.data.StudyAnnotation>> =
        annotationDao.getByGameId(gameId)

    fun insertAnnotation(annotation: com.budspro.app.data.StudyAnnotation) {
        viewModelScope.launch(Dispatchers.IO) { annotationDao.insert(annotation) }
    }

    fun deleteAnnotation(id: String) {
        viewModelScope.launch(Dispatchers.IO) { annotationDao.deleteById(id) }
    }

    fun deleteAnnotationsForGame(gameId: String) {
        viewModelScope.launch(Dispatchers.IO) { annotationDao.deleteByGameId(gameId) }
    }
}
