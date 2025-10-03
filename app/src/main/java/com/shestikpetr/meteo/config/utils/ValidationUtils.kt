package com.shestikpetr.meteo.config.utils

import android.util.Log
import com.shestikpetr.meteo.config.interfaces.ValidationConfigRepository
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class for handling validation logic using dynamic configuration.
 * Provides both suspending and blocking validation methods for UI compatibility.
 */
@Singleton
class ValidationUtils @Inject constructor(
    private val validationConfigRepository: ValidationConfigRepository
) {

    /**
     * Validates station number input with dynamic configuration.
     * Used for UI input validation.
     */
    fun validateStationNumberInput(input: String): Boolean {
        return runBlocking {
            try {
                validationConfigRepository.isValidStationNumber(input)
            } catch (e: Exception) {
                Log.w("ValidationUtils", "Failed to validate station number, using fallback", e)
                // Fallback to original hardcoded logic
                input.length <= 8 && input.all { it.isDigit() }
            }
        }
    }

    /**
     * Gets the validation filter for station number input.
     * Returns a function that can be used to filter input characters.
     */
    fun getStationNumberInputFilter(): (String) -> String {
        return { input ->
            runBlocking {
                try {
                    val rule = validationConfigRepository.getStationNumberValidationRule().getOrThrow()
                    filterTextByRule(input, rule)
                } catch (e: Exception) {
                    Log.w("ValidationUtils", "Failed to get station number validation rule, using fallback", e)
                    // Fallback to original hardcoded logic
                    if (input.length <= 8 && input.all { it.isDigit() }) input else input.dropLast(1)
                }
            }
        }
    }

    /**
     * Validates station name with dynamic configuration.
     */
    fun validateStationName(name: String): Boolean {
        return runBlocking {
            try {
                validationConfigRepository.isValidStationName(name)
            } catch (e: Exception) {
                Log.w("ValidationUtils", "Failed to validate station name, using fallback", e)
                name.isNotBlank() && name.length <= 100 // Reasonable fallback
            }
        }
    }

    /**
     * Gets the maximum length for station numbers from configuration.
     */
    fun getStationNumberMaxLength(): Int {
        return runBlocking {
            try {
                validationConfigRepository.getStationNumberValidationRule()
                    .getOrThrow().maxLength
            } catch (e: Exception) {
                Log.w("ValidationUtils", "Failed to get station number max length, using fallback", e)
                8 // Original hardcoded value
            }
        }
    }

    /**
     * Gets the allowed characters for station numbers from configuration.
     */
    fun getStationNumberAllowedCharacters(): String? {
        return runBlocking {
            try {
                validationConfigRepository.getStationNumberValidationRule()
                    .getOrThrow().allowedCharacters
            } catch (e: Exception) {
                Log.w("ValidationUtils", "Failed to get station number allowed characters, using fallback", e)
                "0123456789" // Original hardcoded logic
            }
        }
    }

    /**
     * Validates a coordinate value (latitude/longitude).
     */
    fun validateCoordinate(coordinate: Double): Boolean {
        return runBlocking {
            try {
                val rule = validationConfigRepository.getCoordinateValidationRule().getOrThrow()
                when {
                    coordinate.isNaN() -> rule.allowNaN
                    coordinate.isInfinite() -> rule.allowInfinity
                    else -> coordinate >= rule.minValue && coordinate <= rule.maxValue
                }
            } catch (e: Exception) {
                Log.w("ValidationUtils", "Failed to validate coordinate, using fallback", e)
                coordinate >= -180.0 && coordinate <= 180.0 // Reasonable fallback
            }
        }
    }

    /**
     * Validates a parameter code with dynamic configuration.
     */
    fun validateParameterCode(parameterCode: String): Boolean {
        return runBlocking {
            try {
                val rule = validationConfigRepository.getParameterCodeValidationRule().getOrThrow()
                validateTextRule(parameterCode, rule)
            } catch (e: Exception) {
                Log.w("ValidationUtils", "Failed to validate parameter code, using fallback", e)
                parameterCode.isNotBlank() && parameterCode.length <= 10 // Reasonable fallback
            }
        }
    }

    /**
     * Helper function to filter text input based on validation rules.
     */
    private fun filterTextByRule(
        input: String,
        rule: ValidationConfigRepository.TextValidationRule
    ): String {
        var filtered = input

        // Enforce max length
        if (filtered.length > rule.maxLength) {
            filtered = filtered.take(rule.maxLength)
        }

        // Filter allowed characters
        rule.allowedCharacters?.let { allowedChars ->
            filtered = filtered.filter { char -> char.toString() in allowedChars }
        }

        return filtered
    }

    /**
     * Helper function to validate text against validation rules.
     */
    private fun validateTextRule(
        text: String,
        rule: ValidationConfigRepository.TextValidationRule
    ): Boolean {
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
}