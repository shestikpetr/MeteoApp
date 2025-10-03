package com.shestikpetr.meteo.config.impl

import android.util.Log
import androidx.compose.ui.graphics.Color
import com.shestikpetr.meteo.config.cache.ConfigurationCache
import com.shestikpetr.meteo.config.data.ThemeConfigResponse
import com.shestikpetr.meteo.config.interfaces.ThemeConfigRepository
import com.shestikpetr.meteo.config.network.ConfigurationApiService
import kotlinx.coroutines.delay
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Network-based implementation of ThemeConfigRepository with caching and fallback support.
 * Follows SOLID principles with proper dependency injection and error handling.
 * Uses simple retry logic to avoid circular dependency.
 */
@Singleton
class NetworkThemeConfigRepository @Inject constructor(
    private val configApiService: ConfigurationApiService,
    private val cache: ConfigurationCache
) : ThemeConfigRepository {

    /**
     * Gets weather condition color scheme
     */
    override suspend fun getWeatherColors(): Result<ThemeConfigRepository.WeatherColorScheme> {
        return getThemeConfig().mapCatching { config ->
            ThemeConfigRepository.WeatherColorScheme(
                sunny = parseColor(config.weatherColors.sunny),
                cloudy = parseColor(config.weatherColors.cloudy),
                rainy = parseColor(config.weatherColors.rainy),
                snowy = parseColor(config.weatherColors.snowy),
                stormy = parseColor(config.weatherColors.stormy),
                foggy = parseColor(config.weatherColors.foggy)
            )
        }
    }

    /**
     * Gets temperature-based color scheme
     */
    override suspend fun getTemperatureColors(): Result<ThemeConfigRepository.TemperatureColorScheme> {
        return getThemeConfig().mapCatching { config ->
            ThemeConfigRepository.TemperatureColorScheme(
                hot = parseColor(config.temperatureColors.hot),
                warm = parseColor(config.temperatureColors.warm),
                mild = parseColor(config.temperatureColors.mild),
                cool = parseColor(config.temperatureColors.cool),
                cold = parseColor(config.temperatureColors.cold),
                freezing = parseColor(config.temperatureColors.freezing)
            )
        }
    }

    /**
     * Gets status indicator color scheme
     */
    override suspend fun getStatusColors(): Result<ThemeConfigRepository.StatusColorScheme> {
        return getThemeConfig().mapCatching { config ->
            ThemeConfigRepository.StatusColorScheme(
                online = parseColor(config.statusColors.online),
                offline = parseColor(config.statusColors.offline),
                warning = parseColor(config.statusColors.warning),
                error = parseColor(config.statusColors.error),
                inactive = parseColor(config.statusColors.inactive)
            )
        }
    }

    /**
     * Gets color mapping for weather parameters
     */
    override suspend fun getParameterColorMappings(): Result<List<ThemeConfigRepository.ParameterColorMapping>> {
        return getThemeConfig().mapCatching { config ->
            config.parameterColors.map { dto ->
                ThemeConfigRepository.ParameterColorMapping(
                    parameterCode = dto.parameterCode,
                    color = parseColor(dto.color),
                    description = dto.description
                )
            }
        }
    }

    /**
     * Gets color for a specific parameter code
     */
    override suspend fun getColorForParameter(parameterCode: String): Result<Color> {
        return getParameterColorMappings().mapCatching { mappings ->
            mappings.find { it.parameterCode == parameterCode }?.color
                ?: getDefaultParameterColor(parameterCode)
        }
    }

    /**
     * Gets complete theme configuration
     */
    override suspend fun getThemeConfiguration(): Result<ThemeConfigRepository.ThemeConfiguration> {
        return getThemeConfig().mapCatching { config ->
            ThemeConfigRepository.ThemeConfiguration(
                weatherColors = ThemeConfigRepository.WeatherColorScheme(
                    sunny = parseColor(config.weatherColors.sunny),
                    cloudy = parseColor(config.weatherColors.cloudy),
                    rainy = parseColor(config.weatherColors.rainy),
                    snowy = parseColor(config.weatherColors.snowy),
                    stormy = parseColor(config.weatherColors.stormy),
                    foggy = parseColor(config.weatherColors.foggy)
                ),
                temperatureColors = ThemeConfigRepository.TemperatureColorScheme(
                    hot = parseColor(config.temperatureColors.hot),
                    warm = parseColor(config.temperatureColors.warm),
                    mild = parseColor(config.temperatureColors.mild),
                    cool = parseColor(config.temperatureColors.cool),
                    cold = parseColor(config.temperatureColors.cold),
                    freezing = parseColor(config.temperatureColors.freezing)
                ),
                statusColors = ThemeConfigRepository.StatusColorScheme(
                    online = parseColor(config.statusColors.online),
                    offline = parseColor(config.statusColors.offline),
                    warning = parseColor(config.statusColors.warning),
                    error = parseColor(config.statusColors.error),
                    inactive = parseColor(config.statusColors.inactive)
                ),
                parameterColors = config.parameterColors.map { dto ->
                    ThemeConfigRepository.ParameterColorMapping(
                        parameterCode = dto.parameterCode,
                        color = parseColor(dto.color),
                        description = dto.description
                    )
                },
                isDarkTheme = false
            )
        }
    }

    /**
     * Gets theme configuration for dark mode
     */
    override suspend fun getDarkThemeConfiguration(): Result<ThemeConfigRepository.ThemeConfiguration> {
        return getThemeConfig().mapCatching { config ->
            val darkTheme = config.darkTheme
            if (darkTheme != null) {
                ThemeConfigRepository.ThemeConfiguration(
                    weatherColors = darkTheme.weatherColors?.let { colors ->
                        ThemeConfigRepository.WeatherColorScheme(
                            sunny = parseColor(colors.sunny),
                            cloudy = parseColor(colors.cloudy),
                            rainy = parseColor(colors.rainy),
                            snowy = parseColor(colors.snowy),
                            stormy = parseColor(colors.stormy),
                            foggy = parseColor(colors.foggy)
                        )
                    } ?: getDefaultThemeConfiguration().weatherColors,
                    temperatureColors = darkTheme.temperatureColors?.let { colors ->
                        ThemeConfigRepository.TemperatureColorScheme(
                            hot = parseColor(colors.hot),
                            warm = parseColor(colors.warm),
                            mild = parseColor(colors.mild),
                            cool = parseColor(colors.cool),
                            cold = parseColor(colors.cold),
                            freezing = parseColor(colors.freezing)
                        )
                    } ?: getDefaultThemeConfiguration().temperatureColors,
                    statusColors = darkTheme.statusColors?.let { colors ->
                        ThemeConfigRepository.StatusColorScheme(
                            online = parseColor(colors.online),
                            offline = parseColor(colors.offline),
                            warning = parseColor(colors.warning),
                            error = parseColor(colors.error),
                            inactive = parseColor(colors.inactive)
                        )
                    } ?: getDefaultThemeConfiguration().statusColors,
                    parameterColors = config.parameterColors.map { dto ->
                        ThemeConfigRepository.ParameterColorMapping(
                            parameterCode = dto.parameterCode,
                            color = parseColor(dto.color),
                            description = dto.description
                        )
                    },
                    isDarkTheme = true
                )
            } else {
                // Fallback to light theme configuration
                getThemeConfiguration().getOrThrow().copy(isDarkTheme = true)
            }
        }
    }

    /**
     * Gets the default fallback theme configuration
     */
    override fun getDefaultThemeConfiguration(): ThemeConfigRepository.ThemeConfiguration {
        return ThemeConfigRepository.ThemeConfiguration(
            weatherColors = ThemeConfigRepository.WeatherColorScheme(
                sunny = Color(0xFFFFD54F), // Current hardcoded values from MeteoColors.kt
                cloudy = Color(0xFF90A4AE),
                rainy = Color(0xFF42A5F5),
                snowy = Color(0xFFE1F5FE),
                stormy = Color(0xFF5D4037),
                foggy = Color(0xFFBDBDBD)
            ),
            temperatureColors = ThemeConfigRepository.TemperatureColorScheme(
                hot = Color(0xFFFF5722),
                warm = Color(0xFFFF9800),
                mild = Color(0xFF4CAF50),
                cool = Color(0xFF2196F3),
                cold = Color(0xFF3F51B5),
                freezing = Color(0xFF9C27B0)
            ),
            statusColors = ThemeConfigRepository.StatusColorScheme(
                online = Color(0xFF4CAF50),
                offline = Color(0xFF757575),
                warning = Color(0xFFFF9800),
                error = Color(0xFFF44336),
                inactive = Color(0xFFBDBDBD)
            ),
            parameterColors = emptyList(),
            isDarkTheme = false
        )
    }

    /**
     * Forces refresh of theme configuration from remote source
     */
    override suspend fun refreshConfiguration(): Result<Unit> {
        return try {
            cache.invalidate(ConfigurationCache.THEME_CONFIG_KEY)
            getThemeConfig()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ThemeConfig", "Failed to refresh configuration", e)
            Result.failure(e)
        }
    }

    /**
     * Gets theme configuration with caching and retry logic
     */
    private suspend fun getThemeConfig(): Result<ThemeConfigResponse> {
        // Try cache first
        cache.get<ThemeConfigResponse>(ConfigurationCache.THEME_CONFIG_KEY)?.let { cached ->
            Log.d("ThemeConfig", "Using cached theme config")
            return Result.success(cached)
        }

        // Server config endpoints don't exist yet, use defaults immediately
        Log.d("ThemeConfig", "Using default theme config (server endpoints not available)")
        val defaultConfig = getDefaultThemeConfigResponse()

        // Cache the default config for consistency
        cache.put(ConfigurationCache.THEME_CONFIG_KEY, defaultConfig)

        return Result.success(defaultConfig)
    }

    /**
     * Parses hex color string to Compose Color
     */
    private fun parseColor(colorString: String): Color {
        return try {
            val hex = if (colorString.startsWith("#")) {
                colorString.substring(1)
            } else {
                colorString
            }

            when (hex.length) {
                6 -> Color(android.graphics.Color.parseColor("#$hex"))
                8 -> Color(android.graphics.Color.parseColor("#$hex"))
                else -> Color.Gray // Fallback color
            }
        } catch (e: Exception) {
            Log.w("ThemeConfig", "Failed to parse color: $colorString, using gray fallback", e)
            Color.Gray
        }
    }

    /**
     * Gets default color for parameter code when not configured
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

    /**
     * Provides default theme configuration response as fallback
     */
    private fun getDefaultThemeConfigResponse(): ThemeConfigResponse {
        val defaultConfig = getDefaultThemeConfiguration()

        return ThemeConfigResponse(
            weatherColors = com.shestikpetr.meteo.config.data.WeatherColorsDto(
                sunny = "#FFD54F",
                cloudy = "#90A4AE",
                rainy = "#42A5F5",
                snowy = "#E1F5FE",
                stormy = "#5D4037",
                foggy = "#BDBDBD"
            ),
            temperatureColors = com.shestikpetr.meteo.config.data.TemperatureColorsDto(
                hot = "#FF5722",
                warm = "#FF9800",
                mild = "#4CAF50",
                cool = "#2196F3",
                cold = "#3F51B5",
                freezing = "#9C27B0"
            ),
            statusColors = com.shestikpetr.meteo.config.data.StatusColorsDto(
                online = "#4CAF50",
                offline = "#757575",
                warning = "#FF9800",
                error = "#F44336",
                inactive = "#BDBDBD"
            ),
            parameterColors = emptyList(),
            darkTheme = null
        )
    }
}