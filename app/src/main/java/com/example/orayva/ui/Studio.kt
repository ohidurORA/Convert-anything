package com.example.orayva.ui

import android.graphics.Bitmap
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Popup
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import com.example.orayva.core.Destination
import com.example.orayva.core.MediaItem
import com.example.orayva.core.MediaKind
import com.example.orayva.core.ProbeInfo
import com.example.orayva.data.ConvertApp
import com.example.orayva.data.MediaLibrary
import com.example.orayva.playback.SharedPlayer

private fun studioTitle(typeId: String, fallback: String?): String = when (typeId) {
    "audio" -> "Audio"
    "video" -> "Video"
    "image" -> "Images"
    "video_to_audio" -> "Video to Audio"
    else -> fallback ?: typeId
}

@Composable
fun StudioScreen(typeId: String, onPick: () -> Unit, onDone: () -> Unit, nav: NavController) {
    val ctx = LocalContext.current
    val view = LocalView.current
    val vm: StudioViewModel = viewModel(key = "studio-$typeId") {
        StudioViewModel(ctx.applicationContext as android.app.Application, typeId)
    }
    val state by vm.state.collectAsState()
    val toast = LocalToast.current
    DisposableEffect(Unit) { onDispose { SharedPlayer.stop() } }
    LaunchedEffect(Unit) {
        (ctx.applicationContext as ConvertApp).errors.collect {
            toast.error(it)
        }
    }

    val handle = nav.currentBackStackEntry?.savedStateHandle
    val picked = handle?.getStateFlow("picked_ids", emptyList<Long>())?.collectAsState()
    val pickedUris = handle?.getStateFlow("picked_uris", emptyList<String>())?.collectAsState()
    LaunchedEffect(picked?.value) {
        picked?.value?.takeIf { it.isNotEmpty() }?.let {
            SharedPlayer.stop()
            vm.loadIds(it)
            handle.set("picked_ids", emptyList<Long>())
        }
    }
    LaunchedEffect(pickedUris?.value) {
        pickedUris?.value?.takeIf { it.isNotEmpty() }?.let { raw ->
            SharedPlayer.stop()
            vm.loadUris(raw.map { android.net.Uri.parse(it) })
            handle.set("picked_uris", emptyList<String>())
        }
    }

    val treePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let { vm.previewFolder(it) }
    }

    LazyColumn(Modifier.fillMaxSize().background(Ink).padding(horizontal = 20.dp)) {
        item {
            Spacer(Modifier.height(12.dp))
            ScreenHeader(
                studioTitle(typeId, state.type?.title),
                "Pick files, then a format.",
            )
            Spacer(Modifier.height(12.dp))
            if (state.items.isEmpty()) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(CardShape)
                        .background(Surface)
                        .border(1.dp, Hairline, CardShape)
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("No files selected", color = Snow, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text("Browse device media.", color = Mist, fontSize = 13.sp)
                    Spacer(Modifier.height(16.dp))
                    OrayvaButton("Browse files", onClick = onPick)
                }
            } else if (state.items.size == 1) {
                SinglePreview(state.items.first(), state.probes[state.items.first().id], vm.library)
                Spacer(Modifier.height(8.dp))
                OrayvaGhostButton("Change files") { SharedPlayer.stop(); onPick() }
            } else {
                Text(
                    "${state.items.size} files",
                    color = Blue,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.items) { item ->
                        Column(Modifier.width(120.dp)) {
                            Thumb(
                                item, vm.library,
                                Modifier.height(80.dp).clip(RoundedCornerShape(10.dp)),
                            )
                            Text(item.displayName, color = Snow, fontSize = 11.sp, maxLines = 1)
                            Text(formatBytes(item.sizeBytes), color = Mist, fontSize = 11.sp)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OrayvaGhostButton("Change files") { SharedPlayer.stop(); onPick() }
            }
            Spacer(Modifier.height(8.dp))
        }

        item {
            SectionLabel("Format")
            OutlinedTextField(
                value = state.formatQuery,
                onValueChange = vm::setQuery,
                placeholder = { Text("Search format") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = fieldColors(),
            )
            Spacer(Modifier.height(8.dp))
        }
        items(vm.filteredFormats().chunked(2)) { row ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { f ->
                    val sel = state.selectedExt == f.extension
                    var info by remember(f.extension) { mutableStateOf(false) }
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (sel) BlueDim else Surface)
                            .border(1.dp, if (sel) Blue else Hairline, RoundedCornerShape(14.dp))
                            .pointerInput(f.extension, state.items.isNotEmpty()) {
                                detectTapGestures(
                                    onTap = {
                                        if (state.items.isNotEmpty()) {
                                            view.tick()
                                            vm.chooseFormat(f.extension)
                                        }
                                    },
                                    onLongPress = { info = true },
                                    onPress = { awaitRelease(); info = false },
                                )
                            }
                            .padding(14.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    f.extension.uppercase(),
                                    color = if (sel) Blue else Snow,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.2).sp,
                                )
                                Text(f.label, color = Mist, fontSize = 12.sp)
                            }
                            if (sel) {
                                Text("On", color = Success, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        if (info && f.blurb.isNotBlank()) {
                            Popup(alignment = Alignment.Center, onDismissRequest = { info = false }) {
                                Column(
                                    Modifier
                                        .clip(CardShape)
                                        .background(SurfaceHigh)
                                        .border(1.dp, Hairline, CardShape)
                                        .padding(16.dp),
                                ) {
                                    Text(
                                        f.extension.uppercase() + "  " + f.label,
                                        color = Snow,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(f.blurb, color = Mist, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        item {
            Spacer(Modifier.height(8.dp))
            if (state.selectedExt != null && state.items.isNotEmpty()) {
                val src = if (state.items.size == 1) {
                    state.items.first().extension.uppercase().ifBlank { "FILE" }
                } else {
                    "${state.items.size} FILES"
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceHigh)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(src, color = Snow, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text("  to  ", color = Mist, fontSize = 15.sp)
                    Text(
                        state.selectedExt!!.uppercase(),
                        color = Success,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
            state.destination?.let { dest ->
                DestinationRow(
                    dest = dest,
                    onChange = { state.selectedExt?.let { vm.chooseFormat(it) } },
                    onPickFolder = { treePicker.launch(null) },
                )
            }
            Spacer(Modifier.height(12.dp))
            OrayvaButton(
                label = "Convert",
                enabled = state.items.isNotEmpty() && state.selectedExt != null && state.destination != null,
                onClick = {
                    view.commit()
                    if (vm.proceed()) {
                        toast.success("Queued. Watch progress in Queue.")
                        onDone()
                    }
                },
            )
            Spacer(Modifier.height(32.dp))
        }
    }

    if (state.showDestination) {
        val t = state.type
        AlertDialog(
            onDismissRequest = vm::dismissDestination,
            containerColor = Surface,
            title = { Text("Save to", color = Snow) },
            text = {
                Column {
                    Text("Downloads", fontWeight = FontWeight.Bold, color = Snow)
                    Text(
                        "Downloads/Orayva/${t?.outputSubfolder}/",
                        color = Mist,
                        fontSize = 13.sp,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("Custom folder", fontWeight = FontWeight.Bold, color = Snow)
                    Text("Pick any folder, then confirm.", color = Mist, fontSize = 13.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (state.destination is Destination.Default) vm.keepDefault()
                }) { Text("Downloads", color = Blue) }
            },
            dismissButton = {
                TextButton(onClick = { treePicker.launch(null) }) { Text("Custom", color = Mist) }
            },
        )
    }

    if (state.pendingFolder != null) {
        val preview = state.folderPreview
        AlertDialog(
            onDismissRequest = vm::cancelCustomFolder,
            containerColor = Surface,
            title = { Text("Confirm folder", color = Snow) },
            text = {
                Column {
                    Text(
                        preview?.displayName ?: "…",
                        color = Snow,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                    )
                    Text("${preview?.childCount ?: 0} items inside", color = Mist, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        preview?.imageUris?.forEach { uri ->
                            TreeThumb(uri, vm.library)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = vm::confirmCustomFolder) { Text("OK", color = Blue) }
            },
            dismissButton = {
                TextButton(onClick = vm::cancelCustomFolder) { Text("Cancel", color = Mist) }
            },
        )
    }
}

@Composable
private fun TreeThumb(uri: android.net.Uri, library: MediaLibrary) {
    var bmp by remember(uri) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(uri) { bmp = library.thumbnailForUri(uri) }
    bmp?.let {
        Image(it.asImageBitmap(), null, Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)))
    }
}

@Composable
private fun SinglePreview(item: MediaItem, probe: ProbeInfo?, library: MediaLibrary) {
    val ctx = LocalContext.current
    Column(
        Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(Surface)
            .border(1.dp, Hairline, CardShape)
            .padding(12.dp),
    ) {
        when (item.kind) {
            MediaKind.IMAGE -> Thumb(
                item, library,
                Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(12.dp)),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
            )
            MediaKind.VIDEO -> {
                AndroidView(
                    factory = { c ->
                        PlayerView(c).apply {
                            player = SharedPlayer.get(c)
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT, 600)
                            useController = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(12.dp)),
                )
                LaunchedEffect(item.uriString) { SharedPlayer.play(ctx, item.uri) }
            }
            MediaKind.AUDIO -> AudioPreview(item)
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                item.extension.uppercase().ifBlank { item.kind.name },
                color = Blue,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(BlueDim)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                item.displayName,
                color = Snow,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
        }
        Text(
            when (item.kind) {
                MediaKind.VIDEO -> {
                    val res = if ((probe?.width ?: 0) > 0) "${probe!!.width}x${probe.height}"
                    else "${item.width}x${item.height}"
                    val fps = probe?.fps?.takeIf { it > 0 }?.let { " · ${"%.0f".format(it)}fps" } ?: ""
                    val dur = probe?.durationMs?.takeIf { it > 0 } ?: item.durationMs
                    "$res$fps · ${formatDuration(dur)} · ${formatBytes(item.sizeBytes)}"
                }
                MediaKind.AUDIO -> "${item.mimeType.substringAfter('/')} · " +
                    "${formatDuration(item.durationMs)} · ${formatBytes(item.sizeBytes)}"
                MediaKind.IMAGE -> "${item.width}x${item.height} · ${formatBytes(item.sizeBytes)}"
            },
            color = Mist,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun AudioPreview(item: MediaItem) {
    val ctx = LocalContext.current
    val playing by SharedPlayer.playing.collectAsState()
    val pos by SharedPlayer.positionMs.collectAsState()
    val dur by SharedPlayer.durationMs.collectAsState()
    val active = playing == item.uri
    val length = dur.takeIf { it > 0 } ?: item.durationMs
    Column(Modifier.fillMaxWidth()) {
        OrayvaButton(if (active) "Pause" else "Play") {
            SharedPlayer.toggle(ctx, item.uri)
        }
        if (active && length > 0) {
            Spacer(Modifier.height(10.dp))
            ScrubBar(pos, length) { SharedPlayer.seek(it) }
            Spacer(Modifier.height(4.dp))
            Text(
                "${formatDuration(pos)} / ${formatDuration(length)}",
                color = Mist,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
fun ScrubBar(positionMs: Long, durationMs: Long, onSeek: (Long) -> Unit) {
    val frac = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    Box(
        Modifier
            .fillMaxWidth()
            .height(28.dp)
            .pointerInput(durationMs) {
                detectTapGestures { offset ->
                    if (durationMs > 0) {
                        val p = (offset.x / size.width).coerceIn(0f, 1f)
                        onSeek((p * durationMs).toLong())
                    }
                }
            }
            .pointerInput(durationMs) {
                detectHorizontalDragGestures { change, _ ->
                    if (durationMs > 0 && size.width > 0) {
                        val p = (change.position.x / size.width).coerceIn(0f, 1f)
                        onSeek((p * durationMs).toLong())
                    }
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Hairline),
        )
        Box(
            Modifier
                .fillMaxWidth(frac)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Blue),
        )
    }
}

@Composable
private fun DestinationRow(dest: Destination, onChange: () -> Unit, onPickFolder: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface)
            .border(1.dp, Hairline, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            when (dest) {
                is Destination.Default -> "Downloads/Orayva/${dest.type.outputSubfolder}/"
                is Destination.Custom -> dest.folderUri.lastPathSegment
                    ?.substringAfterLast(':') ?: dest.folderUri.toString()
            },
            color = Snow,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f),
            maxLines = 2,
        )
        TextButton(onClick = { if (dest is Destination.Custom) onPickFolder() else onChange() }) {
            Text("Change", color = Blue)
        }
    }
}
