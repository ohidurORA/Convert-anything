package com.example.converanything.core

import android.net.Uri
import org.json.JSONArray

/** One device media row from MediaStore. Covers picker, batch strip, vault grid. */
data class MediaItem(
    val id: Long,
    /** String so pure-JVM tests can construct items; parsed lazily for Android APIs. */
    val uriString: String,
    val displayName: String,
    val mimeType: String,
    val kind: MediaKind,
    val sizeBytes: Long = 0,
    val durationMs: Long = 0, // video/audio only
    val width: Int = 0, // video/image only
    val height: Int = 0,
    val dateAddedSec: Long = 0,
) {
    val uri: Uri get() = Uri.parse(uriString)
    val extension: String get() = displayName.substringAfterLast('.', "").lowercase()
    val resolutionBucket: String
        get() {
            val h = maxOf(width, height)
            return when {
                h >= 2160 -> "4K"
                h >= 1080 -> "1080p"
                h >= 720 -> "720p"
                h > 0 -> "SD"
                else -> "Unknown"
            }
        }
}

/** Generic picker/vault filter. New facet = one more nullable field, no UI rewrite. */
data class MediaFilter(
    val query: String = "",
    val resolutionBuckets: Set<String> = emptySet(), // e.g. setOf("4K", "1080p")
    val extensions: Set<String> = emptySet(), // e.g. setOf("mp4", "mov")
    val minDurationMs: Long = 0,
) {
    fun matches(item: MediaItem): Boolean {
        if (query.isNotBlank() &&
            !item.displayName.contains(query, ignoreCase = true) &&
            !item.mimeType.contains(query, ignoreCase = true)
        ) return false
        if (resolutionBuckets.isNotEmpty() && item.resolutionBucket !in resolutionBuckets) return false
        if (extensions.isNotEmpty() && item.extension !in extensions) return false
        if (item.durationMs < minDurationMs) return false
        return true
    }
}

enum class JobStatus { QUEUED, RUNNING, DONE, FAILED }

/** libavformat probe result (Studio metadata + engine progress math). */
data class ProbeInfo(
    val durationMs: Long = 0,
    val width: Int = 0,
    val height: Int = 0,
    val fps: Double = 0.0,
    val pixFmt: String = "",
    val hasVideo: Boolean = false,
    val hasAudio: Boolean = false,
)

fun parseProbe(json: String): ProbeInfo {
    val o = org.json.JSONObject(json)
    return ProbeInfo(
        durationMs = o.optLong("durationMs"),
        width = o.optInt("width"),
        height = o.optInt("height"),
        fps = o.optDouble("fps"),
        pixFmt = o.optString("pixFmt"),
        hasVideo = o.optBoolean("hasVideo"),
        hasAudio = o.optBoolean("hasAudio"),
    )
}

/** Destination from §5.4: fixed tree vs user-chosen folder. */
sealed interface Destination {
    data class Default(val type: ConversionType) : Destination
    data class Custom(val folderUri: Uri) : Destination
}

data class ConversionJob(
    val id: String,
    val typeId: String,
    val source: MediaItem,
    val outputFormat: OutputFormat,
    val batchId: String = id,
    val status: JobStatus = JobStatus.QUEUED,
    val progress: Int = 0, // 0..100
    val outputPath: String? = null,
    val outputUri: Uri? = null,
    val error: String? = null,
)

/** Parses the C++ engine snapshot into UI-ready jobs. */
fun parseJobs(json: String, types: List<ConversionType>): List<ConversionJob> {
    val byId = types.associateBy { it.id }
    val out = mutableListOf<ConversionJob>()
    val arr = JSONArray(json)
    for (i in 0 until arr.length()) {
        val j = arr.getJSONObject(i)
        val type = byId[j.getString("typeId")] ?: continue
        val kind = type.inputKind
        val status = when (j.getString("status")) {
            "running" -> JobStatus.RUNNING
            "done" -> JobStatus.DONE
            "failed" -> JobStatus.FAILED
            else -> JobStatus.QUEUED
        }
        val ext = j.getString("formatExt")
        out += ConversionJob(
            id = j.getString("id"),
            typeId = type.id,
            source = MediaItem(
                id = -1,
                uriString = j.getString("sourceUri"),
                displayName = j.getString("sourceName"),
                mimeType = j.getString("mimeType"),
                kind = kind,
            ),
            outputFormat = type.outputFormats.firstOrNull { it.extension == ext }
                ?: OutputFormat(ext, ext.uppercase(), ""),
            batchId = j.getString("batchId"),
            status = status,
            progress = j.getInt("progress"),
            outputPath = j.getString("outputPath").ifBlank { null },
            error = j.getString("error").ifBlank { null },
        )
    }
    return out
}
