package com.example.orayva.ui

import android.app.Application
import android.view.ViewGroup
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.ui.PlayerView
import com.example.orayva.core.MediaFilter
import com.example.orayva.core.MediaItem
import com.example.orayva.core.MediaKind
import com.example.orayva.data.ConvertApp
import com.example.orayva.data.MediaLibrary
import com.example.orayva.playback.SharedPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VaultViewModel(app: Application) : AndroidViewModel(app) {
    val library: MediaLibrary = (app as ConvertApp).library
    private val _items = MutableStateFlow<List<MediaItem>>(emptyList())
    val items: StateFlow<List<MediaItem>> = _items.asStateFlow()
    private val _tab = MutableStateFlow("Images")
    val tab: StateFlow<String> = _tab.asStateFlow()
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    init { reload() }

    fun reload() {
        viewModelScope.launch {
            val kinds = when (_tab.value) {
                "Videos" -> setOf(MediaKind.VIDEO)
                "Audios" -> setOf(MediaKind.AUDIO)
                "Images" -> setOf(MediaKind.IMAGE)
                else -> enumValues<MediaKind>().toSet()
            }
            _items.value = library.queryVault(MediaFilter(query = _query.value), kinds)
        }
    }

    fun setTab(t: String) { _tab.value = t; reload() }
    fun setQuery(q: String) { _query.value = q; reload() }
}

