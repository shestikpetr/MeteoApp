package com.shestikpetr.meteo.config.interfaces

import androidx.compose.ui.graphics.Color

/**
 * Repository interface for theme configuration following Interface Segregation Principle.
 * Provides dynamic color schemes and theme configurations.
 */
interface ThemeConfigRepository {

    /**
     * Data class representing weather condition colors
     */
    data class WeatherColorScheme(
        val sunny: Color,
        val cloudy: Color,
        val rainy: Color,
        val snowy: Color,
        val stormy: Color,
        val foggy: Color
    )

    /**
     * Data class representing temperature-based colors
     */
    data class TemperatureColorScheme(
        val hot: Color,
        val warm: Color,
        val mild: Color,
        val cool: Color,
        val cold: Color,
        val freezing: Color
    )

    /**
     * Data class representing status indicator colors
     */
    data class StatusColorScheme(
        val online: Color,
        val offline: Color,
        val warning: Color,
        val error: Color,
        val inactive: Color
    )

    /**
     * Data class representing parameter-specific colors
     */
    data class ParameterColorMapping(
        val parameterCode: String,
        val color: Color,
        val description: String
    )

    /**
     * Data class representing complete theme configuration
     */
    data class ThemeConfiguration(
        val weatherColors: WeatherColorScheme,
        val temperatureColors: TemperatureColorScheme,
        val statusColors: StatusColorScheme,
        val parameterColors: List<ParameterColorMapping>,
        val isDarkTheme: Boolean = false
    )

    /**
     * Gets weather condition color scheme
     */
    suspend fun getWeatherColors(): Result<WeatherColorScheme>

    /**
     * Gets temperature-based color scheme
     */
    suspend fun getTemperatureColors(): Result<TemperatureColorScheme>

    /**
     * Gets status indicator color scheme
     */
    suspend fun getStatusColors(): Result<StatusColorScheme>

    /**
     * Gets color mapping for weather parameters
     */
    suspend fun getParameterColorMappings(): Result<List<ParameterColorMapping>>

    /**
     * Gets color for a specific parameter code
     */
    suspend fun getColorForParameter(parameterCode: String): Result<Color>

    /**
     * Gets complete theme configuration
     */
    suspend fun getThemeConfiguration(): Result<ThemeConfiguration>

    /**
     * Gets theme configuration for dark mode
     */
    suspend fun getDarkThemeConfiguration(): Result<ThemeConfiguration>

    /**
     * Gets the default fallback theme configuration
     */
    fun getDefaultThemeConfiguration(): ThemeConfiguration

    /**
     * Forces refresh of theme configuration from remote source
     */
    suspend fun refreshConfiguration(): Result<Unit>
}