package com.update.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ─── Renkler ──────────────────────────────────────────────
val Bg = Color(0xFF0B0914)          // Web ile aynı arka plan
val BgCard = Color(0xFF111020)
val BgInput = Color(0xFF1A1830)

val Primary = Color(0xFFFF416C)     // Web'in kırmızısı
val PrimaryLight = Color(0xFFFF4B2B) // Web'in turuncusu
val PrimaryGlow = Color(0xFFFF7A5C)  // Soft turuncu
val Accent = Color(0xFFFFB3A0)

val Like = Color(0xFF4ADE80)         // Yeşil kalır
val Dislike = Color(0xFFF87171)

val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFCCCCCC)
val TextMuted = Color(0xFF666680)

val Border = Color(0xFF2A2040)
val BorderLight = Color(0xFF3D304A)

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
