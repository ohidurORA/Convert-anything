package com.example.orayva.playback

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem as ExoItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** One shared ExoPlayer. Cells/previews bind PlayerView to it; state drives inline play UI. */
object SharedPlayer {
    private var player: ExoPlayer? = null
    private val _playing = MutableStateFlow<Uri?>(null)
    val playing: StateFlow<Uri?> = _playing.asStateFlow()
    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()
    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()
    private val handler = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() {
            val p = player ?: return
            _positionMs.value = p.currentPosition.coerceAtLeast(0L)
            val d = p.duration
            if (d > 0) _durationMs.value = d
            if (_playing.value != null) handler.postDelayed(this, 200)
        }
    }

    fun get(context: Context): ExoPlayer {
        val appCtx = context.applicationContext
        return player ?: ExoPlayer.Builder(appCtx).build().also { built ->
            player = built
            built.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    val d = built.duration
                    if (d > 0) _durationMs.value = d
                    if (state == Player.STATE_ENDED) stop()
                }
            })
        }
    }

    fun play(context: Context, uri: Uri) {
        val p = get(context)
        if (_playing.value == uri && p.isPlaying) return
        p.setMediaItem(ExoItem.fromUri(uri))
        p.prepare()
        p.play()
        _playing.value = uri
        _positionMs.value = 0L
        handler.removeCallbacks(tick)
        handler.post(tick)
    }

    fun stop() {
        handler.removeCallbacks(tick)
        player?.stop()
        _playing.value = null
        _positionMs.value = 0L
        _durationMs.value = 0L
    }

    fun toggle(context: Context, uri: Uri) {
        if (_playing.value == uri && (player?.isPlaying == true)) stop() else play(context, uri)
    }

    fun seek(ms: Long) {
        player?.seekTo(ms.coerceAtLeast(0L))
        _positionMs.value = ms.coerceAtLeast(0L)
    }
}
