package com.example.converanything.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.converanything.R
import com.example.converanything.core.ConversionType
import com.example.converanything.core.JobStatus
import com.example.converanything.native.EngineBridge

private val HomeOrder = listOf("audio", "video", "image", "video_to_audio")

private const val FacebookUrl = "https://www.facebook.com/wolfeap"
private const val InstagramUrl = "https://www.instagram.com/orabid9999/?hl=en"

private fun ConversionType.homeTitle(): String = when (id) {
    "audio" -> "Audio"
    "video" -> "Video"
    "image" -> "Images"
    "video_to_audio" -> "Video to Audio"
    else -> title.substringBefore(" ")
}

@Composable
fun SplashScreen(onStart: () -> Unit) {
    var ready by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { ready = true }
    val rise by animateFloatAsState(
        targetValue = if (ready) 0f else 12f,
        animationSpec = orayvaOut(360),
        label = "rise",
    )
    val fade by animateFloatAsState(
        targetValue = if (ready) 1f else 0f,
        animationSpec = orayvaOut(360),
        label = "fade",
    )
    Column(
        Modifier
            .fillMaxSize()
            .background(Ink)
            .padding(24.dp)
            .graphicsLayer { alpha = fade; translationY = rise },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(96.dp))
        OrayvaMark(80)
        Spacer(Modifier.height(24.dp))
        Text(
            "Orayva",
            color = Snow,
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-1.2).sp,
        )
        Spacer(Modifier.weight(1f))
        OrayvaButton("Continue", onClick = onStart)
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
fun HomeScreen(onOpenType: (String) -> Unit) {
    val types by EngineBridge.types.collectAsState()
    val jobs by EngineBridge.jobs.collectAsState()
    val ordered = HomeOrder.mapNotNull { id -> types.firstOrNull { it.id == id } } +
        types.filter { it.id !in HomeOrder }
    val recent = jobs.filter { it.status == JobStatus.DONE || it.status == JobStatus.FAILED }.takeLast(5).reversed()
    var about by remember { mutableStateOf(false) }
    var aboutOn by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(Ink)) {
        LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Spacer(Modifier.height(16.dp))
                Text(
                    "Orayva",
                    color = Snow,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.8).sp,
                    modifier = Modifier.pressable {
                        about = true
                        aboutOn = true
                    },
                )
                Spacer(Modifier.height(18.dp))
            }
            itemsIndexed(ordered, key = { _, t -> t.id }) { _, t ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(CardShape)
                        .background(Surface)
                        .border(1.dp, Hairline, CardShape)
                        .pressable { onOpenType(t.id) }
                        .padding(horizontal = 18.dp, vertical = 20.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Blue),
                        )
                        Spacer(Modifier.size(14.dp))
                        Text(
                            t.homeTitle(),
                            color = Snow,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.2).sp,
                        )
                    }
                }
            }
            if (recent.isNotEmpty()) {
                item { SectionLabel("Recent") }
                itemsIndexed(recent, key = { _, j -> j.id }) { _, j ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Surface)
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                j.source.displayName,
                                color = Snow,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                            )
                            Text(
                                "${j.source.extension.uppercase().ifBlank { "FILE" }}  to  ${j.outputFormat.extension.uppercase()}",
                                color = Mist,
                                fontSize = 12.sp,
                            )
                        }
                        Text(
                            if (j.status == JobStatus.DONE) "Done" else "Failed",
                            color = if (j.status == JobStatus.DONE) Success else Danger,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(28.dp)) }
        }

        LaunchedEffect(about) { if (about) aboutOn = true }
        if (about) {
            AboutDeveloperCard(
                visible = aboutOn,
                onDismiss = { aboutOn = false },
                onGone = { about = false },
            )
        }
    }
}

@Composable
private fun AboutDeveloperCard(
    visible: Boolean,
    onDismiss: () -> Unit,
    onGone: () -> Unit,
) {
    val ctx = LocalContext.current
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.96f,
        animationSpec = orayvaOut(if (visible) DurPop else DurPress),
        finishedListener = { if (!visible) onGone() },
        label = "about-scale",
    )
    val fade by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = orayvaOut(if (visible) DurPop else DurPress),
        label = "about-fade",
    )
    Box(
        Modifier
            .fillMaxSize()
            .background(Ink.copy(alpha = 0.62f * fade))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onDismiss() },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    alpha = fade
                }
                .clip(SheetShape)
                .background(Surface)
                .border(1.dp, Hairline, SheetShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {}
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.me),
                contentDescription = "Ohidur",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(108.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .border(1.dp, Hairline, RoundedCornerShape(28.dp))
                    .background(SurfaceHigh),
            )
            Spacer(Modifier.height(18.dp))
            Text(
                "Ohidur",
                color = Snow,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.3).sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Developer of Orayva",
                color = Blue,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.4.sp,
            )
            Spacer(Modifier.height(14.dp))
            Text(
                "I built Orayva to keep conversion quiet, local, and exact. If it helps you, a star on GitHub or a short review would mean a great deal.",
                color = Mist,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(18.dp))
            AboutLinks(
                onOpen = { url ->
                    ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                },
            )
        }
    }
}

@Composable
private fun AboutLinks(onOpen: (String) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text("Follow on  ", color = Mist, fontSize = 14.sp)
        Text(
            "Facebook",
            color = Blue,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier.pressable { onOpen(FacebookUrl) },
        )
        Text("   ·   ", color = Mist, fontSize = 14.sp)
        Text(
            "Instagram",
            color = Blue,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier.pressable { onOpen(InstagramUrl) },
        )
    }
}
