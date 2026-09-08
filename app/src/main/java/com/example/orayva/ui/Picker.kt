package com.example.orayva.ui

import android.app.Application
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.orayva.core.MediaFilter
import com.example.orayva.core.MediaItem
import com.example.orayva.core.MediaKind
import com.example.orayva.data.ConvertApp
import com.example.orayva.data.MediaLibrary
import com.example.orayva.native.EngineBridge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class PickerSort { NAME, SIZE }

class PickerViewModel(app: Application, val kind: MediaKind) : AndroidViewModel(app) {
    val library: MediaLibrary = (app as ConvertApp).library
    private val _items = MutableStateFlow<List<MediaItem>>(emptyList())
    val items: StateFlow<List<MediaItem>> = _items.asStateFlow()
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()
    private val _buckets = MutableStateFlow<Set<String>>(emptySet())
    val buckets: StateFlow<Set<String>> = _buckets.asStateFlow()
    private val _selected = MutableStateFlow<Set<Long>>(emptySet())
    val selected: StateFlow<Set<Long>> = _selected.asStateFlow()
    private val _sort = MutableStateFlow(PickerSort.NAME)
    val sort: StateFlow<PickerSort> = _sort.asStateFlow()

    init { reload() }

    fun reload() {
        viewModelScope.launch {
            val raw = library.query(
                kind,
                MediaFilter(query = _query.value, resolutionBuckets = _buckets.value),
            )
            _items.value = when (_sort.value) {
                PickerSort.NAME -> raw.sortedBy { it.displayName.lowercase() }
                PickerSort.SIZE -> raw.sortedByDescending { it.sizeBytes }
            }
        }
    }

    fun setQuery(q: String) { _query.value = q; reload() }
    fun setSort(s: PickerSort) { _sort.value = s; reload() }
    fun toggleBucket(b: String) {
        _buckets.value = if (b in _buckets.value) _buckets.value - b else _buckets.value + b
        reload()
    }
    fun toggleSelect(id: Long) {
        _selected.value = if (id in _selected.value) _selected.value - id else _selected.value + id
    }
}

@Composable
fun PickerScreen(typeId: String, nav: NavController) {
    val ctx = LocalContext.current
    val kind = runCatching { EngineBridge.type(typeId).inputKind }.getOrNull() ?: MediaKind.VIDEO
    val vm: PickerViewModel = viewModel(key = "picker-$typeId") {
        PickerViewModel(ctx.applicationContext as Application, kind)
    }
    val mime = arrayOf("*/*")
    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        nav.previousBackStackEntry?.savedStateHandle
            ?.set("picked_uris", uris.map { it.toString() })
        nav.popBackStack()
    }
    RequireMediaPermission(onGranted = vm::reload) {
        val items by vm.items.collectAsState()
        val query by vm.query.collectAsState()
        val buckets by vm.buckets.collectAsState()
        val selected by vm.selected.collectAsState()
        val sort by vm.sort.collectAsState()
        val heading = when (kind) {
            MediaKind.VIDEO -> "Select video"
            MediaKind.AUDIO -> "Select audio"
            MediaKind.IMAGE -> "Select images"
        }
        Column(Modifier.fillMaxSize().background(Ink).padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(12.dp))
            ScreenHeader(
                heading,
                "${selected.size} selected",
            )
            OutlinedTextField(
                value = query,
                onValueChange = vm::setQuery,
                placeholder = { Text("Search by name") },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                singleLine = true,
                colors = fieldColors(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OrayvaChip("Name", sort == PickerSort.NAME) { vm.setSort(PickerSort.NAME) }
                OrayvaChip("Size", sort == PickerSort.SIZE) { vm.setSort(PickerSort.SIZE) }
            }
            if (kind == MediaKind.VIDEO || kind == MediaKind.IMAGE) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("4K", "1080p", "720p").forEach { b ->
                        OrayvaChip(b, b in buckets) { vm.toggleBucket(b) }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            OrayvaGhostButton("Browse files") { filePicker.launch(mime) }
            if (items.isEmpty()) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    EmptyState(
                        "Nothing in the gallery",
                        "Browse files to open formats the gallery hides.",
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f).padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(items, key = { it.id }) { item ->
                        val sel = item.id in selected
                        Box(
                            Modifier
                                .height(150.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (sel) BlueDim else Surface)
                                .border(1.dp, if (sel) Blue else Hairline, RoundedCornerShape(14.dp))
                                .pressable { vm.toggleSelect(item.id) },
                        ) {
                            Thumb(item, vm.library, Modifier.fillMaxSize())
                            Text(
                                item.displayName,
                                color = Snow,
                                fontSize = 11.sp,
                                maxLines = 1,
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .background(Ink.copy(alpha = 0.72f))
                                    .padding(8.dp)
                                    .fillMaxWidth(),
                            )
                        }
                    }
                }
            }
            OrayvaButton(
                label = if (selected.isEmpty()) "Select files" else "Load ${selected.size}",
                enabled = selected.isNotEmpty(),
                onClick = {
                    nav.previousBackStackEntry?.savedStateHandle
                        ?.set("picked_ids", selected.toList())
                    nav.popBackStack()
                },
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Snow,
    unfocusedTextColor = Snow,
    focusedBorderColor = Blue,
    unfocusedBorderColor = Hairline,
    cursorColor = Blue,
    focusedPlaceholderColor = Mist,
    unfocusedPlaceholderColor = Mist,
    focusedContainerColor = Surface,
    unfocusedContainerColor = Surface,
)
