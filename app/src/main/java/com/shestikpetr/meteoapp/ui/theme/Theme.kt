package com.shestikpetr.meteoapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.shestikpetr.meteoapp.domain.model.ThemeMode

/**
 * Семантические цвета приложения, выходящие за рамки Material3 ColorScheme.
 * Соответствуют CSS-переменным meteo-web.
 */
data class AppPalette(
    val ink: Color,
    val ink2: Color,
    val ink3: Color,
    val ink4: Color,
    val line: Color,
    val line2: Color,
    val bg: Color,
    val bgElev: Color,
    val bgSunken: Color,
    val accent: Color,
    val accentSoft: Color,
    val accentInk: Color,
    val warn: Color,
    val danger: Color,
    val ok: Color,
    val isDark: Boolean
)

private val LocalAppPalette = staticCompositionLocalOf<AppPalette> {
    error("AppPalette not provided")
}

private val LocalTooltipsEnabled = compositionLocalOf { true }

/**
 * Доступ к расширенной палитре. Использовать через `MaterialTheme.appColors`.
 */
val MaterialTheme.appColors: AppPalette
    @Composable @ReadOnlyComposable
    get() = LocalAppPalette.current

/** Глобальная подсказка, что подсказки в UI включены. */
val MaterialTheme.tooltipsEnabled: Boolean
    @Composable @ReadOnlyComposable
    get() = LocalTooltipsEnabled.current

private val LightPalette = AppPalette(
    ink = MeteoColors.Light.Ink,
    ink2 = MeteoColors.Light.Ink2,
    ink3 = MeteoColors.Light.Ink3,
    ink4 = MeteoColors.Light.Ink4,
    line = MeteoColors.Light.Line,
    line2 = MeteoColors.Light.Line2,
    bg = MeteoColors.Light.Bg,
    bgElev = MeteoColors.Light.BgElev,
    bgSunken = MeteoColors.Light.BgSunken,
    accent = MeteoColors.Light.Accent,
    accentSoft = MeteoColors.Light.AccentSoft,
    accentInk = MeteoColors.Light.AccentInk,
    warn = MeteoColors.Light.Warn,
    danger = MeteoColors.Light.Danger,
    ok = MeteoColors.Light.Ok,
    isDark = false
)

private val DarkPalette = AppPalette(
    ink = MeteoColors.Dark.Ink,
    ink2 = MeteoColors.Dark.Ink2,
    ink3 = MeteoColors.Dark.Ink3,
    ink4 = MeteoColors.Dark.Ink4,
    line = MeteoColors.Dark.Line,
    line2 = MeteoColors.Dark.Line2,
    bg = MeteoColors.Dark.Bg,
    bgElev = MeteoColors.Dark.BgElev,
    bgSunken = MeteoColors.Dark.BgSunken,
    accent = MeteoColors.Dark.Accent,
    accentSoft = MeteoColors.Dark.AccentSoft,
    accentInk = MeteoColors.Dark.AccentInk,
    warn = MeteoColors.Dark.Warn,
    danger = MeteoColors.Dark.Danger,
    ok = MeteoColors.Dark.Ok,
    isDark = true
)

/**
 * Material3 ColorScheme собран из `AppPalette` так, чтобы стандартные компоненты
 * (Surface, FilledButton, OutlinedTextField, Card) сразу выглядели «по-метео-веб».
 */
private fun lightSchemeFrom(p: AppPalette) = lightColorScheme(
    primary = p.ink,                  // primary action = тёмная заливка
    onPrimary = p.bgElev,
    primaryContainer = p.accentSoft,
    onPrimaryContainer = p.accentInk,

    secondary = p.accent,
    onSecondary = Color.White,
    secondaryContainer = p.accentSoft,
    onSecondaryContainer = p.accentInk,

    tertiary = p.accent,
    onTertiary = Color.White,

    error = p.danger,
    onError = Color.White,
    errorContainer = p.danger.copy(alpha = 0.12f),
    onErrorContainer = p.danger,

    background = p.bg,
    onBackground = p.ink,
    surface = p.bgElev,
    onSurface = p.ink,
    surfaceVariant = p.bgSunken,
    onSurfaceVariant = p.ink3,
    surfaceTint = p.accent,

    inverseSurface = p.ink,
    inverseOnSurface = p.bgElev,
    inversePrimary = p.accent,

    outline = p.line2,
    outlineVariant = p.line,
    scrim = Color.Black.copy(alpha = 0.5f)
)

private fun darkSchemeFrom(p: AppPalette) = darkColorScheme(
    primary = p.ink,                  // в тёмной теме primary тоже строится от ink (светлый-беж)
    onPrimary = p.bg,
    primaryContainer = p.accentSoft,
    onPrimaryContainer = p.accentInk,

    secondary = p.accent,
    onSecondary = p.ink,
    secondaryContainer = p.accentSoft,
    onSecondaryContainer = p.accentInk,

    tertiary = p.accent,
    onTertiary = p.ink,

    error = p.danger,
    onError = Color.White,
    errorContainer = p.danger.copy(alpha = 0.18f),
    onErrorContainer = p.danger,

    background = p.bg,
    onBackground = p.ink,
    surface = p.bgElev,
    onSurface = p.ink,
    surfaceVariant = p.bgSunken,
    onSurfaceVariant = p.ink3,
    surfaceTint = p.accent,

    inverseSurface = p.ink,
    inverseOnSurface = p.bg,
    inversePrimary = p.accent,

    outline = p.line2,
    outlineVariant = p.line,
    scrim = Color.Black.copy(alpha = 0.6f)
)

@Composable
fun MeteoAppTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    tooltipsEnabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> systemDark
    }
    val palette = if (useDark) DarkPalette else LightPalette
    val scheme = if (useDark) darkSchemeFrom(palette) else lightSchemeFrom(palette)

    CompositionLocalProvider(
        LocalAppPalette provides palette,
        LocalTooltipsEnabled provides tooltipsEnabled
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = MeteoTypography,
            content = content
        )
    }
}
