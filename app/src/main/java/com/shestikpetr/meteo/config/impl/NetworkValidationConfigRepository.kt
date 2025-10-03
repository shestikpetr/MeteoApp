package com.shestikpetr.meteo.config.impl

import android.util.Log
import com.shestikpetr.meteo.config.cache.ConfigurationCache
import com.shestikpetr.meteo.config.data.ValidationConfigResponse
import com.shestikpetr.meteo.config.interfaces.ValidationConfigRepository
import com.shestikpetr.meteo.config.network.ConfigurationApiService
import kotlinx.coroutines.delay
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Network-based implementation of ValidationConfigRepository with caching and fallback support.
 * Follows SOLID principles with proper dependency injection and error handling.
 * Uses simple retry logic to avoid circular dependency.
 */
@Singleton
class NetworkValidationConfigRepository @Inject constructor(
    private val configApiService: ConfigurationApiService,
    private val cache: ConfigurationCache
) : ValidationConfigRepository {

    /**
     * Gets validation rule for sensor data values
     */
    override suspend fun getSensorDataValidationRule(): Result<ValidationConfigRepository.NumericValidationRule> {
        return getValidationConfig().mapCatching { config ->
            ValidationConfigRepository.NumericValidationRule(
                minValue = config.sensorDataValidation.minValue,
                maxValue = config.sensorDataValidation.maxValue,
                allowNaN = config.sensorDataValidation.allowNaN,
                allowInfinity = config.sensorDataValidation.allowInfinity
            )
        }
    }

    /**
     * Gets validation rule for station numbers
     */
    override suspend fun getStationNumberValidationRule(): Result<ValidationConfigRepository.TextValidationRule> {
        return getValidationConfig().mapCatching { config ->
            ValidationConfigRepository.TextValidationRule(
                minLength = config.stationNumberValidation.minLength,
                maxLength = config.stationNumberValidation.maxLength,
                allowedCharacters = config.stationNumberValidation.allowedCharacters,
                regexPattern = config.stationNumberValidation.regexPattern,
                required = config.stationNumberValidation.required
            )
        }
    }

    /**
     * Gets validation rule for station names
     */
    override suspend fun getStationNameValidationRule(): Result<ValidationConfigRepository.TextValidationRule> {
        return getValidationConfig().mapCatching { config ->
            ValidationConfigRepository.TextValidationRule(
                minLength = config.stationNameValidation.minLength,
                maxLength = config.stationNameValidation.maxLength,
                allowedCharacters = config.stationNameValidation.allowedCharacters,
                regexPattern = config.stationNameValidation.regexPattern,
                required = config.stationNameValidation.required
            )
        }
    }

    /**
     * Gets validation rule for coordinates (latitude/longitude)
     */
    override suspend fun getCoordinateValidationRule(): Result<ValidationConfigRepository.NumericValidationRule> {
        return getValidationConfig().mapCatching { config ->
            ValidationConfigRepository.NumericValidationRule(
                minValue = config.coordinateValidation.minValue,
                maxValue = config.coordinateValidation.maxValue,
                allowNaN = config.coordinateValidation.allowNaN,
                allowInfinity = config.coordinateValidation.allowInfinity
            )
        }
    }

    /**
     * Gets validation rule for parameter codes
     */
    override suspend fun getParameterCodeValidationRule(): Result<ValidationConfigRepository.TextValidationRule> {
        return getValidationConfig().mapCatching { config ->
            ValidationConfigRepository.TextValidationRule(
                minLength = config.parameterCodeValidation.minLength,
                maxLength = config.parameterCodeValidation.maxLength,
                allowedCharacters = config.parameterCodeValidation.allowedCharacters,
                regexPattern = config.parameterCodeValidation.regexPattern,
                required = config.parameterCodeValidation.required
            )
        }
    }

    /**
     * Validates a numeric value against sensor data rules
     */
    override suspend fun isValidSensorValue(value: Double): Boolean {
        return getSensorDataValidationRule().fold(
            onSuccess = { rule ->
                when {
                    value.isNaN() -> rule.allowNaN
                    value.isInfinite() -> rule.allowInfinity
                    else -> value >= rule.minValue && value <= rule.maxValue
                }
            },
            onFailure = {
                Log.w("ValidationConfig", "Failed to get sensor validation rule, using fallback", it)
                getDefaultSensorDataValidationRule().let { rule ->
                    when {
                        value.isNaN() -> rule.allowNaN
                        value.isInfinite() -> rule.allowInfinity
                        else -> value >= rule.minValue && value <= rule.maxValue
                    }
                }
            }
        )
    }

    /**
     * Validates a station number against current rules
     */
    override suspend fun isValidStationNumber(stationNumber: String): Boolean {
        return getStationNumberValidationRule().fold(
            onSuccess = { rule ->
                validateTextRule(stationNumber, rule)
            },
            onFailure = {
                Log.w("ValidationConfig", "Failed to get station number validation rule, using fallback", it)
                validateTextRule(stationNumber, getDefaultStationNumberValidationRule())
            }
        )
    }

    /**
     * Validates station name against current rules
     */
    override suspend fun isValidStationName(stationName: String): Boolean {
        return getStationNameValidationRule().fold(
            onSuccess = { rule ->
                validateTextRule(stationName, rule)
            },
            onFailure = {
                Log.w("ValidationConfig", "Failed to get station name validation rule, using fallback", it)
                validateTextRule(stationName, getDefaultStationNameValidationRule())
            }
        )
    }

    /**
     * Forces refresh of validation configuration from remote source
     */
    override suspend fun refreshConfiguration(): Result<Unit> {
        return try {
            cache.invalidate(ConfigurationCache.VALIDATION_CONFIG_KEY)
            getValidationConfig()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ValidationConfig", "Failed to refresh configuration", e)
            Result.failure(e)
        }
    }

    /**
     * Gets validation configuration with caching and retry logic
     */
    private suspend fun getValidationConfig(): Result<ValidationConfigResponse> {
        // Try cache first
        cache.get<ValidationConfigResponse>(ConfigurationCache.VALIDATION_CONFIG_KEY)?.let { cached ->
            Log.d("ValidationConfig", "Using cached validation config")
            return Result.success(cached)
        }

        // Server config endpoints don't exist yet, use defaults immediately
        Log.d("ValidationConfig", "Using default validation config (server endpoints not available)")
        val defaultConfig = getDefaultValidationConfig()

        // Cache the default config for consistency
        cache.put(ConfigurationCache.VALIDATION_CONFIG_KEY, defaultConfig)

        return Result.success(defaultConfig)
    }

    /**
     * Helper function to validate text against rules
     */
    private fun validateTextRule(text: String, rule: ValidationConfigRepository.TextValidationRule): Boolean {
        // Check required
        if (rule.required && text.isBlank()) return false

        // Check length
        if (text.length < rule.minLength || text.length > rule.maxLength) return false

        // Check allowed characters
        rule.allowedCharacters?.let { allowed ->
            if (!text.all { it.toString() in allowed }) return false
        }

        // Check regex pattern
        rule.regexPattern?.let { pattern ->
            if (!text.matches(Regex(pattern))) return false
        }

        return true
    }

    /**
     * Provides default validation configuration as fallback
     */
    private fun getDefaultValidationConfig(): ValidationConfigResponse {
        return ValidationConfigResponse(
            sensorDataValidation = getDefaultSensorDataValidationRule().let {
                com.shestikpetr.meteo.config.data.SensorDataValidationDto(
                    minValue = it.minValue,
                    maxValue = it.maxValue,
                    allowNaN = it.allowNaN,
                    allowInfinity = it.allowInfinity
                )
            },
            stationNumberValidation = getDefaultStationNumberValidationRule().let {
                com.shestikpetr.meteo.config.data.TextValidationDto(
                    minLength = it.minLength,
                    maxLength = it.maxLength,
                    allowedCharacters = it.allowedCharacters,
                    regexPattern = it.regexPattern,
                    required = it.required
                )
            },
            stationNameValidation = getDefaultStationNameValidationRule().let {
                com.shestikpetr.meteo.config.data.TextValidationDto(
                    minLength = it.minLength,
                    maxLength = it.maxLength,
                    allowedCharacters = it.allowedCharacters,
                    regexPattern = it.regexPattern,
                    required = it.required
                )
            },
            coordinateValidation = getDefaultCoordinateValidationRule().let {
                com.shestikpetr.meteo.config.data.NumericValidationDto(
                    minValue = it.minValue,
                    maxValue = it.maxValue,
                    allowNaN = it.allowNaN,
                    allowInfinity = it.allowInfinity
                )
            },
            parameterCodeValidation = getDefaultParameterCodeValidationRule().let {
                com.shestikpetr.meteo.config.data.TextValidationDto(
                    minLength = it.minLength,
                    maxLength = it.maxLength,
                    allowedCharacters = it.allowedCharacters,
                    regexPattern = it.regexPattern,
                    required = it.required
                )
            }
        )
    }

    /**
     * Default fallback validation rules
     */
    private fun getDefaultSensorDataValidationRule() = ValidationConfigRepository.NumericValidationRule(
        minValue = -100.0, // Current hardcoded value
        maxValue = 1000.0,
        allowNaN = false,
        allowInfinity = false
    )

    private fun getDefaultStationNumberValidationRule() = ValidationConfigRepository.TextValidationRule(
        minLength = 1,
        maxLength = 8, // Current hardcoded value
        allowedCharacters = "0123456789", // Current hardcoded logic
        regexPattern = "\\d{1,8}", // Current hardcoded logic
        required = true
    )

    private fun getDefaultStationNameValidationRule() = ValidationConfigRepository.TextValidationRule(
        minLength = 1,
        maxLength = 100,
        allowedCharacters = null,
        regexPattern = null,
        required = true
    )

    private fun getDefaultCoordinateValidationRule() = ValidationConfigRepository.NumericValidationRule(
        minValue = -180.0,
        maxValue = 180.0,
        allowNaN = false,
        allowInfinity = false
    )

    private fun getDefaultParameterCodeValidationRule() = ValidationConfigRepository.TextValidationRule(
        minLength = 1,
        maxLength = 10,
        allowedCharacters = null,
        regexPattern = null,
        required = true
    )
}