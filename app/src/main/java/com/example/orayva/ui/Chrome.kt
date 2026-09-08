package com.example.orayva.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val SheetShape = RoundedCornerShape(20.dp)
val CardShape = RoundedCornerShape(16.dp)
val ChipShape = RoundedCornerShape(20.dp)

@Composable
fun OrayvaMark(size: Int = 64) {
    Box(
        Modifier
            .size(size.dp)
            .clip(RoundedCornerShape((size * 0.28f).dp))
            .background(Blue),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "O",
            color = Snow,
            fontSize = (size * 0.42f).sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-1).sp,
        )
    }
}

@Composable
fun ScreenHeader(title: String, subtitle: String? = null) {
    Column(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp)) {
        Text(
            title,
            color = Snow,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.6).sp,
            lineHeight = 32.sp,
        )
        if (subtitle != null) {
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = Mist, fontSize = 14.sp, lineHeight = 20.sp)
        }
    }
}

@Composable
fun OrayvaButton(
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (enabled) Blue else Hairline)
            .pressable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (enabled) Snow else Mist,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun OrayvaGhostButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, Hairline, RoundedCornerShape(14.dp))
            .pressable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Snow, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun OrayvaChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(ChipShape)
            .background(if (selected) Blue else SurfaceHigh)
            .border(1.dp, if (selected) Blue else Hairline, ChipShape)
            .pressable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (selected) Snow else Mist,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun EmptyState(title: String, body: String) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(SurfaceHigh),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(Blue))
        }
        Spacer(Modifier.height(16.dp))
        Text(title, color = Snow, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(body, color = Mist, fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 18.sp)
    }
}


