package com.shestikpetr.meteo.cache

import android.util.Log
import com.shestikpetr.meteo.ui.Parameters
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cache for storing sensor data values to reduce network requests and provide immediate feedback.
 *
 * This class handles caching of sensor data with cache keys based on station ID and parameter type.
 * It provides storage, retrieval, and cache management functionality while maintaining thread safety.
 */
@Singleton
class SensorDataCache @Inject constructor() {

    private val sensorDataCache = mutableMapOf<String, Double>()

    /**
     * Generates a cache key for storing/retrieving sensor data.
     *
     * @param stationId The station identifier
     * @param parameter The weather parameter type
     * @return A unique cache key string
     */
    private fun getCacheKey(stationId: String, parameter: Parameters): String {
        return "${stationId}_${parameter.name}"
    }

    /**
     * Stores a value in the cache for the given station and parameter.
     *
     * @param stationId The station identifier
     * @param parameter The weather parameter type
     * @param value The sensor data value to cache
     */
    fun putValue(stationId: String, parameter: Parameters, value: Double) {
        val cacheKey = getCacheKey(stationId, parameter)
        sensorDataCache[cacheKey] = value
        Log.d("SensorDataCache", "Cached value for $stationId-${parameter.name}: $value")
    }

    /**
     * Retrieves a cached value for the given station and parameter.
     *
     * @param stationId The station identifier
     * @param parameter The weather parameter type
     * @return The cached value or null if not found
     */
    fun getValue(stationId: String, parameter: Parameters): Double? {
        val cacheKey = getCacheKey(stationId, parameter)
        return sensorDataCache[cacheKey]
    }

    /**
     * Checks if a value exists in the cache for the given station and parameter.
     *
     * @param stationId The station identifier
     * @param parameter The weather parameter type
     * @return true if cached data exists, false otherwise
     */
    fun hasValue(stationId: String, parameter: Parameters): Boolean {
        val cacheKey = getCacheKey(stationId, parameter)
        return sensorDataCache.containsKey(cacheKey)
    }

    /**
     * Gets all cached values for the specified parameter across all stations.
     *
     * @param parameter The weather parameter type
     * @return A map of station IDs to their cached values
     */
    fun getValuesForParameter(parameter: Parameters): Map<String, Double> {
        return sensorDataCache
            .filterKeys { it.endsWith("_${parameter.name}") }
            .mapKeys { it.key.substringBefore("_${parameter.name}") }
    }

    /**
     * Gets all cached values as a map of station IDs to values.
     *
     * @return A copy of all cached values
     */
    fun getAllValues(): Map<String, Double> {
        return sensorDataCache.toMap()
    }

    /**
     * Gets the list of station IDs that have cached data for the specified parameter.
     *
     * @param parameter The weather parameter type
     * @return A set of station IDs with cached data
     */
    fun getCachedStationsForParameter(parameter: Parameters): Set<String> {
        return sensorDataCache.keys
            .filter { it.endsWith("_${parameter.name}") }
            .map { it.substringBefore("_${parameter.name}") }
            .toSet()
    }

    /**
     * Removes cached value for a specific station and parameter.
     *
     * @param stationId The station identifier
     * @param parameter The weather parameter type
     */
    fun removeValue(stationId: String, parameter: Parameters) {
        val cacheKey = getCacheKey(stationId, parameter)
        sensorDataCache.remove(cacheKey)
        Log.d("SensorDataCache", "Removed cached value for $stationId-${parameter.name}")
    }

    /**
     * Removes all cached values for a specific station across all parameters.
     *
     * @param stationId The station identifier
     */
    fun removeStation(stationId: String) {
        val keysToRemove = sensorDataCache.keys.filter { it.startsWith("${stationId}_") }
        keysToRemove.forEach { sensorDataCache.remove(it) }
        Log.d("SensorDataCache", "Removed all cached values for station $stationId")
    }

    /**
     * Clears all cached values.
     */
    fun clearAll() {
        Log.d("SensorDataCache", "Clearing all cached data (${sensorDataCache.size} entries)")
        sensorDataCache.clear()
    }

    /**
     * Gets cache statistics for debugging purposes.
     *
     * @return A map containing cache size and cached values
     */
    fun getCacheInfo(): Map<String, Any> {
        return mapOf(
            "cacheSize" to sensorDataCache.size,
            "cachedValues" to sensorDataCache.toMap()
        )
    }

    /**
     * Gets the current cache size.
     *
     * @return The number of cached entries
     */
    fun size(): Int = sensorDataCache.size

    /**
     * Checks if the cache is empty.
     *
     * @return true if cache is empty, false otherwise
     */
    fun isEmpty(): Boolean = sensorDataCache.isEmpty()

    /**
     * Validates that a cached value is reasonable (not the error sentinel value).
     *
     * @param value The value to validate
     * @return true if the value is valid for display, false otherwise
     */
    fun isValidValue(value: Double): Boolean {
        return value > -100.0 // Based on the original logic from MeteoViewModel
    }
}