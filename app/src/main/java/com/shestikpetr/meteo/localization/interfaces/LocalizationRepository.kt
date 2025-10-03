package com.shestikpetr.meteo.localization.interfaces

/**
 * Repository interface for localization data following Repository Pattern
 * Abstracts the data source for string resources (network, cache, embedded)
 */
interface LocalizationRepository {
    suspend fun loadStrings(locale: String): Result<Map<String, String>>
    suspend fun getSupportedLocales(): Result<List<String>>
    suspend fun isLocaleSupported(locale: String): Boolean
}

/**
 * Interface for caching localization data
 * Follows Single Responsibility Principle - only handles caching
 */
interface LocalizationCache {
    suspend fun getCachedStrings(locale: String): Map<String, String>?
    suspend fun cacheStrings(locale: String, strings: Map<String, String>)
    suspend fun clearCache()
    suspend fun isCacheValid(locale: String): Boolean
    suspend fun getCacheAge(locale: String): Long?
}

/**
 * Interface for embedded fallback strings
 * Ensures offline functionality when network is unavailable
 */
interface EmbeddedStringProvider {
    fun getEmbeddedStrings(locale: String): Map<String, String>
    fun getSupportedLocales(): List<String>
    fun getDefaultLocale(): String
}

/**
 * Interface for network-based string loading
 * Follows Dependency Inversion Principle - depends on abstraction
 */
interface NetworkStringLoader {
    suspend fun loadStringsFromNetwork(locale: String): Result<Map<String, String>>
    suspend fun loadSupportedLocales(): Result<List<String>>
}