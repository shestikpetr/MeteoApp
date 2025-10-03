package com.shestikpetr.meteo.config.interfaces

/**
 * Repository interface for validation configuration following Interface Segregation Principle.
 * Provides dynamic validation rules for different data types and scenarios.
 */
interface ValidationConfigRepository {

    /**
     * Data class representing validation rules for numeric values
     */
    data class NumericValidationRule(
        val minValue: Double,
        val maxValue: Double,
        val allowNaN: Boolean = false,
        val allowInfinity: Boolean = false
    )

    /**
     * Data class representing validation rules for text fields
     */
    data class TextValidationRule(
        val minLength: Int,
        val maxLength: Int,
        val allowedCharacters: String? = null,
        val regexPattern: String? = null,
        val required: Boolean = true
    )

    /**
     * Gets validation rule for sensor data values
     */
    suspend fun getSensorDataValidationRule(): Result<NumericValidationRule>

    /**
     * Gets validation rule for station numbers
     */
    suspend fun getStationNumberValidationRule(): Result<TextValidationRule>

    /**
     * Gets validation rule for station names
     */
    suspend fun getStationNameValidationRule(): Result<TextValidationRule>

    /**
     * Gets validation rule for coordinates (latitude/longitude)
     */
    suspend fun getCoordinateValidationRule(): Result<NumericValidationRule>

    /**
     * Gets validation rule for parameter codes
     */
    suspend fun getParameterCodeValidationRule(): Result<TextValidationRule>

    /**
     * Validates a numeric value against sensor data rules
     */
    suspend fun isValidSensorValue(value: Double): Boolean

    /**
     * Validates a station number against current rules
     */
    suspend fun isValidStationNumber(stationNumber: String): Boolean

    /**
     * Validates station name against current rules
     */
    suspend fun isValidStationName(stationName: String): Boolean

    /**
     * Forces refresh of validation configuration from remote source
     */
    suspend fun refreshConfiguration(): Result<Unit>
}