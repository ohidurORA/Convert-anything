package com.example.converanything.ui

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Ink = Color(0xFF05070B)
val Surface = Color(0xFF0C121C)
val SurfaceHigh = Color(0xFF141C2A)
val Hairline = Color(0xFF1E2A3D)
val Blue = Color(0xFF4C8DFF)
val BlueDeep = Color(0xFF1A4A9E)
val BlueDim = Color(0xFF0B1F3A)
val Snow = Color(0xFFFFFFFF)
val Mist = Color(0xFF8B93A7)
val Success = Color(0xFF30D158)
val Danger = Color(0xFFFF453A)
val Warning = Color(0xFFFF9F0A)

val Canvas = Ink
val Panel = Surface
val Card = SurfaceHigh
val Accent = Blue
val LabelSecondary = Mist

val EaseOutExpo = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)
val EaseInOutStrong = CubicBezierEasing(0.77f, 0f, 0.175f, 1f)
val EaseDrawer = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)

const val DurPress = 120
const val DurPop = 180
const val DurSheet = 280

fun <T> orayvaOut(duration: Int = DurPop) = tween<T>(duration, easing = EaseOutExpo)
fun <T> orayvaMove(duration: Int = DurPop) = tween<T>(duration, easing = EaseInOutStrong)
fun <T> orayvaDrawer(duration: Int = DurSheet) = tween<T>(duration, easing = EaseDrawer)

private val Scheme = darkColorScheme(
    background = Ink,
    surface = Surface,
    surfaceContainer = SurfaceHigh,
    surfaceVariant = SurfaceHigh,
    primary = Blue,
    onBackground = Snow,
    onSurface = Snow,
    onSurfaceVariant = Mist,
    outline = Hairline,
    tertiary = Success,
    error = Danger,
)

private val Type = Typography(
    displayLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        letterSpacing = (-0.8).sp,
        lineHeight = 38.sp,
        color = Snow,
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        letterSpacing = (-0.3).sp,
        lineHeight = 28.sp,
        color = Snow,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        letterSpacing = (-0.2).sp,
        lineHeight = 22.sp,
        color = Snow,
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        letterSpacing = 0.sp,
        lineHeight = 22.sp,
        color = Snow,
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        letterSpacing = 0.1.sp,
        lineHeight = 20.sp,
        color = Mist,
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        letterSpacing = 0.8.sp,
        lineHeight = 14.sp,
        color = Mist,
    ),
)

@Composable
fun ConvertTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, typography = Type, content = content)
}

@Composable
fun SectionLabel(text: String, trailing: @Composable (() -> Unit)? = null) {
    Column {
        Spacer(Modifier.height(20.dp))
        Text(
            text.uppercase(),
            color = Mist,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.2.sp,
        )
        trailing?.invoke()
        Spacer(Modifier.height(10.dp))
    }
}
