package com.streambert.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ── Accent color presets (matching Streambert) ──
object AccentPresets {
    val Red = Color(0xFFE50914)
    val Blue = Color(0xFF2563EB)
    val Purple = Color(0xFF7C3AED)
    val Green = Color(0xFF059669)
    val Orange = Color(0xFFD97706)
    val Pink = Color(0xFFDB2777)

    val all = mapOf(
        "Red" to Red, "Blue" to Blue, "Purple" to Purple,
        "Green" to Green, "Orange" to Orange, "Pink" to Pink,
    )

    fun fromHex(hex: String): Color {
        return try {
            Color(android.graphics.Color.parseColor(hex))
        } catch (_: Exception) { Red }
    }
}

// ── Theme presets ──
data class StreambertColors(
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val surface3: Color,
    val border: Color,
    val text: Color,
    val text2: Color,
    val text3: Color,
    val accent: Color,
    val accentDim: Color,
    val accentGlow: Color,
)

object ThemePresets {
    val Dark = StreambertColors(
        bg = Color(0xFF0A0A0A), surface = Color(0xFF111111), surface2 = Color(0xFF1A1A1A),
        surface3 = Color(0xFF222222), border = Color(0xFF2A2A2A),
        text = Color(0xFFF0F0F0), text2 = Color(0xFFAAAAAA), text3 = Color(0xFF666666),
        accent = AccentPresets.Red, accentDim = Color(0x55E50914), accentGlow = Color(0x33E50914),
    )
    val AMOLED = StreambertColors(
        bg = Color(0xFF000000), surface = Color(0xFF080808), surface2 = Color(0xFF111111),
        surface3 = Color(0xFF1A1A1A), border = Color(0xFF222222),
        text = Color(0xFFFFFFFF), text2 = Color(0xFFBBBBBB), text3 = Color(0xFF777777),
        accent = AccentPresets.Red, accentDim = Color(0x55E50914), accentGlow = Color(0x33E50914),
    )
    val Mocha = StreambertColors(
        bg = Color(0xFF0E0B09), surface = Color(0xFF1A1410), surface2 = Color(0xFF231C16),
        surface3 = Color(0xFF2D241C), border = Color(0xFF3D3228),
        text = Color(0xFFF0E8DF), text2 = Color(0xFFB0A898), text3 = Color(0xFF7A7060),
        accent = AccentPresets.Red, accentDim = Color(0x55E50914), accentGlow = Color(0x33E50914),
    )
    val Slate = StreambertColors(
        bg = Color(0xFF0D1117), surface = Color(0xFF161B22), surface2 = Color(0xFF1F2937),
        surface3 = Color(0xFF2D3748), border = Color(0xFF374151),
        text = Color(0xFFE6EDF3), text2 = Color(0xFFA0AEC0), text3 = Color(0xFF636E7C),
        accent = AccentPresets.Red, accentDim = Color(0x55E50914), accentGlow = Color(0x33E50914),
    )
    val Light = StreambertColors(
        bg = Color(0xFFEBEBED), surface = Color(0xFFF8F8FA), surface2 = Color(0xFFEEEEF0),
        surface3 = Color(0xFFE0E0E2), border = Color(0xFFD0D0D2),
        text = Color(0xFF111113), text2 = Color(0xFF555557), text3 = Color(0xFF999999),
        accent = AccentPresets.Red, accentDim = Color(0x33E50914), accentGlow = Color(0x22E50914),
    )

    val map = mapOf("dark" to Dark, "amoled" to AMOLED, "mocha" to Mocha, "slate" to Slate, "light" to Light)
}

@Composable
fun StreambertTheme(
    themeId: String,
    accentHex: String,
    content: @Composable () -> Unit,
) {
    val baseColors = ThemePresets.map[themeId] ?: ThemePresets.Dark
    val customAccent = AccentPresets.fromHex(accentHex)
    val colors = baseColors.copy(accent = customAccent, accentDim = customAccent.copy(alpha = 0.33f), accentGlow = customAccent.copy(alpha = 0.2f))

    val colorScheme = if (baseColors == ThemePresets.Light) {
        lightColorScheme(
            primary = colors.accent,
            onPrimary = Color.White,
            background = colors.bg,
            onBackground = colors.text,
            surface = colors.surface,
            onSurface = colors.text,
            surfaceVariant = colors.surface2,
            onSurfaceVariant = colors.text2,
            outline = colors.border,
            secondary = colors.surface2,
            onSecondary = colors.text,
        )
    } else {
        darkColorScheme(
            primary = colors.accent,
            onPrimary = Color.White,
            background = colors.bg,
            onBackground = colors.text,
            surface = colors.surface,
            onSurface = colors.text,
            surfaceVariant = colors.surface2,
            onSurfaceVariant = colors.text2,
            outline = colors.border,
            secondary = colors.surface2,
            onSecondary = colors.text,
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(
            headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 28.sp),
            headlineMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
            headlineSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
            titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
            titleMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp),
            titleSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp),
            bodyLarge = TextStyle(fontSize = 16.sp),
            bodyMedium = TextStyle(fontSize = 14.sp),
            bodySmall = TextStyle(fontSize = 12.sp),
            labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp),
            labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp),
            labelSmall = TextStyle(fontSize = 11.sp),
        ),
        content = content,
    )
}