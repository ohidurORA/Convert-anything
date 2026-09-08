package com.example.converanything.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

enum class ToastKind { INFO, SUCCESS, ERROR }

data class OrayvaNotice(val message: String, val kind: ToastKind = ToastKind.INFO)

class ToastHost {
    private val _notices = MutableSharedFlow<OrayvaNotice>(extraBufferCapacity = 4)
    val notices = _notices.asSharedFlow()
    fun show(message: String, kind: ToastKind = ToastKind.INFO) {
        _notices.tryEmit(OrayvaNotice(mapUserError(message), kind))
    }
    fun error(message: String) = show(message, ToastKind.ERROR)
    fun success(message: String) = show(message, ToastKind.SUCCESS)
}

val LocalToast = staticCompositionLocalOf { ToastHost() }

fun mapUserError(raw: String): String {
    val t = raw.lowercase()
    return when {
        "space" in t || "enospc" in t || "no space" in t || "storage" in t ->
            "Not enough storage to save this file. Free some space and try again."
        "permission" in t || "eacces" in t || "denied" in t ->
            "Orayva needs permission to continue. Allow access in Settings, then retry."
        "unreadable" in t || "unsupported" in t || "not a valid" in t || "no audio" in t || "no video" in t ->
            raw.ifBlank { "This file can't be converted. Try another file or format." }
        "converter unavailable" in t || "stopped unexpectedly" in t ->
            "Conversion was interrupted. Tap retry to run it again."
        "empty or invalid" in t || "couldn't save" in t || "couldn't finish" in t ->
            "The conversion finished, but the file could not be saved. Check storage and try again."
        raw.isBlank() -> "Something went wrong. Try again."
        else -> raw
    }
}

fun showOrayvaToast(context: Context, message: String) {
    Toast.makeText(context, mapUserError(message), Toast.LENGTH_LONG).show()
}

@Composable
fun BoxScope.OrayvaToastLayer(host: ToastHost) {
    var current by remember { mutableStateOf<OrayvaNotice?>(null) }
    val view = LocalView.current
    LaunchedEffect(host) {
        host.notices.collect { notice ->
            current = notice
            when (notice.kind) {
                ToastKind.ERROR -> view.warn()
                ToastKind.SUCCESS -> view.commit()
                ToastKind.INFO -> { }
            }
            delay(3200)
            if (current == notice) current = null
        }
    }
    AnimatedVisibility(
        visible = current != null,
        modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = 18.dp, start = 20.dp, end = 20.dp),
        enter = fadeIn(orayvaOut(DurPop)) + slideInVertically(orayvaOut(DurPop)) { -24 },
        exit = fadeOut(orayvaOut(DurPress)) + slideOutVertically(orayvaOut(DurPress)) { -16 },
    ) {
        val n = current
        if (n != null) {
            val accent = when (n.kind) {
                ToastKind.SUCCESS -> Success
                ToastKind.ERROR -> Danger
                ToastKind.INFO -> Blue
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceHigh)
                    .border(1.dp, Hairline, RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(accent))
                Spacer(Modifier.width(12.dp))
                Text(n.message, color = Snow, fontSize = 14.sp, lineHeight = 20.sp)
            }
        }
    }
}
