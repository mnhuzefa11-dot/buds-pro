package com.budspro.app.util

import android.content.Context
import android.net.Uri
import com.budspro.app.data.CollectionItem
import com.budspro.app.data.GameItem
import com.budspro.app.data.effectiveCover
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Exports and restores the whole library as a single ZIP archive.
 *
 * Layout of the archive:
 *
 *   manifest.json          – all GameItem + CollectionItem rows
 *   games/<fileName>       – the imported content files
 *   covers/<fileName>      – cover images
 *
 * Nothing here touches the running database directly; the ViewModel decides
 * what to do with the parsed payload, so a corrupt archive can never leave the
 * app in a half-migrated state.
 */
class BackupManager(private val context: Context) {

    data class Payload(
        val items: List<GameItem>,
        val collections: List<CollectionItem>
    )

    fun export(
        destination: Uri,
        items: List<GameItem>,
        collections: List<CollectionItem>
    ): Result<Unit> = runCatching {
        val gamesDir = File(context.filesDir, "games")
        val coversDir = File(context.filesDir, "covers")

        val out = context.contentResolver.openOutputStream(destination)
            ?: error("Cannot open destination")

        ZipOutputStream(out.buffered()).use { zip ->
            zip.putNextEntry(ZipEntry(MANIFEST))
            zip.write(buildManifest(items, collections).toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            items.forEach { item ->
                val f = File(gamesDir, item.fileName)
                if (f.exists() && f.isFile) writeEntry(zip, "games/${item.fileName}", f)

                item.effectiveCover?.let { coverPath ->
                    val cover = File(coverPath)
                    if (cover.exists() && cover.isFile) {
                        writeEntry(zip, "covers/${cover.name}", cover)
                    }
                }
            }

            // Any stray covers not referenced by a row are still included so a
            // restore is byte-for-byte faithful.
            coversDir.listFiles()?.forEach { cover ->
                if (cover.isFile) runCatching { writeEntry(zip, "covers/${cover.name}", cover) }
            }
        }
    }

    fun import(source: Uri): Result<Payload> = runCatching {
        val gamesDir = File(context.filesDir, "games").apply { mkdirs() }
        val coversDir = File(context.filesDir, "covers").apply { mkdirs() }

        var manifestJson: String? = null

        val input = context.contentResolver.openInputStream(source)
            ?: error("Cannot open backup file")

        ZipInputStream(input.buffered()).use { zip ->
            var entry: ZipEntry? = zip.nextEntry
            while (entry != null) {
                val name = entry.name
                when {
                    name == MANIFEST -> manifestJson = zip.readBytes().toString(Charsets.UTF_8)

                    name.startsWith("games/") && !entry.isDirectory ->
                        extractTo(zip, gamesDir, name.removePrefix("games/"))

                    name.startsWith("covers/") && !entry.isDirectory ->
                        extractTo(zip, coversDir, name.removePrefix("covers/"))
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }

        val json = manifestJson ?: error("Backup is missing its manifest")
        parseManifest(json, coversDir)
    }

    // ------------------------------------------------------------------

    private fun writeEntry(zip: ZipOutputStream, entryName: String, file: File) {
        zip.putNextEntry(ZipEntry(entryName))
        file.inputStream().use { it.copyTo(zip) }
        zip.closeEntry()
    }

    /** Zip-slip safe extraction: the resolved path must stay inside [dir]. */
    private fun extractTo(zip: ZipInputStream, dir: File, rawName: String) {
        val safeName = File(rawName).name
        if (safeName.isBlank()) return
        val target = File(dir, safeName)
        if (!target.canonicalPath.startsWith(dir.canonicalPath + File.separator)) return
        target.outputStream().use { zip.copyTo(it) }
    }

    private fun buildManifest(items: List<GameItem>, collections: List<CollectionItem>): String {
        val root = JSONObject()
        root.put("version", BACKUP_VERSION)
        root.put("exportedAt", System.currentTimeMillis())

        val itemArray = JSONArray()
        items.forEach { item ->
            val o = JSONObject()
            o.put("id", item.id)
            o.put("title", item.title)
            o.put("type", item.type)
            o.put("fileName", item.fileName)
            o.put("fileSize", item.fileSize)
            o.put("addedAt", item.addedAt)
            o.put("lastPlayedAt", item.lastPlayedAt ?: JSONObject.NULL)
            o.put("progress", item.progress)
            o.put("isFavorite", item.isFavorite)
            o.put("coverFileName", item.effectiveCover?.let { File(it).name } ?: JSONObject.NULL)
            o.put("folderId", item.folderId ?: JSONObject.NULL)
            o.put("tags", item.tags ?: JSONObject.NULL)
            o.put("collectionId", item.collectionId ?: JSONObject.NULL)
            o.put("totalPlayTime", item.totalPlayTime)
            itemArray.put(o)
        }
        root.put("items", itemArray)

        val collectionArray = JSONArray()
        collections.forEach { c ->
            val o = JSONObject()
            o.put("id", c.id)
            o.put("name", c.name)
            o.put("createdAt", c.createdAt)
            o.put("coverFileName", c.coverImagePath?.let { File(it).name } ?: JSONObject.NULL)
            collectionArray.put(o)
        }
        root.put("collections", collectionArray)

        return root.toString()
    }

    private fun parseManifest(json: String, coversDir: File): Payload {
        val root = JSONObject(json)

        val items = mutableListOf<GameItem>()
        val itemArray = root.optJSONArray("items") ?: JSONArray()
        for (i in 0 until itemArray.length()) {
            val o = itemArray.getJSONObject(i)
            val coverName = o.optStringOrNull("coverFileName")
            val coverPath = coverName?.let { File(coversDir, it).absolutePath }
            items += GameItem(
                id = o.getString("id"),
                title = o.optString("title", "Untitled"),
                type = o.optString("type", "html"),
                fileName = o.optString("fileName", ""),
                fileSize = o.optLong("fileSize", 0L),
                addedAt = o.optLong("addedAt", System.currentTimeMillis()),
                lastPlayedAt = if (o.isNull("lastPlayedAt")) null else o.optLong("lastPlayedAt"),
                progress = o.optInt("progress", 0),
                isFavorite = o.optBoolean("isFavorite", false),
                coverPath = coverPath,
                folderId = o.optStringOrNull("folderId"),
                tags = o.optStringOrNull("tags"),
                coverImagePath = coverPath,
                collectionId = o.optStringOrNull("collectionId"),
                totalPlayTime = o.optLong("totalPlayTime", 0L)
            )
        }

        val collections = mutableListOf<CollectionItem>()
        val collectionArray = root.optJSONArray("collections") ?: JSONArray()
        for (i in 0 until collectionArray.length()) {
            val o = collectionArray.getJSONObject(i)
            val coverName = o.optStringOrNull("coverFileName")
            collections += CollectionItem(
                id = o.getString("id"),
                name = o.optString("name", "Collection"),
                createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                coverImagePath = coverName?.let { File(coversDir, it).absolutePath }
            )
        }

        return Payload(items = items.filter { it.fileName.isNotBlank() }, collections = collections)
    }

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }

    companion object {
        private const val MANIFEST = "manifest.json"
        private const val BACKUP_VERSION = 1

        fun dirSize(dir: File): Long {
            if (!dir.exists()) return 0L
            if (dir.isFile) return dir.length()
            return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        }

        fun suggestedBackupName(): String {
            val stamp = java.text.SimpleDateFormat("yyyyMMdd-HHmm", java.util.Locale.US)
                .format(java.util.Date())
            return "BudsPro-backup-$stamp.zip"
        }
    }
}
