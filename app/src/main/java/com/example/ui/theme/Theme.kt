package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppThemeMode

@Immutable
data class ReadingColors(
    val isDark: Boolean,
    val card: Color,
    val accentBg: Color,
    val accentFg: Color,
    val gold: Color,
    val goldFg: Color,
    val sidebar: Color,
    val sidebarAccent: Color,
    val border: Color,
    val heroBrush: Brush,
    val brandBrush: Brush,
    val nightBrush: Brush,
)

private val LightReadingColors = ReadingColors(
    isDark = false,
    card = LightCard,
    accentBg = LightAccent,
    accentFg = LightAccentForeground,
    gold = LightGold,
    goldFg = LightGoldForeground,
    sidebar = LightSidebar,
    sidebarAccent = LightSidebarAccent,
    border = LightBorder,
    heroBrush = Brush.linearGradient(LightHeroStops),
    brandBrush = Brush.linearGradient(listOf(BrandStart, BrandEnd)),
    nightBrush = Brush.linearGradient(listOf(NightStart, NightEnd)),
)

private val DarkReadingColors = ReadingColors(
    isDark = true,
    card = DarkCard,
    accentBg = DarkAccent,
    accentFg = DarkAccentForeground,
    gold = DarkGold,
    goldFg = DarkGoldForeground,
    sidebar = DarkSidebar,
    sidebarAccent = DarkSidebarAccent,
    border = DarkBorder,
    heroBrush = Brush.linearGradient(DarkHeroStops),
    brandBrush = Brush.linearGradient(listOf(BrandStart, BrandEnd)),
    nightBrush = Brush.linearGradient(listOf(NightStart, NightEnd)),
)

val LocalReadingColors = staticCompositionLocalOf { LightReadingColors }

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightPrimaryForeground,
    primaryContainer = LightSidebarAccent,
    onPrimaryContainer = LightPrimary,
    secondary = LightSecondary,
    onSecondary = LightSecondaryForeground,
    secondaryContainer = LightSecondary,
    onSecondaryContainer = LightSecondaryForeground,
    tertiary = LightAccentForeground,
    onTertiary = LightPrimaryForeground,
    tertiaryContainer = LightAccent,
    onTertiaryContainer = LightAccentForeground,
    background = LightBackground,
    onBackground = LightForeground,
    surface = LightCard,
    onSurface = LightForeground,
    surfaceVariant = LightMuted,
    onSurfaceVariant = LightMutedForeground,
    outline = LightBorder,
    outlineVariant = LightBorder,
    error = LightDestructive,
    onError = LightDestructiveForeground,
    errorContainer = Color(0xFFFDE8E8),
    onErrorContainer = LightDestructive,
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkPrimaryForeground,
    primaryContainer = DarkSidebarAccent,
    onPrimaryContainer = DarkPrimary,
    secondary = DarkSecondary,
    onSecondary = DarkSecondaryForeground,
    secondaryContainer = DarkSecondary,
    onSecondaryContainer = DarkSecondaryForeground,
    tertiary = DarkAccentForeground,
    onTertiary = DarkPrimaryForeground,
    tertiaryContainer = DarkAccent,
    onTertiaryContainer = DarkAccentForeground,
    background = DarkBackground,
    onBackground = DarkForeground,
    surface = DarkCard,
    onSurface = DarkForeground,
    surfaceVariant = DarkMuted,
    onSurfaceVariant = DarkMutedForeground,
    outline = DarkBorder,
    outlineVariant = DarkBorder,
    error = DarkDestructive,
    onError = DarkDestructiveForeground,
    errorContainer = Color(0xFF4A2022),
    onErrorContainer = DarkDestructive,
)

val DisplayFontFamily = FontFamily.Serif
val BodyFontFamily = FontFamily.SansSerif

val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 36.sp,
        lineHeight = 42.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.35).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.25).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.2).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
    ),
)

val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun ReadingCornerTheme(
    appThemeMode: AppThemeMode = AppThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (appThemeMode) {
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
        AppThemeMode.SYSTEM -> systemDark
    }

    val colorScheme = if (useDark) DarkColorScheme else LightColorScheme
    val readingColors = if (useDark) DarkReadingColors else LightReadingColors

    CompositionLocalProvider(LocalReadingColors provides readingColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}
