package com.budspro.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.budspro.app.data.AppDatabase
import com.budspro.app.data.GameItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class GameViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.getInstance(app).gameDao()

    val games: StateFlow<List<GameItem>> = dao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Reads straight from the picked file's stream and copies it into the
    // app's own storage immediately — no lingering dependency on the
    // original URI, no permission re-prompts, no flaky picker behavior.
    fun importFile(uri: Uri, displayName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val ext = displayName.substringAfterLast('.', "").lowercase()
            val type = when (ext) {
                "html", "htm" -> "html"
                "pdf" -> "pdf"
                "json" -> "json"
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
            dao.deleteById(item.id)
        }
    }

    fun toggleFavorite(item: GameItem) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.updateFavorite(item.id, !item.isFavorite)
        }
    }

    /**
     * Called right before an item is handed to PlayerActivity so the Recent
     * tab is accurate for every file type (HTML, PDF and JSON alike).
     * Purely a timestamp write — opening/playing logic is untouched.
     */
    fun markOpened(item: GameItem) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.updateLastPlayed(item.id, System.currentTimeMillis())
        }
    }

    // ---- Derived lists for the Saves / Recent tabs -------------------------
    // These are just filtered views of the same `games` flow, so nothing new
    // is stored and nothing existing changes.

    /** Favourites, newest first. */
    val favorites: StateFlow<List<GameItem>> = games
        .map { list -> list.filter { it.isFavorite } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Anything that has been opened at least once, most recent first. */
    val recent: StateFlow<List<GameItem>> = games
        .map { list ->
            list.filter { it.lastPlayedAt != null }
                .sortedByDescending { it.lastPlayedAt ?: 0L }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
