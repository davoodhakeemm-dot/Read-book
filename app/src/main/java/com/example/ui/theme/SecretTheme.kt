package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.model.SecretThemeMode

@Immutable
data class SecretPalette(
    val mode: SecretThemeMode,
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val onBackground: Color,
    val onSurface: Color,
    val onSurfaceMuted: Color,
    val border: Color,
    val primary: Color,
    val onPrimary: Color,
    val accent: Color,
    val onAccent: Color,
    val sentBubbleBrush: Brush,
    val receivedBubbleBg: Color,
    val receivedBubbleFg: Color,
    val storyRingBrush: Brush,
    val headerBrush: Brush,
    val fabBrush: Brush,
    val navIndicatorBg: Color,
    val danger: Color,
)

private val InstagramPalette = SecretPalette(
    mode = SecretThemeMode.INSTAGRAM,
    isDark = false,
    background = Color(0xFFFAFAFC),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF3F4F8),
    onBackground = Color(0xFF111827),
    onSurface = Color(0xFF111827),
    onSurfaceMuted = Color(0xFF6B7280),
    border = Color(0xFFE5E7EB),
    primary = Color(0xFFDD2A7B),
    onPrimary = Color(0xFFFFFFFF),
    accent = Color(0xFF8134AF),
    onAccent = Color(0xFFFFFFFF),
    sentBubbleBrush = Brush.linearGradient(
        listOf(Color(0xFFF58529), Color(0xFFDD2A7B), Color(0xFF8134AF), Color(0xFF515BD4)),
    ),
    receivedBubbleBg = Color(0xFFEFEFF4),
    receivedBubbleFg = Color(0xFF111827),
    storyRingBrush = Brush.linearGradient(
        listOf(Color(0xFFFEDA75), Color(0xFFFA7E1E), Color(0xFFD62976), Color(0xFF962FBF), Color(0xFF4F5BD5)),
    ),
    headerBrush = Brush.linearGradient(
        listOf(Color(0xFFFD5949), Color(0xFFD6249F), Color(0xFF285AEB)),
    ),
    fabBrush = Brush.linearGradient(
        listOf(Color(0xFFF58529), Color(0xFFDD2A7B), Color(0xFF8134AF)),
    ),
    navIndicatorBg = Color(0xFFFCE7F3),
    danger = Color(0xFFDC2626),
)

private val EmeraldPalette = SecretPalette(
    mode = SecretThemeMode.EMERALD,
    isDark = false,
    background = Color(0xFFF3FAF6),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE6F4EC),
    onBackground = Color(0xFF064E3B),
    onSurface = Color(0xFF064E3B),
    onSurfaceMuted = Color(0xFF4B7A67),
    border = Color(0xFFC8E6D5),
    primary = Color(0xFF059669),
    onPrimary = Color(0xFFFFFFFF),
    accent = Color(0xFFF97316),
    onAccent = Color(0xFFFFFFFF),
    sentBubbleBrush = Brush.linearGradient(
        listOf(Color(0xFF059669), Color(0xFF10B981)),
    ),
    receivedBubbleBg = Color(0xFFFFEDD5),
    receivedBubbleFg = Color(0xFF7C2D12),
    storyRingBrush = Brush.linearGradient(
        listOf(Color(0xFF10B981), Color(0xFF34D399), Color(0xFFF97316)),
    ),
    headerBrush = Brush.linearGradient(
        listOf(Color(0xFF065F46), Color(0xFF059669), Color(0xFFF97316)),
    ),
    fabBrush = Brush.linearGradient(
        listOf(Color(0xFF059669), Color(0xFFF97316)),
    ),
    navIndicatorBg = Color(0xFFD1FAE5),
    danger = Color(0xFFDC2626),
)

private val CherryDarkPalette = SecretPalette(
    mode = SecretThemeMode.DARK,
    isDark = true,
    background = Color(0xFF09090B),
    surface = Color(0xFF141418),
    surfaceVariant = Color(0xFF232329),
    onBackground = Color(0xFFF9FAFB),
    onSurface = Color(0xFFF9FAFB),
    onSurfaceMuted = Color(0xFF9CA3AF),
    border = Color(0xFF2E2E36),
    primary = Color(0xFFE11D48),
    onPrimary = Color(0xFFFFFFFF),
    accent = Color(0xFFFB7185),
    onAccent = Color(0xFF18080D),
    sentBubbleBrush = Brush.linearGradient(
        listOf(Color(0xFFBE123C), Color(0xFFE11D48)),
    ),
    receivedBubbleBg = Color(0xFF27272A),
    receivedBubbleFg = Color(0xFFF4F4F5),
    storyRingBrush = Brush.linearGradient(
        listOf(Color(0xFFE11D48), Color(0xFFFB7185), Color(0xFF52525B)),
    ),
    headerBrush = Brush.linearGradient(
        listOf(Color(0xFF09090B), Color(0xFF3F1221), Color(0xFF9F1239)),
    ),
    fabBrush = Brush.linearGradient(
        listOf(Color(0xFFBE123C), Color(0xFFE11D48)),
    ),
    navIndicatorBg = Color(0xFF3B121E),
    danger = Color(0xFFFB7185),
)

fun paletteForSecretTheme(mode: SecretThemeMode): SecretPalette = when (mode) {
    SecretThemeMode.INSTAGRAM -> InstagramPalette
    SecretThemeMode.EMERALD -> EmeraldPalette
    SecretThemeMode.DARK -> CherryDarkPalette
}
