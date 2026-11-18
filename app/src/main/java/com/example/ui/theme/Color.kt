package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Light palette derived from Reading Corner's warm cream & violet design system
val LightBackground = Color(0xFFFBF8F3)
val LightForeground = Color(0xFF241E30)
val LightCard = Color(0xFFFFFFFF)
val LightPrimary = Color(0xFF6C3CE0)
val LightPrimaryForeground = Color(0xFFFDFBFF)
val LightSecondary = Color(0xFFF5EFE6)
val LightSecondaryForeground = Color(0xFF3B314D)
val LightMuted = Color(0xFFF4EFE7)
val LightMutedForeground = Color(0xFF746985)
val LightAccent = Color(0xFFF7E5DF)
val LightAccentForeground = Color(0xFF853B27)
val LightBorder = Color(0xFFE7E1D6)
val LightSidebar = Color(0xFFF8F4ED)
val LightSidebarAccent = Color(0xFFEAE2F8)
val LightDestructive = Color(0xFFD93D38)
val LightDestructiveForeground = Color(0xFFFFFDFB)
val LightGold = Color(0xFFE6AA35)
val LightGoldForeground = Color(0xFF4D350C)

// Dark palette
val DarkBackground = Color(0xFF1A1727)
val DarkForeground = Color(0xFFF5F3F0)
val DarkCard = Color(0xFF262137)
val DarkPrimary = Color(0xFFB794F6)
val DarkPrimaryForeground = Color(0xFF1D1530)
val DarkSecondary = Color(0xFF302A43)
val DarkSecondaryForeground = Color(0xFFF2EFFA)
val DarkMuted = Color(0xFF302A43)
val DarkMutedForeground = Color(0xFFA89EC0)
val DarkAccent = Color(0xFF422B2A)
val DarkAccentForeground = Color(0xFFF7CF9A)
val DarkBorder = Color(0xFF3D3752)
val DarkSidebar = Color(0xFF211D30)
val DarkSidebarAccent = Color(0xFF3A3154)
val DarkDestructive = Color(0xFFE6534C)
val DarkDestructiveForeground = Color(0xFFFFFBFB)
val DarkGold = Color(0xFFF0B947)
val DarkGoldForeground = Color(0xFF382605)

// Brand & surface gradients
val BrandStart = Color(0xFF6C3CE0)
val BrandEnd = Color(0xFFCA5299)

val NightStart = Color(0xFF2C1E56)
val NightEnd = Color(0xFF1A1434)

val LightHeroStops = listOf(Color(0xFFFAEEE0), Color(0xFFF7E5DF), Color(0xFFEBDCF9))
val DarkHeroStops = listOf(Color(0xFF412E5B), Color(0xFF432844), Color(0xFF2B2D52))

// Online indicator emerald
val OnlineEmerald = Color(0xFF10B981)
val OnlineEmeraldText = Color(0xFF059669)

// Reader surfaces
val ReaderLightBg = Color(0xFFFFFFFF)
val ReaderLightFg = Color(0xFF251F31)
val ReaderSepiaBg = Color(0xFFF4E9D6)
val ReaderSepiaFg = Color(0xFF45311B)
val ReaderDarkBg = Color(0xFF1C192A)
val ReaderDarkFg = Color(0xFFEBE7DF)

fun parseHexColor(hex: String, fallback: Color = Color(0xFF1F2A44)): Color {
    return try {
        val clean = hex.removePrefix("#")
        val value = clean.toLong(16)
        when (clean.length) {
            6 -> Color(0xFF000000L or value)
            8 -> Color(value)
            else -> fallback
        }
    } catch (_: Exception) {
        fallback
    }
}
