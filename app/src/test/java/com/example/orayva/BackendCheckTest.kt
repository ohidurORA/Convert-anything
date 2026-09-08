package com.example.orayva

import com.example.orayva.core.MediaFilter
import com.example.orayva.core.MediaItem
import com.example.orayva.core.MediaKind
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** JVM check over the pure-Kotlin picker/vault filter math (engine lives in C++). */
class BackendCheckTest {
    private fun item(name: String, mime: String, w: Int = 0, h: Int = 0, dur: Long = 0) =
        MediaItem(
            id = 1, uriString = "content://media/1",
            displayName = name, mimeType = mime, kind = MediaKind.VIDEO,
            sizeBytes = 1000, durationMs = dur, width = w, height = h,
        )

    @Test fun filter_matchesQueryBucketsAndDuration() {
        val clip = item("iceland_drone.mov", "video/quicktime", w = 3840, h = 2160, dur = 225_000)
        assertTrue(MediaFilter().matches(clip))
        assertTrue(MediaFilter(query = "iceland").matches(clip))
        assertTrue(MediaFilter(query = ".mov").matches(clip))
        assertTrue(MediaFilter(resolutionBuckets = setOf("4K")).matches(clip))
        assertFalse(MediaFilter(resolutionBuckets = setOf("1080p")).matches(clip))
        assertTrue(MediaFilter(extensions = setOf("mov")).matches(clip))
        assertFalse(MediaFilter(extensions = setOf("mp4")).matches(clip))
        assertTrue(MediaFilter(minDurationMs = 60_000).matches(clip))
        assertFalse(MediaFilter(minDurationMs = 300_000).matches(clip))
        assertFalse(MediaFilter(query = "tokyo").matches(clip))
    }
}
