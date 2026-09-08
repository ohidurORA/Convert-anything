package com.example.converanything.data

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import androidx.core.content.ContextCompat
import android.webkit.MimeTypeMap
import com.example.converanything.core.ConversionJob
import com.example.converanything.core.Destination
import com.example.converanything.core.JobStatus
import com.example.converanything.core.MediaItem
import com.example.converanything.core.OutputFormat
import com.example.converanything.core.outputFileName
import com.example.converanything.native.EngineBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Owns the engine connection and per-job I/O.
 * ffmpeg always reads a real-or-fd input and writes a real temp file with a
 * proper extension (muxers can't init on fd paths). Publishing to
 * MediaStore/tree happens only after DONE — failures leave no traces.
 */
class ConvertApp : Application() {
    lateinit var library: MediaLibrary
        private set
    val history: QueryHistory by lazy { QueryHistory(this) }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val destinations = ConcurrentHashMap<String, Destination>()
    private val handles = ConcurrentHashMap<String, IoHandle>()
    private val finalized = ConcurrentHashMap.newKeySet<String>()
    // User-visible enqueue failures (a silent Log is how "nothing happens" reports start).
    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val errors: SharedFlow<String> = _errors.asSharedFlow()

    private data class IoHandle(
        val inputPfd: ParcelFileDescriptor?,
        val stagedInput: File?,
        val tempFile: File,
        val fileName: String,
        val mime: String,
        val subfolder: String,
    )

    override fun onCreate() {
        super.onCreate()
        library = MediaLibrary(this)
        EngineBridge.init(this)
        // Ensure the required folder structure exists under Downloads/Orayva
        // so that even if the user deletes the app folder, it gets recreated on first convert.
        val appDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), MediaLibrary.APP_DIR)
        appDir.mkdirs()
        listOf("video", "audio", "images").forEach { sub ->
            File(appDir, sub).mkdirs()
        }
        scope.launch {
            // Sweep stale per-job scratch (killed runs leave temps behind).
            runCatching {
                listOf("outputs", "inputs", "engine").forEach { dir ->
                    File(cacheDir, dir).listFiles()?.forEach { it.deleteRecursively() }
                }
            }
        }
        scope.launch {
            EngineBridge.jobs.collect { jobs ->
                jobs.filter {
                    (it.status == JobStatus.DONE || it.status == JobStatus.FAILED) &&
                        finalized.add(it.id)
                }.forEach {
                    // Bulkhead: one poison job must never kill this collector —
                    // a dead collector means no publish/history ever again while
                    // the app looks perfectly alive ("works once, then nothing").
                    runCatching { finalize(it) }.onFailure { e ->
                        android.util.Log.e("ConvertApp", "finalize failed for ${it.id}", e)
                        _errors.tryEmit("Couldn't finish conversion: ${e.message ?: "unknown error"}")
                    }
                }
            }
        }
    }

    /** Enqueues one engine job per item; batch items share a batchId for Queries grouping. */
    fun enqueue(
        typeId: String,
        items: List<MediaItem>,
        formatExt: String,
        destination: Destination,
    ) {
        val batchId = if (items.size > 1) UUID.randomUUID().toString() else ""
        scope.launch {
            for (item in items) {
                try {
                    // --- input: fd path when readable, staged file otherwise ---
                    var inputPfd: ParcelFileDescriptor? = null
                    var inputPath: String? = null
                    if (Build.VERSION.SDK_INT >= 29) {
                        runCatching {
                            val pfd = library.openFd(item.uri, "r")
                            val fdPath = library.fdPath(pfd)
                            // ponytail: probe-then-stage; cloud/pipe Uris fail the
                            // probe and fall back to a staged copy. Upgrade path:
                            // SAF chunk streaming if staging proves too slow.
                            if (EngineBridge.probe(fdPath).let { it.hasVideo || it.hasAudio }) {
                                inputPfd = pfd
                                inputPath = fdPath
                            } else {
                                pfd.close()
                            }
                        }
                    }
                    if (inputPath == null) {
                        inputPfd?.close()
                        inputPfd = null
                        inputPath = library.inputPathFor(item)
                    }
                    val staged: File? = if (inputPfd == null) File(inputPath) else null
                    // --- output: always a real temp file (see MediaLibrary note) ---
                    // The temp file and the eventual published file must use the
                    // format's *container* extension (HEVC→mp4, ALAC→m4a, else the
                    // advertised one), NOT the bare advertised extension. If they
                    // diverge, FFmpeg writes the real container file while we try
                    // to publish a temp with the advertised extension that was
                    // never created — a "conversion succeeded but nothing saved"
                    // false FAILURE. The container mapping is the single source of
                    // truth mirrored from the C++ registry's "container" field.
                    val fmt = EngineBridge.type(typeId).outputFormats
                        .firstOrNull { it.extension.equals(formatExt, ignoreCase = true) }
                        ?: OutputFormat(formatExt, formatExt.uppercase(), "")
                    val outExt = fmt.container
                    val fileName = uniqueOutputName(outputFileName(item.displayName, outExt), items, item)
                    val mime = MimeTypeMap.getSingleton()
                        .getMimeTypeFromExtension(outExt.lowercase()) ?: "application/octet-stream"
                    val temp = library.tempOutputFile(item.displayName, outExt)
                    val jobId = EngineBridge.enqueue(
                        typeId, inputPath!!, temp.absolutePath, formatExt.lowercase(), batchId, item)
                    EngineBridge.noteInput(jobId, inputPfd, inputPath)
                    destinations[jobId] = destination
                    handles[jobId] = IoHandle(
                        inputPfd, staged, temp, fileName, mime,
                        EngineBridge.type(typeId).outputSubfolder)
                } catch (e: Exception) {
                    android.util.Log.e("ConvertApp", "enqueue failed", e)
                    _errors.tryEmit("Couldn't start conversion: ${e.message ?: "unknown error"}")
                }
            }
            startConversionService()
        }
    }

    private suspend fun finalize(job: ConversionJob) {
        val dest = destinations.remove(job.id)
        val h = handles.remove(job.id)
        h?.inputPfd?.closeQuietly()
        // Publish BEFORE recording: a DONE row with no file in Downloads is a
        // lie. If the save fails, the history row says FAILED with the reason.
        var final = job
        if (job.status == JobStatus.FAILED || h == null) {
            h?.tempFile?.delete()
            h?.stagedInput?.delete()
        } else {
            val saved = when (dest) {
                is Destination.Default -> {
                    library.publishOutput(h.subfolder, h.fileName, h.mime, h.tempFile) != null
                }
                is Destination.Custom -> {
                    if (h.tempFile.exists()) {
                        val uri = library.copyToTree(h.tempFile, dest.folderUri, h.fileName,
                            h.fileName.substringAfterLast('.', "bin"))
                        h.tempFile.delete()
                        uri != null
                    } else false
                }
                null -> {
                    h.tempFile.delete()
                    false
                }
            }
            h.stagedInput?.delete()
            if (!saved) {
                android.util.Log.e("ConvertApp", "publish failed for ${job.id}")
                h.tempFile.delete()
                final = job.copy(
                    status = JobStatus.FAILED,
                    error = "Couldn't save to Downloads/Orayva/${h.subfolder}/",
                )
            }
        }
        history.record(final)
        EngineBridge.remove(job.id)
    }

    /**
     * Batch items that share a stem (IMG_0001.jpg × N) must not publish to the
     * same Downloads name. Index only when a collision actually exists.
     */
    private fun uniqueOutputName(fileName: String, items: List<MediaItem>, item: MediaItem): String {
        val stem = fileName.substringBeforeLast('.')
        val ext = fileName.substringAfterLast('.', "")
        val same = items.count {
            outputFileName(it.displayName, ext).equals(fileName, ignoreCase = true)
        }
        if (same <= 1) return fileName
        val idx = items.filter {
            outputFileName(it.displayName, ext).equals(fileName, ignoreCase = true)
        }.indexOfFirst { it.id == item.id }.coerceAtLeast(0) + 1
        return "$stem-$idx.$ext"
    }

    private fun startConversionService() {
        // API 34+ users can deny dataSync FGS: degrade to background execution, never crash.
        runCatching {
            ContextCompat.startForegroundService(this, Intent(this, ConversionService::class.java))
        }.onFailure { android.util.Log.w("ConvertApp", "foreground service denied", it) }
    }

    private fun ParcelFileDescriptor.closeQuietly() = runCatching { close() }
}