@Composable
fun VaultScreen() {
    val ctx = LocalContext.current
    val vm: VaultViewModel = viewModel {
        VaultViewModel(ctx.applicationContext as Application)
    }
    DisposableEffect(Unit) { onDispose { SharedPlayer.stop() } }
    RequireMediaPermission(onGranted = vm::reload) {
        val items by vm.items.collectAsState()
        val tab by vm.tab.collectAsState()
        val query by vm.query.collectAsState()
        val playing by SharedPlayer.playing.collectAsState()
        var zoom by remember { mutableStateOf<MediaItem?>(null) }
        var previewOn by remember { mutableStateOf(false) }

        Box(Modifier.fillMaxSize().background(Ink)) {
            LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                item {
                    Spacer(Modifier.height(12.dp))
                    ScreenHeader("Vault", "${items.size} items")
                    OutlinedTextField(
                        value = query,
                        onValueChange = vm::setQuery,
                        placeholder = { Text("Search files") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        singleLine = true,
                        colors = fieldColors(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Images", "Videos", "Audios").forEach { t ->
                            OrayvaChip(t, tab == t) { vm.setTab(t) }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                if (items.isEmpty()) {
                    item {
                        EmptyState(
                            "Vault is empty",
                            "Converted files appear here once indexed.",
                        )
                    }
                }
                items(items, key = { it.kind.name + it.id }) { item ->
                    when (item.kind) {
                        MediaKind.VIDEO -> VideoCell(
                            item, vm.library, playing == item.uri,
                            onPlay = { SharedPlayer.toggle(ctx, item.uri) },
                        )
                        MediaKind.AUDIO -> AudioCell(
                            item, playing == item.uri,
                            onPlay = { SharedPlayer.toggle(ctx, item.uri) },
                        )
                        MediaKind.IMAGE -> ImageCell(item, vm.library, onZoom = { zoom = item })
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            LaunchedEffect(zoom) { if (zoom != null) previewOn = true }
            zoom?.let { item ->
                val scale by animateFloatAsState(
                    targetValue = if (previewOn) 1f else 0.96f,
                    animationSpec = orayvaOut(if (previewOn) DurPop else DurPress),
                    finishedListener = { if (!previewOn) zoom = null },
                    label = "preview-scale",
                )
                val fade by animateFloatAsState(
                    targetValue = if (previewOn) 1f else 0f,
                    animationSpec = orayvaOut(if (previewOn) DurPop else DurPress),
                    label = "preview-fade",
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Ink.copy(alpha = 0.55f * fade))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { previewOn = false },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                alpha = fade
                            }
                            .clip(SheetShape)
                            .background(Surface)
                            .border(1.dp, Hairline, SheetShape)
                            .pressable { previewOn = false }
                            .padding(12.dp),
                    ) {
                        Thumb(
                            item,
                            vm.library,
                            Modifier
                                .fillMaxWidth()
                                .height(280.dp)
                                .clip(RoundedCornerShape(14.dp)),
                            contentScale = ContentScale.Fit,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoCell(
    item: MediaItem,
    library: MediaLibrary,
    isPlaying: Boolean,
    onPlay: () -> Unit,
) {
    val pos by SharedPlayer.positionMs.collectAsState()
    val dur by SharedPlayer.durationMs.collectAsState()
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface)
            .border(1.dp, Hairline, RoundedCornerShape(14.dp))
            .padding(8.dp),
    ) {
        if (isPlaying) {
            AndroidView(
                factory = { c ->
                    PlayerView(c).apply {
                        player = SharedPlayer.get(c)
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, 550)
                        useController = true
                    }
                },
                modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(8.dp)),
            )
            val length = dur.takeIf { it > 0 } ?: item.durationMs
            if (length > 0) {
                Spacer(Modifier.height(8.dp))
                ScrubBar(pos, length) { SharedPlayer.seek(it) }
            }
        } else {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .pressable(onClick = onPlay),
            ) {
                Thumb(item, library, Modifier.fillMaxSize())
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .background(Ink.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text("Play  ${formatDuration(item.durationMs)}", color = Snow, fontSize = 13.sp)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(item.displayName, color = Snow, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        Text("${formatBytes(item.sizeBytes)} · ${item.resolutionBucket}", color = Mist, fontSize = 12.sp)
    }
}

@Composable
private fun AudioCell(item: MediaItem, isPlaying: Boolean, onPlay: () -> Unit) {
    val pos by SharedPlayer.positionMs.collectAsState()
    val dur by SharedPlayer.durationMs.collectAsState()
    val length = dur.takeIf { it > 0 } ?: item.durationMs
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface)
            .border(1.dp, Hairline, RoundedCornerShape(14.dp))
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(BlueDim)
                    .pressable(onClick = onPlay)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(if (isPlaying) "Pause" else "Play", color = Blue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.displayName, color = Snow, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                if (!isPlaying) WaveformBars()
                Text(formatDuration(item.durationMs), color = Mist, fontSize = 11.sp)
            }
        }
        if (isPlaying && length > 0) {
            Spacer(Modifier.height(8.dp))
            ScrubBar(pos, length) { SharedPlayer.seek(it) }
            Text("${formatDuration(pos)} / ${formatDuration(length)}", color = Mist, fontSize = 11.sp)
        }
    }
}

@Composable
private fun ImageCell(item: MediaItem, library: MediaLibrary, onZoom: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface)
            .border(1.dp, Hairline, RoundedCornerShape(14.dp))
            .pressable(onClick = onZoom)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Thumb(item, library, Modifier.width(72.dp).height(72.dp).clip(RoundedCornerShape(8.dp)))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(item.displayName, color = Snow, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text("${item.width}x${item.height} · ${formatBytes(item.sizeBytes)}", color = Mist, fontSize = 12.sp)
        }
    }
}

@Composable
private fun WaveformBars() {
    Canvas(Modifier.fillMaxWidth().height(28.dp)) {
        val n = 40
        val step = size.width / n
        for (i in 0 until n) {
            val h = size.height * (0.25f + 0.65f * ((i * 7919) % 100 / 100f))
            drawLine(
                color = if (i < n / 3) Blue else Hairline,
                start = Offset(i * step + step / 2, (size.height - h) / 2),
                end = Offset(i * step + step / 2, (size.height + h) / 2),
                strokeWidth = step * 0.45f,
            )
        }
    }
}
