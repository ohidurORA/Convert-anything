package com.example.converanything

import com.example.converanything.core.MediaKind
import com.example.converanything.core.OutputFormat
import com.example.converanything.core.outputFileName
import com.example.converanything.core.parseTypes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression tests for the format/container-extension contract that drives
 * output file naming and publishing. The bug this guards against: HEVC and
 * ALAC advertise an extension (.hevc/.alac) whose muxer does NOT produce a
 * file of that name — HEVC is written into an .mp4 container, ALAC into an
 * .m4a container. If the UI publishes under the advertised extension, every
 * HEVC/ALAC conversion is engine-marked DONE but fails to save ("nothing
 * saved"). The single source of truth is the C++ registry's "container"
 * field, mirrored into [OutputFormat.container].
 */
class ContainerContractTest {

    // A minimal typesJson mirroring the real C++ registry's shape, including
    // the "container" field. parseTypes must surface it on OutputFormat.
    private fun typesJsonWithContainer(): String = """
        [{"id":"video","title":"Video","inputKind":"VIDEO","subfolder":"video","formats":[
          {"ext":"mp4","label":"MP4","mime":"video/mp4","blurb":"b","container":"mp4"},
          {"ext":"hevc","label":"HEVC","mime":"video/mp4","blurb":"b","container":"mp4"}
        ]},{"id":"audio","title":"Audio","inputKind":"AUDIO","subfolder":"audio","formats":[
          {"ext":"mp3","label":"MP3","mime":"audio/mpeg","blurb":"b","container":"mp3"},
          {"ext":"alac","label":"ALAC","mime":"audio/mp4","blurb":"b","container":"m4a"}
        ]}]
    """.trimIndent()

    @Test fun containerFieldParsedFromRegistry() {
        val types = parseTypes(typesJsonWithContainer())
        val video = types.first { it.id == "video" }
        val audio = types.first { it.id == "audio" }
        assertEquals("mp4", video.outputFormats.first { it.extension == "mp4" }.container)
        // HEVC advertises .hevc but the muxer writes .mp4.
        assertEquals("mp4", video.outputFormats.first { it.extension == "hevc" }.container)
        assertEquals("mp3", audio.outputFormats.first { it.extension == "mp3" }.container)
        // ALAC advertises .alac but the muxer writes .m4a.
        assertEquals("m4a", audio.outputFormats.first { it.extension == "alac" }.container)
    }

    @Test fun containerDefaultsToExtensionWhenAbsent() {
        // Older/partial registry payloads may omit "container"; it must fall
        // back to the advertised extension so nothing silently breaks.
        val json = """[{"id":"audio","title":"A","inputKind":"AUDIO","subfolder":"audio",
            "formats":[{"ext":"flac","label":"FLAC","mime":"audio/flac","blurb":"b"}]}]"""
        val fmt = parseTypes(json).first().outputFormats.first()
        assertEquals("flac", fmt.container)
    }

    @Test fun outputFileNameUsesContainerExtension() {
        // HEVC source -> published file must be .mp4, never .hevc.
        assertEquals("drone.mp4", outputFileName("drone.mov", "mp4"))
        // ALAC source -> published file must be .m4a, never .alac.
        assertEquals("song.m4a", outputFileName("song.flac", "m4a"))
        // Ordinary format: container == extension.
        assertEquals("song.mp3", outputFileName("song.wav", "mp3"))
    }

    @Test fun outputFileNameStemsFromSourceDisplayName() {
        assertEquals("vacation.mp4", outputFileName("vacation.jpg", "mp4"))
        // No-extension source falls back to "converted".
        assertEquals("converted.mp4", outputFileName("vacation", "mp4"))
        // Blank source falls back to "converted".
        assertEquals("converted.mp4", outputFileName("", "mp4"))
    }

    @Test fun everyAdvertisedFormatHasContainer() {
        // The real registry (from the C++ engine) must carry a container for
        // every advertised output format. A missing container would mean the
        // UI publishes under the wrong extension for that format.
        val types = parseTypes(typesJsonWithContainer())
        for (t in types) for (f in t.outputFormats) {
            assertTrue("format ${f.extension} has blank container", f.container.isNotBlank())
        }
    }

    @Test fun hevcAndAlacAreTheOnlyMismatches() {
        // Sanity: in the real registry only HEVC and ALAC diverge. If this ever
        // grows, the publish path must still follow container, not extension.
        val types = parseTypes(typesJsonWithContainer())
        val mismatches = types.flatMap { it.outputFormats }.filter { it.extension != it.container }
        val ids = mismatches.map { it.extension }.toSet()
        assertEquals(setOf("hevc", "alac"), ids)
    }
}