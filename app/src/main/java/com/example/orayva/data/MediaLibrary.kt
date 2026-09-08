package com.example.orayva.data

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Size
import android.webkit.MimeTypeMap
import com.example.orayva.native.EngineBridge
import com.example.orayva.core.MediaFilter
import com.example.orayva.core.MediaItem
import com.example.orayva.core.MediaKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * All device-media access. Picker (per-kind + [MediaFilter]) and Vault
 * (all kinds) share this; Vault just queries every kind. Inline grid
 * playback consumes [MediaItem.uri] directly (Media3/ExoPlayer later).
 */
class MediaLibrary(private val context: Context) {

    companion object {
        /** Gate: media access. Asked at first picker open, never at launch. */
        fun requiredPermissions(): Array<String> = if (Build.VERSION.SDK_INT >= 33) {
            arrayOf(
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.READ_MEDIA_IMAGES,
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        /** Requested alongside, but never blocks browsing when denied. */
        fun optionalPermissions(): Array<String> = if (Build.VERSION.SDK_INT >= 33) {
            arrayOf(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            emptyArray()
        }

        const val APP_DIR = "Orayva"
    }

    suspend fun query(kind: MediaKind, filter: MediaFilter = MediaFilter()): List<MediaItem> =
        withContext(Dispatchers.IO) { queryBlocking(kind).filter { filter.matches(it) } }

    /** Vault: every media type on device, newest first. Filter narrows per sub-tab. */
    suspend fun queryVault(filter: MediaFilter = MediaFilter(), kinds: Set<MediaKind> = enumValues<MediaKind>().toSet()): List<MediaItem> =
        withContext(Dispatchers.IO) {
            kinds.flatMap { queryBlocking(it) }
                .filter { filter.matches(it) }
                .sortedByDescending { it.dateAddedSec }
        }

    /** Re-resolves picker selections (ids) back to full items for the studio. */
    suspend fun lookup(ids: List<Long>, kind: MediaKind): List<MediaItem> = withContext(Dispatchers.IO) {
        val all = queryBlocking(kind).associateBy { it.id }
        ids.mapNotNull { all[it] }
    }

    /**
     * Builds a [MediaItem] from a SAF / DocumentsContract Uri so formats
     * MediaStore never indexes (PPM, DPX, QOI, JP2, TGA, MPEG, …) can still
     * enter the conversion pipeline. Takes persistable read permission when
     * the provider grants it.
     */
    suspend fun itemFromUri(uri: Uri, kind: MediaKind): MediaItem? = withContext(Dispatchers.IO) {
        runCatching {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: SecurityException) { }
            val name = queryDisplayName(uri) ?: uri.lastPathSegment
                ?.substringAfterLast('/')?.substringAfterLast(':') ?: "file"
            val mime = context.contentResolver.getType(uri)
                ?: MimeTypeMap.getSingleton()
                    .getMimeTypeFromExtension(name.substringAfterLast('.', "").lowercase())
                ?: fallbackMime(kind)
            val size = querySize(uri)
            MediaItem(
                id = uri.hashCode().toLong() and 0x7fffffffffffffffL,
                uriString = uri.toString(),
                displayName = name,
                mimeType = mime,
                kind = kind,
                sizeBytes = size,
            )
        }.getOrNull()
    }

    private fun queryDisplayName(uri: Uri): String? =
        runCatching {
            context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
                null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        }.getOrNull()

    private fun querySize(uri: Uri): Long =
        runCatching {
            context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.SIZE),
                null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getLong(0) else 0L
            } ?: 0L
        }.getOrDefault(0L)

    private fun fallbackMime(kind: MediaKind): String = when (kind) {
        MediaKind.VIDEO -> "video/*"
        MediaKind.AUDIO -> "audio/*"
        MediaKind.IMAGE -> "image/*"
    }

    // --- Output paths (§9): Downloads/Orayva/<Video|Audio|Image>/, originals never copied there ---
    // (All outputs go through tempOutputFile() + publishOutput()/copyToTree() below.)

    /** Copies a finished temp file into a user-picked tree folder. Returns its Uri. */
    suspend fun copyToTree(temp: File, folderUri: Uri, fileName: String, extension: String): Uri? =
        withContext(Dispatchers.IO) {
            val resolver = context.contentResolver
            val parent = DocumentsContract.buildDocumentUriUsingTree(
                folderUri, DocumentsContract.getTreeDocumentId(folderUri))
            val mime = MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(extension.lowercase()) ?: "application/octet-stream"
            val doc = DocumentsContract.createDocument(resolver, parent, mime, fileName) ?: return@withContext null
            val ok = runCatching {
                resolver.openOutputStream(doc)?.use { out ->
                    temp.inputStream().use { it.copyTo(out) }
                } ?: error("no stream")
            }.isSuccess
            if (ok) temp.delete() else runCatching { DocumentsContract.deleteDocument(resolver, doc) }
            if (ok) doc else null
        }

