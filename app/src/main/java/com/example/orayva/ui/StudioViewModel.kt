package com.example.orayva.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.orayva.core.ConversionType
import com.example.orayva.core.Destination
import com.example.orayva.core.MediaItem
import com.example.orayva.core.ProbeInfo
import com.example.orayva.data.ConvertApp
import com.example.orayva.data.MediaLibrary
import com.example.orayva.native.EngineBridge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StudioState(
    val type: ConversionType? = null,
    val items: List<MediaItem> = emptyList(),
    val probes: Map<Long, ProbeInfo> = emptyMap(),
    val formatQuery: String = "",
    val selectedExt: String? = null,
    val destination: Destination? = null,
    val showDestination: Boolean = false,
    val pendingFolder: Uri? = null,
    val folderPreview: MediaLibrary.TreePreview? = null,
)

/** Holder for the shared conversion flow (§5), parameterized by conversion type. */
class StudioViewModel(app: Application, private val typeId: String) : AndroidViewModel(app) {
    val library: MediaLibrary = (app as ConvertApp).library
    private val _state = MutableStateFlow(StudioState(type = runCatching { EngineBridge.type(typeId) }.getOrNull()))
    val state: StateFlow<StudioState> = _state.asStateFlow()

    fun loadIds(ids: List<Long>) {
        val t = _state.value.type ?: return
        viewModelScope.launch {
            adopt(library.lookup(ids, t.inputKind))
        }
    }

    /** SAF / OpenDocument selections (formats MediaStore does not index). */
    fun loadUris(uris: List<Uri>) {
        val t = _state.value.type ?: return
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val extra = uris.mapNotNull { library.itemFromUri(it, t.inputKind) }
            val merged = (_state.value.items + extra).distinctBy { it.uriString }
            adopt(merged)
        }
    }

    private suspend fun adopt(items: List<MediaItem>) {
        _state.value = _state.value.copy(items = items)
        val probes = mutableMapOf<Long, ProbeInfo>()
        items.filter { it.kind == com.example.orayva.core.MediaKind.VIDEO }.forEach {
            probes[it.id] = library.probeVideo(it.uriString)
        }
        if (probes.isNotEmpty()) _state.value = _state.value.copy(probes = probes)
    }

    fun setQuery(q: String) { _state.value = _state.value.copy(formatQuery = q) }

    fun chooseFormat(ext: String) {
        val t = _state.value.type ?: return
        _state.value = _state.value.copy(
            selectedExt = ext,
            destination = Destination.Default(t),
            showDestination = true,
        )
    }

    /** Step 2 of §5.4: folder picked → load preview (path + existing thumbs) before OK. */
    fun previewFolder(uri: Uri) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                pendingFolder = uri,
                folderPreview = library.treePreview(uri),
            )
        }
    }

    fun confirmCustomFolder() {
        val uri = _state.value.pendingFolder ?: return
        _state.value = _state.value.copy(
            destination = Destination.Custom(uri),
            showDestination = false,
            pendingFolder = null,
            folderPreview = null,
        )
    }

    fun cancelCustomFolder() {
        _state.value = _state.value.copy(pendingFolder = null, folderPreview = null)
    }

    fun keepDefault() {
        _state.value = _state.value.copy(showDestination = false)
    }

    fun dismissDestination() {
        _state.value = _state.value.copy(showDestination = false, selectedExt = null, destination = null)
    }

    fun filteredFormats() = _state.value.type?.outputFormats
        ?.filter { it.extension.contains(_state.value.formatQuery, true) ||
            it.label.contains(_state.value.formatQuery, true) } ?: emptyList()

    /** Proceed → engine queue (§5.5). Returns false if nothing to enqueue. */
    fun proceed(): Boolean {
        val s = _state.value
        val dest = s.destination ?: return false
        val ext = s.selectedExt ?: return false
        if (s.items.isEmpty()) return false
        (getApplication() as ConvertApp).enqueue(s.type!!.id, s.items, ext, dest)
        _state.value = s.copy(showDestination = false)
        return true
    }
}
