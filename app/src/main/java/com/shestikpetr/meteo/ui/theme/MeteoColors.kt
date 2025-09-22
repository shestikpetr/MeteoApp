package com.shestikpetr.meteo.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Custom blue-themed color palette for Meteo app
 * Minimalist design with soothing blue tones
 */

// Blue color palette
val DeepBlue = Color(0xFF1565C0)      // Primary blue
val SkyBlue = Color(0xFF42A5F5)       // Secondary blue
val LightBlue = Color(0xFF90CAF9)     // Light accent
val VeryLightBlue = Color(0xFFE3F2FD) // Surface tint

val DarkBlue = Color(0xFF0D47A1)      // Dark primary
val NightBlue = Color(0xFF1A237E)     // Very dark blue
val CloudWhite = Color(0xFFFAFAFA)    // Off-white
val StormGray = Color(0xFF37474F)     // Dark gray

// Additional accent colors
val SuccessGreen = Color(0xFF4CAF50)
val WarningAmber = Color(0xFFFF9800)
val ErrorRed = Color(0xFFF44336)

// Light theme colors
val MeteoLightColors = lightColorScheme(
    // Primary colors
    primary = DeepBlue,
    onPrimary = Color.White,
    primaryContainer = VeryLightBlue,
    onPrimaryContainer = DarkBlue,

    // Secondary colors
    secondary = SkyBlue,
    onSecondary = Color.White,
    secondaryContainer = LightBlue.copy(alpha = 0.3f),
    onSecondaryContainer = DarkBlue,

    // Tertiary colors
    tertiary = Color(0xFF6366F1), // Indigo accent
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE0E7FF),
    onTertiaryContainer = Color(0xFF312E81),

    // Background colors
    background = Color.White,
    onBackground = Color(0xFF1A1A1A),
    surface = CloudWhite,
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = VeryLightBlue,
    onSurfaceVariant = Color(0xFF424242),

    // Outline colors
    outline = Color(0xFFBDBDBD),
    outlineVariant = Color(0xFFE0E0E0),

    // Container colors
    surfaceContainer = Color(0xFFF5F5F5),
    surfaceContainerHigh = Color(0xFFEEEEEE),
    surfaceContainerHighest = Color(0xFFE8E8E8),

    // Error colors
    error = ErrorRed,
    onError = Color.White,
    errorContainer = Color(0xFFFFEBEE),
    onErrorContainer = Color(0xFFB71C1C),

    // Inverse colors for high contrast
    inverseSurface = Color(0xFF2E2E2E),
    inverseOnSurface = Color(0xFFF5F5F5),
    inversePrimary = LightBlue,

    // Shadow and scrim
    scrim = Color.Black.copy(alpha = 0.3f)
)

// Dark theme colors
val MeteoDarkColors = darkColorScheme(
    // Primary colors
    primary = LightBlue,
    onPrimary = DarkBlue,
    primaryContainer = Color(0xFF0D47A1),
    onPrimaryContainer = LightBlue,

    // Secondary colors
    secondary = Color(0xFF81C784), // Soft green
    onSecondary = Color(0xFF1B5E20),
    secondaryContainer = Color(0xFF388E3C),
    onSecondaryContainer = Color(0xFFC8E6C9),

    // Tertiary colors
    tertiary = Color(0xFF9C88FF), // Light purple
    onTertiary = Color(0xFF2D1B69),
    tertiaryContainer = Color(0xFF4C28AE),
    onTertiaryContainer = Color(0xFFD0BCFF),

    // Background colors
    background = Color(0xFF121212),
    onBackground = Color(0xFFE0E0E0),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFF424242),
    onSurfaceVariant = Color(0xFFBDBDBD),

    // Outline colors
    outline = Color(0xFF616161),
    outlineVariant = Color(0xFF424242),

    // Container colors
    surfaceContainer = Color(0xFF2A2A2A),
    surfaceContainerHigh = Color(0xFF353535),
    surfaceContainerHighest = Color(0xFF404040),

    // Error colors
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    // Inverse colors
    inverseSurface = Color(0xFFE0E0E0),
    inverseOnSurface = Color(0xFF2A2A2A),
    inversePrimary = DeepBlue,

    // Shadow and scrim
    scrim = Color.Black.copy(alpha = 0.5f)
)

/**
 * Extension function to get weather-specific colors
 */
object WeatherColors {
    val sunny = Color(0xFFFFD54F)
    val cloudy = Color(0xFF90A4AE)
    val rainy = Color(0xFF42A5F5)
    val snowy = Color(0xFFE1F5FE)
    val stormy = Color(0xFF5D4037)
    val foggy = Color(0xFFBDBDBD)

    // Temperature colors
    val hot = Color(0xFFFF5722)
    val warm = Color(0xFFFF9800)
    val mild = Color(0xFF4CAF50)
    val cool = Color(0xFF2196F3)
    val cold = Color(0xFF3F51B5)
    val freezing = Color(0xFF9C27B0)
}

/**
 * Extension function to get status colors
 */
object StatusColors {
    val online = SuccessGreen
    val offline = Color(0xFF757575)
    val warning = WarningAmber
    val error = ErrorRed
    val inactive = Color(0xFFBDBDBD)
}