package com.example.ui.theme

import androidx.compose.ui.graphics.Color

val LanuGreen = Color(0xFF1DB954)
val LanuEmerald = Color(0xFF10B981)
val LanuPurple = Color(0xFF8B5CF6)
val LanuCyan = Color(0xFF06B6D4)
val LanuAmber = Color(0xFFF59E0B)
val LanuRose = Color(0xFFF43F5E)

val LanuDarkBg = Color(0xFF0B0D14)
val LanuDarkSurface = Color(0xFF131622)
val LanuDarkSurfaceElevated = Color(0xFF1B1F30)
val LanuDarkBorder = Color(0xFF262B40)

val LanuTextPrimary = Color(0xFFF8FAFC)
val LanuTextSecondary = Color(0xFF94A3B8)
val LanuTextMuted = Color(0xFF64748B)

// Aliases for modern Spotify-style dark aesthetics
val SpotifyGreen = LanuGreen
val EmeraldLight = LanuEmerald
val ElectricViolet = LanuPurple
val CyanGlow = LanuCyan
val DarkBackground = LanuDarkBg
val DarkCard = LanuDarkSurface
val DarkCardHover = LanuDarkSurfaceElevated
val DarkBorder = LanuDarkBorder
val TextPrimary = LanuTextPrimary
val TextSecondary = LanuTextSecondary
val TextMuted = LanuTextMuted

val DarkColorScheme = androidx.compose.material3.darkColorScheme(
    primary = LanuGreen,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF0E3D1F),
    onPrimaryContainer = Color(0xFF87F7A5),
    secondary = LanuPurple,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF2E1A47),
    onSecondaryContainer = Color(0xFFE9D8FD),
    tertiary = LanuCyan,
    background = LanuDarkBg,
    onBackground = LanuTextPrimary,
    surface = LanuDarkSurface,
    onSurface = LanuTextPrimary,
    surfaceVariant = LanuDarkSurfaceElevated,
    onSurfaceVariant = LanuTextSecondary,
    outline = LanuDarkBorder
)
