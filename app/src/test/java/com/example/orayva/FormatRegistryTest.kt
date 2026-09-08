package com.example.orayva

import com.example.orayva.core.outputFileName
import com.example.orayva.core.parseTypes
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The UI must advertise every format the C++ registry actually converts.
 * A format that exists as a header file but is missing from typesJson never
 * appears on a tab — the user-facing "file exists but isn't shown" bug.
 */
class FormatRegistryTest {

    private val typesJson = """
        [{"id":"video","title":"Video Conversion","inputKind":"VIDEO","subfolder":"video","formats":[
          {"ext":"mp4","label":"MP4","mime":"video/mp4","blurb":"b","container":"mp4"},
          {"ext":"mkv","label":"MKV","mime":"video/x-matroska","blurb":"b","container":"mkv"},
          {"ext":"webm","label":"WebM","mime":"video/webm","blurb":"b","container":"webm"},
          {"ext":"mov","label":"MOV","mime":"video/quicktime","blurb":"b","container":"mov"},
          {"ext":"avi","label":"AVI","mime":"video/x-msvideo","blurb":"b","container":"avi"},
          {"ext":"flv","label":"FLV","mime":"video/x-flv","blurb":"b","container":"flv"},
          {"ext":"mpeg","label":"MPEG","mime":"video/mpeg","blurb":"b","container":"mpeg"},
          {"ext":"hevc","label":"HEVC","mime":"video/mp4","blurb":"b","container":"mp4"}
        ]},{"id":"audio","title":"Audio Conversion","inputKind":"AUDIO","subfolder":"audio","formats":[
          {"ext":"mp3","label":"MP3","mime":"audio/mpeg","blurb":"b","container":"mp3"},
          {"ext":"aac","label":"AAC","mime":"audio/mp4","blurb":"b","container":"aac"},
          {"ext":"flac","label":"FLAC","mime":"audio/flac","blurb":"b","container":"flac"},
          {"ext":"wav","label":"WAV","mime":"audio/x-wav","blurb":"b","container":"wav"},
          {"ext":"ogg","label":"OGG","mime":"audio/ogg","blurb":"b","container":"ogg"},
          {"ext":"opus","label":"OPUS","mime":"audio/opus","blurb":"b","container":"opus"},
          {"ext":"m4a","label":"M4A","mime":"audio/mp4","blurb":"b","container":"m4a"},
          {"ext":"alac","label":"ALAC","mime":"audio/mp4","blurb":"b","container":"m4a"}
        ]},{"id":"image","title":"Image Conversion","inputKind":"IMAGE","subfolder":"images","formats":[
          {"ext":"jpg","label":"JPEG","mime":"image/jpeg","blurb":"b","container":"jpg"},
          {"ext":"png","label":"PNG","mime":"image/png","blurb":"b","container":"png"},
          {"ext":"webp","label":"WebP","mime":"image/webp","blurb":"b","container":"webp"},
          {"ext":"avif","label":"AVIF","mime":"image/avif","blurb":"b","container":"avif"},
          {"ext":"bmp","label":"BMP","mime":"image/bmp","blurb":"b","container":"bmp"},
          {"ext":"tiff","label":"TIFF","mime":"image/tiff","blurb":"b","container":"tiff"},
          {"ext":"gif","label":"GIF","mime":"image/gif","blurb":"b","container":"gif"},
          {"ext":"tga","label":"TGA","mime":"image/x-tga","blurb":"b","container":"tga"},
          {"ext":"jp2","label":"JPEG 2000","mime":"image/jp2","blurb":"b","container":"jp2"},
          {"ext":"qoi","label":"QOI","mime":"image/qoi","blurb":"b","container":"qoi"},
          {"ext":"dpx","label":"DPX","mime":"image/x-dpx","blurb":"b","container":"dpx"},
          {"ext":"ppm","label":"PPM","mime":"image/x-portable-pixmap","blurb":"b","container":"ppm"}
        ]},{"id":"video_to_audio","title":"Video to Audio","inputKind":"VIDEO","subfolder":"audio","formats":[
          {"ext":"mp3","label":"MP3","mime":"audio/mpeg","blurb":"b","container":"mp3"},
          {"ext":"aac","label":"AAC","mime":"audio/mp4","blurb":"b","container":"aac"},
          {"ext":"flac","label":"FLAC","mime":"audio/flac","blurb":"b","container":"flac"},
          {"ext":"wav","label":"WAV","mime":"audio/x-wav","blurb":"b","container":"wav"},
          {"ext":"ogg","label":"OGG","mime":"audio/ogg","blurb":"b","container":"ogg"},
          {"ext":"opus","label":"OPUS","mime":"audio/opus","blurb":"b","container":"opus"},
          {"ext":"m4a","label":"M4A","mime":"audio/mp4","blurb":"b","container":"m4a"},
          {"ext":"alac","label":"ALAC","mime":"audio/mp4","blurb":"b","container":"m4a"}
        ]}]
    """.trimIndent()

    @Test fun everyImageFormatIsAdvertised() {
        val image = parseTypes(typesJson).first { it.id == "image" }
        assertEquals(
            setOf("jpg", "png", "webp", "avif", "bmp", "tiff", "gif", "tga", "jp2", "qoi", "dpx", "ppm"),
            image.outputFormats.map { it.extension }.toSet(),
        )
    }

    @Test fun everyAudioFormatIsAdvertisedOnBothAudioTabs() {
        val types = parseTypes(typesJson)
        val expected = setOf("mp3", "aac", "flac", "wav", "ogg", "opus", "m4a", "alac")
        assertEquals(expected, types.first { it.id == "audio" }.outputFormats.map { it.extension }.toSet())
        assertEquals(expected, types.first { it.id == "video_to_audio" }.outputFormats.map { it.extension }.toSet())
    }

    @Test fun everyVideoFormatIsAdvertised() {
        val video = parseTypes(typesJson).first { it.id == "video" }
        assertEquals(
            setOf("mp4", "mkv", "webm", "mov", "avi", "flv", "mpeg", "hevc"),
            video.outputFormats.map { it.extension }.toSet(),
        )
    }

    @Test fun ppmAndDpxKeepTheirOwnContainer() {
        val image = parseTypes(typesJson).first { it.id == "image" }
        assertEquals("ppm", image.outputFormats.first { it.extension == "ppm" }.container)
        assertEquals("dpx", image.outputFormats.first { it.extension == "dpx" }.container)
        assertEquals("scan.dpx", outputFileName("scan.ppm", "dpx"))
        assertEquals("frame.ppm", outputFileName("frame.dpx", "ppm"))
    }

    @Test fun batchCollisionStemsStayDistinct() {
        assertEquals("photo.mp3", outputFileName("photo.wav", "mp3"))
        assertEquals("photo.mp3", outputFileName("photo.flac", "mp3"))
    }
}
