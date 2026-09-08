package com.example.orayva.ui

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.orayva.data.MediaLibrary

private fun hasMediaAccess(ctx: android.content.Context) =
    MediaLibrary.requiredPermissions().all {
        ContextCompat.checkSelfPermission(ctx, it) == PackageManager.PERMISSION_GRANTED
    }

@Composable
fun RequireMediaPermission(onGranted: () -> Unit = {}, content: @Composable () -> Unit) {
    val ctx = LocalContext.current
    var granted by remember { mutableStateOf(hasMediaAccess(ctx)) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { granted = hasMediaAccess(ctx) }
    LaunchedEffect(Unit) {
        if (!granted) {
            launcher.launch(
                MediaLibrary.requiredPermissions() + MediaLibrary.optionalPermissions())
        }
    }
    LaunchedEffect(granted) { if (granted) onGranted() }
    if (granted) {
        content()
    } else {
        Column(
            Modifier.fillMaxSize().background(Ink).padding(24.dp),
        ) {
            Spacer(Modifier.height(64.dp))
            ScreenHeader(
                "Allow access",
                "Orayva needs permission to browse videos, audio, and images on this device.",
            )
            Spacer(Modifier.height(20.dp))
            OrayvaButton("Allow access") {
                launcher.launch(
                    MediaLibrary.requiredPermissions() + MediaLibrary.optionalPermissions())
            }
        }
    }
}
