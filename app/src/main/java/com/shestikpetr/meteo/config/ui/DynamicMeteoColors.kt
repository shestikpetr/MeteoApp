package com.shestikpetr.meteo.config.ui

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.shestikpetr.meteo.config.interfaces.ThemeConfigRepository
import com.shestikpetr.meteo.ui.theme.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dynamic color provider that replaces hardcoded MeteoColors with configurable themes.
 * Provides a composition local for accessing dynamic colors throughout the app.
 */
@Singleton
class DynamicMeteoColors @Inject constructor(
    private val themeConfigRepository: ThemeConfigRepository
) {

    /**
     * Composable that provides dynamic theme colors to child components.
     */
    @Composable
    fun DynamicThemeProvider(
        isDarkTheme: Boolean = false,
        content: @Composable () -> Unit
    ) {
        var themeConfig by remember { mutableStateOf<ThemeConfigRepository.ThemeConfiguration?>(null) }
        var isLoading by remember { mutableStateOf(true) }

        LaunchedEffect(isDarkTheme) {
            launch {
                try {
                    val config = if (isDarkTheme) {
                        themeConfigRepository.getDarkThemeConfiguration().getOrElse {
                            themeConfigRepository.getDefaultThemeConfiguration().copy(isDarkTheme = true)
                        }
                    } else {
                        themeConfigRepository.getThemeConfiguration().getOrElse {
                            themeConfigRepository.getDefaultThemeConfiguration()
                        }
                    }
                    themeConfig = config
                } catch (e: Exception) {
                    // Fallback to default configuration
                    themeConfig = themeConfigRepository.getDefaultThemeConfiguration().copy(isDarkTheme = isDarkTheme)
                } finally {
                    isLoading = false
                }
            }
        }

        if (!isLoading && themeConfig != null) {
            val colors = createColorScheme(themeConfig!!, isDarkTheme)
            val dynamicWeatherColors = DynamicWeatherColors(themeConfig!!.weatherColors)
            val dynamicStatusColors = DynamicStatusColors(themeConfig!!.statusColors)

            CompositionLocalProvider(
                LocalDynamicWeatherColors provides dynamicWeatherColors,
                LocalDynamicStatusColors provides dynamicStatusColors,
                LocalDynamicThemeConfig provides themeConfig!!
            ) {
                androidx.compose.material3.MaterialTheme(
                    colorScheme = colors,
                    content = content
                )
            }
        } else {
            // Show loading or fallback theme
            val fallbackConfig = themeConfigRepository.getDefaultThemeConfiguration().copy(isDarkTheme = isDarkTheme)
            val fallbackColors = if (isDarkTheme) MeteoDarkColors else MeteoLightColors
            val fallbackWeatherColors = DynamicWeatherColors(fallbackConfig.weatherColors)
            val fallbackStatusColors = DynamicStatusColors(fallbackConfig.statusColors)

            CompositionLocalProvider(
                LocalDynamicWeatherColors provides fallbackWeatherColors,
                LocalDynamicStatusColors provides fallbackStatusColors,
                LocalDynamicThemeConfig provides fallbackConfig
            ) {
                androidx.compose.material3.MaterialTheme(
                    colorScheme = fallbackColors,
                    content = content
                )
            }
        }
    }

    /**
     * Creates a material color scheme from theme configuration.
     */
    private fun createColorScheme(
        config: ThemeConfigRepository.ThemeConfiguration,
        isDarkTheme: Boolean
    ) = if (isDarkTheme) {
        MeteoDarkColors.copy(
            primary = config.statusColors.online,
            error = config.statusColors.error,
            secondary = config.weatherColors.cloudy,
            tertiary = config.temperatureColors.cool
        )
    } else {
        MeteoLightColors.copy(
            primary = config.statusColors.online,
            error = config.statusColors.error,
            secondary = config.weatherColors.sunny,
            tertiary = config.temperatureColors.mild
        )
    }
}

/**
 * Dynamic weather colors wrapper that replaces hardcoded WeatherColors object.
 */
data class DynamicWeatherColors(
    private val config: ThemeConfigRepository.WeatherColorScheme
) {
    val sunny: Color get() = config.sunny
    val cloudy: Color get() = config.cloudy
    val rainy: Color get() = config.rainy
    val snowy: Color get() = config.snowy
    val stormy: Color get() = config.stormy
    val foggy: Color get() = config.foggy

    // Temperature colors can be derived or separate
    val hot: Color get() = Color(0xFFFF5722) // Can be made configurable
    val warm: Color get() = Color(0xFFFF9800)
    val mild: Color get() = Color(0xFF4CAF50)
    val cool: Color get() = Color(0xFF2196F3)
    val cold: Color get() = Color(0xFF3F51B5)
    val freezing: Color get() = Color(0xFF9C27B0)
}

/**
 * Dynamic status colors wrapper that replaces hardcoded StatusColors object.
 */
data class DynamicStatusColors(
    private val config: ThemeConfigRepository.StatusColorScheme
) {
    val online: Color get() = config.online
    val offline: Color get() = config.offline
    val warning: Color get() = config.warning
    val error: Color get() = config.error
    val inactive: Color get() = config.inactive
}

/**
 * Composition locals for accessing dynamic colors.
 */
val LocalDynamicWeatherColors = compositionLocalOf<DynamicWeatherColors> {
    error("DynamicWeatherColors not provided")
}

val LocalDynamicStatusColors = compositionLocalOf<DynamicStatusColors> {
    error("DynamicStatusColors not provided")
}

val LocalDynamicThemeConfig = compositionLocalOf<ThemeConfigRepository.ThemeConfiguration> {
    error("DynamicThemeConfig not provided")
}

/**
 * Composables for accessing dynamic colors.
 */
object DynamicColors {

    /**
     * Get weather colors from the current composition.
     */
    val weather: DynamicWeatherColors
        @Composable get() = LocalDynamicWeatherColors.current

    /**
     * Get status colors from the current composition.
     */
    val status: DynamicStatusColors
        @Composable get() = LocalDynamicStatusColors.current

    /**
     * Get theme configuration from the current composition.
     */
    val theme: ThemeConfigRepository.ThemeConfiguration
        @Composable get() = LocalDynamicThemeConfig.current

    /**
     * Get color for a specific parameter code.
     */
    @Composable
    fun getParameterColor(parameterCode: String): Color {
        val themeConfig = LocalDynamicThemeConfig.current
        return themeConfig.parameterColors.find {
            it.parameterCode.equals(parameterCode, ignoreCase = true)
        }?.color ?: getDefaultParameterColor(parameterCode)
    }

    /**
     * Get default color for parameter when not configured.
     */
    private fun getDefaultParameterColor(parameterCode: String): Color {
        return when (parameterCode.lowercase()) {
            "t", "temp", "temperature" -> Color(0xFF2196F3) // Blue for temperature
            "h", "humidity" -> Color(0xFF4CAF50) // Green for humidity
            "p", "pressure" -> Color(0xFFFF9800) // Orange for pressure
            "w", "wind" -> Color(0xFF9C27B0) // Purple for wind
            "r", "rain" -> Color(0xFF42A5F5) // Light blue for rain
            else -> Color(0xFF757575) // Gray for unknown parameters
        }
    }
}