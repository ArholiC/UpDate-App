package com.update.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ─── Renkler ──────────────────────────────────────────────
val Bg = Color(0xFF0D0D1A)
val BgCard = Color(0xFF13132A)
val BgInput = Color(0xFF1A1A35)

val Primary = Color(0xFF7B2FBE)
val PrimaryLight = Color(0xFF9D4EDD)
val PrimaryGlow = Color(0xFFC77DFF)
val Accent = Color(0xFFE0AAFF)

val Like = Color(0xFF4ADE80)
val Dislike = Color(0xFFF87171)

val TextPrimary = Color(0xFFF0E6FF)
val TextSecondary = Color(0xFF9D8EC9)
val TextMuted = Color(0xFF5C5480)

val Border = Color(0xFF2A2050)
val BorderLight = Color(0xFF3D3070)

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    secondary = PrimaryLight,
    tertiary = PrimaryGlow,
    background = Bg,
    surface = BgCard,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    outline = Border,
)

@Composable
fun UpDateTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
