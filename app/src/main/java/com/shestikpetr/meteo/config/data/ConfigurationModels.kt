package com.shestikpetr.meteo.config.data

import com.google.gson.annotations.SerializedName

/**
 * Data models for configuration API responses.
 * These models represent the JSON structure returned by the configuration endpoints.
 */

/**
 * Generic API response wrapper for configuration endpoints
 */
data class ConfigurationApiResponse<T>(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("data")
    val data: T?,
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("error")
    val error: String? = null
)

/**
 * Validation configuration response model
 */
data class ValidationConfigResponse(
    @SerializedName("sensor_data_validation")
    val sensorDataValidation: SensorDataValidationDto,
    @SerializedName("station_number_validation")
    val stationNumberValidation: TextValidationDto,
    @SerializedName("station_name_validation")
    val stationNameValidation: TextValidationDto,
    @SerializedName("coordinate_validation")
    val coordinateValidation: NumericValidationDto,
    @SerializedName("parameter_code_validation")
    val parameterCodeValidation: TextValidationDto
)

data class SensorDataValidationDto(
    @SerializedName("min_value")
    val minValue: Double,
    @SerializedName("max_value")
    val maxValue: Double,
    @SerializedName("allow_nan")
    val allowNaN: Boolean = false,
    @SerializedName("allow_infinity")
    val allowInfinity: Boolean = false
)

data class NumericValidationDto(
    @SerializedName("min_value")
    val minValue: Double,
    @SerializedName("max_value")
    val maxValue: Double,
    @SerializedName("allow_nan")
    val allowNaN: Boolean = false,
    @SerializedName("allow_infinity")
    val allowInfinity: Boolean = false
)

data class TextValidationDto(
    @SerializedName("min_length")
    val minLength: Int,
    @SerializedName("max_length")
    val maxLength: Int,
    @SerializedName("allowed_characters")
    val allowedCharacters: String? = null,
    @SerializedName("regex_pattern")
    val regexPattern: String? = null,
    @SerializedName("required")
    val required: Boolean = true
)

/**
 * Retry policy configuration response model
 */
data class RetryConfigResponse(
    @SerializedName("sensor_data")
    val sensorData: RetryPolicyDto,
    @SerializedName("station_data")
    val stationData: RetryPolicyDto,
    @SerializedName("authentication")
    val authentication: RetryPolicyDto,
    @SerializedName("configuration")
    val configuration: RetryPolicyDto,
    @SerializedName("parameter_metadata")
    val parameterMetadata: RetryPolicyDto,
    @SerializedName("default")
    val default: RetryPolicyDto
)

data class RetryPolicyDto(
    @SerializedName("max_attempts")
    val maxAttempts: Int,
    @SerializedName("base_delay_ms")
    val baseDelayMs: Long,
    @SerializedName("max_delay_ms")
    val maxDelayMs: Long,
    @SerializedName("use_exponential_backoff")
    val useExponentialBackoff: Boolean,
    @SerializedName("retry_on_http_errors")
    val retryOnHttpErrors: Boolean,
    @SerializedName("retry_on_network_errors")
    val retryOnNetworkErrors: Boolean,
    @SerializedName("retry_on_json_errors")
    val retryOnJsonErrors: Boolean,
    @SerializedName("backoff_multiplier")
    val backoffMultiplier: Double = 2.0
)

/**
 * Theme configuration response model
 */
data class ThemeConfigResponse(
    @SerializedName("weather_colors")
    val weatherColors: WeatherColorsDto,
    @SerializedName("temperature_colors")
    val temperatureColors: TemperatureColorsDto,
    @SerializedName("status_colors")
    val statusColors: StatusColorsDto,
    @SerializedName("parameter_colors")
    val parameterColors: List<ParameterColorDto>,
    @SerializedName("dark_theme")
    val darkTheme: DarkThemeDto? = null
)

data class WeatherColorsDto(
    @SerializedName("sunny")
    val sunny: String, // Hex color string
    @SerializedName("cloudy")
    val cloudy: String,
    @SerializedName("rainy")
    val rainy: String,
    @SerializedName("snowy")
    val snowy: String,
    @SerializedName("stormy")
    val stormy: String,
    @SerializedName("foggy")
    val foggy: String
)

data class TemperatureColorsDto(
    @SerializedName("hot")
    val hot: String,
    @SerializedName("warm")
    val warm: String,
    @SerializedName("mild")
    val mild: String,
    @SerializedName("cool")
    val cool: String,
    @SerializedName("cold")
    val cold: String,
    @SerializedName("freezing")
    val freezing: String
)

data class StatusColorsDto(
    @SerializedName("online")
    val online: String,
    @SerializedName("offline")
    val offline: String,
    @SerializedName("warning")
    val warning: String,
    @SerializedName("error")
    val error: String,
    @SerializedName("inactive")
    val inactive: String
)

data class ParameterColorDto(
    @SerializedName("parameter_code")
    val parameterCode: String,
    @SerializedName("color")
    val color: String,
    @SerializedName("description")
    val description: String
)

data class DarkThemeDto(
    @SerializedName("weather_colors")
    val weatherColors: WeatherColorsDto?,
    @SerializedName("temperature_colors")
    val temperatureColors: TemperatureColorsDto?,
    @SerializedName("status_colors")
    val statusColors: StatusColorsDto?
)

/**
 * Demo configuration response model
 */
data class DemoConfigResponse(
    @SerializedName("demo_credentials")
    val demoCredentials: DemoCredentialsDto,
    @SerializedName("development_features")
    val developmentFeatures: DevelopmentFeaturesDto,
    @SerializedName("demo_data_enabled")
    val demoDataEnabled: Boolean,
    @SerializedName("environment")
    val environment: String
)

data class DemoCredentialsDto(
    @SerializedName("username")
    val username: String,
    @SerializedName("password")
    val password: String,
    @SerializedName("enabled")
    val enabled: Boolean,
    @SerializedName("description")
    val description: String? = null
)

data class DevelopmentFeaturesDto(
    @SerializedName("enable_debug_logging")
    val enableDebugLogging: Boolean,
    @SerializedName("enable_network_logs")
    val enableNetworkLogs: Boolean,
    @SerializedName("enable_performance_metrics")
    val enablePerformanceMetrics: Boolean,
    @SerializedName("mock_data_enabled")
    val mockDataEnabled: Boolean,
    @SerializedName("show_developer_options")
    val showDeveloperOptions: Boolean
)