    fun scanFile(path: String) {
        android.media.MediaScannerConnection.scanFile(context, arrayOf(path), null, null)
    }

    // --- Scoped-storage production I/O -----------------------------------
    // FFmpeg gets REAL file paths only. Inputs arrive as fd paths (proven
    // working: probe + demux read content, no extension needed). Outputs MUST
    // be real temp files: muxers need an extension to pick the format and a
    // seekable target — /proc/self/fd paths fail muxer init. Publish after DONE.

    /** Unique temp file per job: ffmpeg's only output target. */
    fun tempOutputFile(sourceName: String, extension: String): File {
        val dir = File(context.cacheDir, "outputs").also { it.mkdirs() }
        val stem = sourceName.substringBeforeLast('.').ifBlank { "converted" }
        return File(dir, "$stem-${java.util.UUID.randomUUID()}.$extension")
    }

    fun openFd(uri: Uri, mode: String): ParcelFileDescriptor =
        context.contentResolver.openFileDescriptor(uri, mode)
            ?: error("Cannot open $uri")

    fun fdPath(pfd: ParcelFileDescriptor): String = "/proc/self/fd/${pfd.fd}"

    /**
     * Publishes a finished temp file to Downloads/Orayva/<subfolder>/.
     * MediaStore entry is created only on success — no partial files ever surface.
     */
    suspend fun publishOutput(subfolder: String, fileName: String, mime: String, temp: File): Uri? =
        withContext(Dispatchers.IO) {
            if (Build.VERSION.SDK_INT < 29) {
                val dir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "$APP_DIR/$subfolder").also { it.mkdirs() }
                var dest = File(dir, fileName)
                var n = 1
                while (dest.exists()) {
                    dest = File(dir, fileName.substringBeforeLast('.') +
                        " (${n++})." + fileName.substringAfterLast('.', "bin"))
                }
                if (!temp.renameTo(dest)) {
                    runCatching {
                        temp.inputStream().use { input ->
                            dest.outputStream().use { output -> input.copyTo(output) }
                        }
                    }
                    if (dest.exists()) temp.delete() else return@withContext null
                }
                scanFile(dest.absolutePath)
                return@withContext Uri.fromFile(dest)
            }
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, mime)
                put(MediaStore.Downloads.RELATIVE_PATH, "Download/$APP_DIR/$subfolder")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = context.contentResolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return@withContext null
            val ok = runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    temp.inputStream().use { it.copyTo(out) }
                } ?: error("no stream")
            }.isSuccess
            if (ok) {
                context.contentResolver.update(uri, ContentValues().apply {
                    put(MediaStore.Downloads.IS_PENDING, 0)
                }, null, null)
                temp.delete()
                uri
            } else {
                runCatching { context.contentResolver.delete(uri, null, null) }
                null
            }
        }

    /** Frame rate + authoritative duration for Studio metadata (fd opened briefly). */
    suspend fun probeVideo(uriString: String): com.example.orayva.core.ProbeInfo =
        withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openFileDescriptor(Uri.parse(uriString), "r")?.use { pfd ->
                    EngineBridge.probe(fdPath(pfd))
                } ?: com.example.orayva.core.ProbeInfo()
            }.getOrElse { com.example.orayva.core.ProbeInfo() }
        }

    // --- Custom-destination preview (§5.4) ----------------------------------
    data class TreePreview(val displayName: String, val childCount: Int, val imageUris: List<Uri>)

    suspend fun treePreview(treeUri: Uri): TreePreview = withContext(Dispatchers.IO) {
        val name = treeUri.lastPathSegment?.substringAfterLast(':')
            ?.substringAfterLast('/')?.ifBlank { "Selected folder" } ?: "Selected folder"
        runCatching {
            val parentId = DocumentsContract.getTreeDocumentId(treeUri)
            val children = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentId)
            val uris = mutableListOf<Uri>()
            var count = 0
            context.contentResolver.query(
                children,
                arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_MIME_TYPE),
                null, null, null)?.use { c ->
                while (c.moveToNext()) {
                    count++
                    if (uris.size < 4 && (c.getString(1)?.startsWith("image/") == true)) {
                        uris += DocumentsContract.buildDocumentUriUsingTree(treeUri, c.getString(0))
                    }
                }
            }
            TreePreview(name, count, uris)
        }.getOrElse { TreePreview(name, 0, emptyList()) }
    }

    suspend fun thumbnailForUri(uri: Uri, width: Int = 160, height: Int = 160): Bitmap? =
        withContext(Dispatchers.IO) {
            platformThumbnail(uri.toString(), width, height)
                ?: ffmpegThumbnail(uri.toString(), width)
        }

    /** Grid-cell thumbnail. Platform first, ffmpeg decode fallback for exotic formats. */
    suspend fun thumbnail(item: MediaItem, width: Int = 320, height: Int = 320): Bitmap? =
        withContext(Dispatchers.IO) {
            platformThumbnail(item.uriString, width, height)
                ?: ffmpegThumbnail(item.uriString, width)
        }

    private fun platformThumbnail(uriString: String, width: Int, height: Int): Bitmap? =
        runCatching {
            if (Build.VERSION.SDK_INT >= 29) {
                context.contentResolver.loadThumbnail(Uri.parse(uriString), Size(width, height), null)
            } else null
        }.getOrNull()

    /** First-frame decode via our own FFmpeg (tiff, jp2, dpx, avif...). */
    private fun ffmpegThumbnail(uriString: String, width: Int): Bitmap? = runCatching {
        context.contentResolver.openFileDescriptor(Uri.parse(uriString), "r")?.use { pfd ->
            EngineBridge.decodeThumb("/proc/self/fd/${pfd.fd}", width)
        }
    }.getOrNull()

    /** FFmpeg needs a file path; MediaStore gives content Uris -> stage into cache. */
    suspend fun inputPathFor(item: MediaItem): String = withContext(Dispatchers.IO) {
        // Unique per call: callers delete after use. Staging is the rare path
        // (fd inputs that fail the probe), so no cache bookkeeping needed.
        val staged = File(context.cacheDir,
            "inputs/${item.id}_${java.util.UUID.randomUUID()}_${item.displayName}")
        staged.parentFile?.mkdirs()
        context.contentResolver.openInputStream(item.uri)?.use { input ->
            staged.outputStream().use { input.copyTo(it) }
        }
        staged.absolutePath
    }

    // --- MediaStore scan ---
    private fun queryBlocking(kind: MediaKind): List<MediaItem> {
        val (collection, projection, sort) = when (kind) {
            MediaKind.VIDEO -> Triple(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                arrayOf(
                    MediaStore.Video.Media._ID, MediaStore.Video.Media.DISPLAY_NAME,
                    MediaStore.Video.Media.MIME_TYPE, MediaStore.Video.Media.SIZE,
                    MediaStore.Video.Media.DURATION, MediaStore.Video.Media.WIDTH,
                    MediaStore.Video.Media.HEIGHT, MediaStore.Video.Media.DATE_ADDED,
                ),
                "${MediaStore.Video.Media.DATE_ADDED} DESC",
            )
            MediaKind.AUDIO -> Triple(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                arrayOf(
                    MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DISPLAY_NAME,
                    MediaStore.Audio.Media.MIME_TYPE, MediaStore.Audio.Media.SIZE,
                    MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.DATE_ADDED,
                ),
                "${MediaStore.Audio.Media.DATE_ADDED} DESC",
            )
            MediaKind.IMAGE -> Triple(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                arrayOf(
                    MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME,
                    MediaStore.Images.Media.MIME_TYPE, MediaStore.Images.Media.SIZE,
                    MediaStore.Images.Media.WIDTH, MediaStore.Images.Media.HEIGHT,
                    MediaStore.Images.Media.DATE_ADDED,
                ),
                "${MediaStore.Images.Media.DATE_ADDED} DESC",
            )
        }
        val out = mutableListOf<MediaItem>()
        val cursor = runCatching {
            context.contentResolver.query(collection, projection, null, null, sort)
        }.getOrNull()
        if (cursor == null) {
            android.util.Log.w("MediaLibrary", "query($kind): null cursor (permission missing?)")
            return out
        }
        cursor.use { c ->
            fun col(name: String) = c.getColumnIndexOrThrow(name)
            // Width/height/duration columns differ per kind; resolve lazily per row.
            while (c.moveToNext()) {
                val id = c.getLong(col("_id"))
                val uri: Uri = ContentUris.withAppendedId(collection, id)
                fun str(name: String) = runCatching { c.getString(col(name)) }.getOrNull() ?: ""
                fun long(name: String) = runCatching { c.getLong(col(name)) }.getOrNull() ?: 0L
                fun int(name: String) = runCatching { c.getInt(col(name)) }.getOrNull() ?: 0
                out += MediaItem(
                    id = id,
                    uriString = uri.toString(),
                    displayName = str("_display_name").ifBlank { "media_$id" },
                    mimeType = str("mime_type"),
                    kind = kind,
                    sizeBytes = long("_size"),
                    durationMs = long("duration"),
                    width = int("width"),
                    height = int("height"),
                    dateAddedSec = long("date_added"),
                )
            }
        }
        android.util.Log.d("MediaLibrary", "query($kind): ${out.size} rows")
        return out
    }
}
