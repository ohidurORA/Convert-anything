package com.example.converanything.data

import android.content.Context
import com.example.converanything.core.ConversionJob
import com.example.converanything.core.JobStatus
import com.example.converanything.core.MediaItem
import com.example.converanything.core.MediaKind
import com.example.converanything.core.OutputFormat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Persistent Queries history. One small JSON file (capped at 100 terminal
 * jobs, metadata only — no bitmaps, kilobytes total). Live engine jobs take
 * precedence; history fills in everything from previous runs.
 */
class QueryHistory(context: Context) {
    private val file = File(context.filesDir, "query-history.json")
    private val _entries = MutableStateFlow<List<ConversionJob>>(emptyList())
    val entries: StateFlow<List<ConversionJob>> = _entries.asStateFlow()

    companion object { const val MAX = 100 }

    init {
        _entries.value = runCatching {
            if (!file.exists()) return@runCatching emptyList()
            // Per-entry tolerance: one corrupt entry (killed mid-write, old
            // schema) must never wipe the whole history.
            val arr = JSONArray(file.readText())
            val kept = mutableListOf<ConversionJob>()
            for (i in 0 until arr.length()) {
                runCatching { kept += fromJson(arr.getJSONObject(i)) }
                    .onFailure { android.util.Log.w("QueryHistory", "dropping bad entry $i") }
            }
            android.util.Log.d("QueryHistory", "loaded ${kept.size}/${arr.length()}")
            kept
        }.getOrElse {
            android.util.Log.w("QueryHistory", "history file unreadable, starting fresh", it)
            emptyList()
        }
    }

    @Synchronized
    fun record(job: ConversionJob) {
        if (job.status != JobStatus.DONE && job.status != JobStatus.FAILED) return
        _entries.value = listOf(job) + _entries.value.filter { it.id != job.id }.take(MAX - 1)
        save()
    }

    @Synchronized
    fun remove(id: String) {
        _entries.value = _entries.value.filter { it.id != id }
        save()
    }

    @Synchronized
    fun clear() {
        _entries.value = emptyList()
        runCatching { file.delete() }
    }

    private fun save() {
        // Atomic write: a kill mid-save leaves either the old or the new file,
        // never a truncation that wipes history on next launch.
        runCatching {
            val tmp = File(file.parent, file.name + ".tmp")
            val arr = JSONArray()
            _entries.value.forEach { arr.put(toJson(it)) }
            tmp.writeText(arr.toString())
            if (!tmp.renameTo(file)) {
                file.delete()
                tmp.renameTo(file)
            }
        }.onFailure { android.util.Log.w("QueryHistory", "save failed", it) }
    }

    private fun toJson(j: ConversionJob) = JSONObject().apply {
        put("id", j.id)
        put("typeId", j.typeId)
        put("batchId", j.batchId)
        put("status", j.status.name)
        put("progress", j.progress)
        put("error", j.error ?: "")
        put("formatExt", j.outputFormat.extension)
        put("formatLabel", j.outputFormat.label)
        put("formatMime", j.outputFormat.mimeType)
        put("srcUri", j.source.uriString)
        put("srcName", j.source.displayName)
        put("srcMime", j.source.mimeType)
        put("srcKind", j.source.kind.name)
    }

    private fun fromJson(o: JSONObject) = ConversionJob(
        id = o.getString("id"),
        typeId = o.getString("typeId"),
        source = MediaItem(
            id = -1,
            uriString = o.optString("srcUri"),
            displayName = o.optString("srcName", "unknown"),
            mimeType = o.optString("srcMime"),
            kind = runCatching { MediaKind.valueOf(o.optString("srcKind")) }
                .getOrElse { MediaKind.VIDEO },
        ),
        outputFormat = OutputFormat(
            o.optString("formatExt"), o.optString("formatLabel"), o.optString("formatMime")),
        batchId = o.optString("batchId"),
        status = runCatching { JobStatus.valueOf(o.optString("status")) }
            .getOrElse { JobStatus.FAILED },
        progress = o.optInt("progress"),
        error = o.optString("error").ifBlank { null },
    )
}
