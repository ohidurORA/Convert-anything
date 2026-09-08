package com.example.converanything.core

import org.json.JSONArray

/** Input media kind. New kinds (e.g. documents) extend this enum + C++ registry. */
enum class MediaKind { VIDEO, AUDIO, IMAGE }

/** Mirror of the C++ registry — parsed from EngineBridge, never hand-written. */
data class ConversionType(
    val id: String,
    val title: String,
    val inputKind: MediaKind,
    val outputSubfolder: String,
    val outputFormats: List<OutputFormat>,
)

data class OutputFormat(
    val extension: String,
    val label: String,
    val mimeType: String,
    val blurb: String = "",
    /**
     * The real on-disk/container extension the muxer produces for this format.
     * Mirrors the C++ registry's "container" field. Almost always equals
     * [extension]; the two exceptions are exceptional on purpose:
     *  - ALAC is a codec with no muxer — it ships inside an .m4a container.
     *  - HEVC is written ("hvc1") into an .mp4 container.
     * The engine and the UI both derive every file name / published extension
     * from this value, so a conversion can never be written to one extension
     * and published (or advertised) under a different one.
     */
    val container: String = extension,
)

/**
 * Computes the final output file name for a conversion. The stem comes from
 * the source display name, and the extension is the *container* extension of
 * the chosen format (never the bare advertised extension, which may not carry
 * the real muxer output — see [OutputFormat.container]).
 */
fun outputFileName(sourceName: String, containerExt: String): String {
    val base = sourceName.substringBeforeLast('.').ifBlank { "converted" }
    return "$base.$containerExt"
}

fun parseTypes(json: String): List<ConversionType> {
    val out = mutableListOf<ConversionType>()
    val arr = JSONArray(json)
    for (i in 0 until arr.length()) {
        val t = arr.getJSONObject(i)
        val fmts = mutableListOf<OutputFormat>()
        val fa = t.getJSONArray("formats")
        for (j in 0 until fa.length()) {
            val f = fa.getJSONObject(j)
            val ext = f.getString("ext")
            fmts += OutputFormat(
                ext,
                f.getString("label"),
                f.getString("mime"),
                f.optString("blurb"),
                container = f.optString("container", ext).ifBlank { ext },
            )
        }
        out += ConversionType(
            id = t.getString("id"),
            title = t.getString("title"),
            inputKind = MediaKind.valueOf(t.getString("inputKind").uppercase()),
            outputSubfolder = t.getString("subfolder"),
            outputFormats = fmts,
        )
    }
    return out
}
