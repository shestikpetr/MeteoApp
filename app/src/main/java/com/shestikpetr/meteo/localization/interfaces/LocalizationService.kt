package com.shestikpetr.meteo.localization.interfaces

import kotlinx.coroutines.flow.StateFlow

/**
 * High-level service interface for localization management
 * Coordinates between repository, cache, and UI state
 * Follows Single Responsibility Principle - manages localization business logic
 */
interface LocalizationService {
    val currentLocale: StateFlow<String>
    val isLoading: StateFlow<Boolean>
    val error: StateFlow<String?>

    suspend fun initialize()
    suspend fun changeLocale(locale: String): Result<Unit>
    suspend fun refreshStrings(): Result<Unit>
    suspend fun getSupportedLocales(): List<String>
    fun getString(key: StringKey): String
    fun getString(key: StringKey, vararg args: Any): String
}

/**
 * Interface for handling string formatting and interpolation
 * Follows Open/Closed Principle - extensible for different formatting strategies
 */
interface StringFormatter {
    fun format(template: String, vararg args: Any): String
    fun formatPlural(template: String, count: Int, vararg args: Any): String
}

/**
 * Interface for locale preferences storage
 * Abstracts persistence layer for user language preferences
 */
interface LocalePreferences {
    suspend fun getPreferredLocale(): String?
    suspend fun setPreferredLocale(locale: String)
    suspend fun getSystemLocale(): String
}