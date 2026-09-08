package com.example.converanything.native

import android.content.Context
import android.graphics.Bitmap
import android.os.ParcelFileDescriptor
import com.example.converanything.converter.ConverterClient
import com.example.converanything.core.ConversionJob
import com.example.converanything.core.ConversionType
import com.example.converanything.core.MediaItem
import com.example.converanything.core.ProbeInfo
import com.example.converanything.core.parseJobs
import com.example.converanything.core.parseProbe
import com.example.converanything.core.parseTypes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * Thin JNI wrapper over the C++ engine. All conversion logic (registry,
 * FFmpeg argv, queue, workers) lives in app/src/main/cpp/. Kotlin only
 * marshals paths/JSON and exposes StateFlows the UI collects.
 * Heavy ffmpeg runs execute in the :converter sandbox process.
 */
object EngineBridge {
    fun interface JobListener { fun onChanged(snapshotJson: String) }

    private external fun nativeTypes(): String
    private external fun nativeEnqueue(
        typeId: String, inputPath: String, outputPath: String, formatExt: String,
        batchId: String, sourceUri: String, sourceName: String, mimeType: String,
        workDir: String,
    ): String
    private external fun nativeCancel(jobId: String)
    private external fun nativeClearDone()
    private external fun nativeRemove(jobId: String)
    private external fun nativeSnapshot(): String
    private external fun nativeSetListener(listener: JobListener?)
    private external fun nativeProbe(path: String): String
    private external fun nativeDecodeThumb(path: String, maxDim: Int): Bitmap?
    private external fun nativeNotifyFinished(jobId: String, rc: Int, error: String)

    // Sandbox surface (called from :converter service + dispatch path).
    external fun nativeBuildArgs(
        typeId: String, input: String, output: String, ext: String, pixFmt: String,
        inputExt: String,
    ): Array<String>
    external fun nativeRunFfmpeg(args: Array<String>, progressPath: String, stderrPath: String, jobId: String): Int
    external fun nativeCancelFfmpeg(jobId: String)

    private val _types = MutableStateFlow<List<ConversionType>>(emptyList())
    val types: StateFlow<List<ConversionType>> = _types.asStateFlow()

    private val _jobs = MutableStateFlow<List<ConversionJob>>(emptyList())
    val jobs: StateFlow<List<ConversionJob>> = _jobs.asStateFlow()

    /** Input fds staged for the sandbox handoff (fd numbers are per-process). */
    private val pendingInputs = ConcurrentHashMap<String, Pair<ParcelFileDescriptor?, String>>()

    @Volatile private var ready = false
    private var workDir: String = ""
    private var appContext: Context? = null

    internal fun appContextForKill(): Context? = appContext

    /** Loads the library without touching engine state (safe in :converter too). */
    @Synchronized
    fun ensureLoaded() {
        try {
            System.loadLibrary("converanything")
        } catch (_: UnsatisfiedLinkError) {
            android.util.Log.e("EngineBridge", "native lib missing")
        }
    }

    /** Call once from Application.onCreate (main process). Idempotent. */
    @Synchronized
    fun init(context: Context) {
        if (ready) return
        ensureLoaded()
        appContext = context.applicationContext
        workDir = java.io.File(context.cacheDir, "engine").also { it.mkdirs() }.absolutePath
        _types.value = parseTypes(nativeTypes())
        pull()
        nativeSetListener(JobListener { snap ->
            _jobs.value = runCatching { parseJobs(snap, _types.value) }.getOrElse { _jobs.value }
        })
        ready = true
    }

    val isReady: Boolean get() = ready && _types.value.isNotEmpty()

    fun type(id: String): ConversionType = _types.value.first { it.id == id }

    fun enqueue(
        typeId: String,
        inputPath: String,
        outputPath: String,
        formatExt: String,
        batchId: String,
        source: MediaItem,
    ): String = nativeEnqueue(
        typeId, inputPath, outputPath, formatExt, batchId,
        source.uri.toString(), source.displayName, source.mimeType,
        workDir,
    )

    fun noteInput(jobId: String, pfd: ParcelFileDescriptor?, fallbackPath: String) {
        pendingInputs[jobId] = pfd to fallbackPath
    }

    internal fun takeInput(jobId: String): Pair<ParcelFileDescriptor?, String> {
        // ConvertApp.noteInput runs AFTER nativeEnqueue returns. The worker
        // can already be in dispatchRemote, so wait briefly rather than
        // sending the converter an empty path (batch job 2+ then fail).
        repeat(40) {
            pendingInputs.remove(jobId)?.let { return it }
            try { Thread.sleep(25) } catch (_: InterruptedException) { return null to "" }
        }
        return pendingInputs.remove(jobId) ?: (null to "")
    }

    fun cancel(jobId: String) {
        // The C++ poll loop forwards running cancels to the sandbox itself,
        // so a queued cancel can never kill its running sibling.
        nativeCancel(jobId)
    }

    fun clearDone() {
        nativeClearDone()
        pull()
    }

    fun remove(jobId: String) {
        nativeRemove(jobId)
        pull()
    }

    fun pull() {
        _jobs.value = runCatching { parseJobs(nativeSnapshot(), _types.value) }
            .getOrElse { _jobs.value }
    }

    /** libavformat probe of a file/fd path. Never throws — blank info on failure. */
    fun probe(path: String): ProbeInfo =
        runCatching { parseProbe(nativeProbe(path)) }.getOrElse { ProbeInfo() }

    /** First-frame decode for formats the platform thumbnailer can't render. */
    fun decodeThumb(path: String, maxDim: Int = 320): Bitmap? =
        runCatching { nativeDecodeThumb(path, maxDim) }.getOrNull()

    // --- Sandbox dispatch (called from native worker via JNI) ---
    @JvmStatic
    fun runRemote(
        jobId: String,
        typeId: String,
        formatExt: String,
        outputPath: String,
        progressPath: String,
        stderrPath: String,
        inputExt: String,
        inputPath: String,
    ): Boolean {
        val ctx = appContext ?: return false
        return ConverterClient.dispatch(
            ctx, jobId, typeId, formatExt, outputPath, progressPath, stderrPath, inputExt, inputPath)
    }

    @JvmStatic
    fun cancelRemote(jobId: String) = ConverterClient.cancel(jobId)

    @JvmStatic
    fun killConverter() = ConverterClient.killSandbox()

    fun onRemoteFinished(jobId: String, rc: Int, error: String) {
        runCatching { nativeNotifyFinished(jobId, rc, error) }
    }
}
