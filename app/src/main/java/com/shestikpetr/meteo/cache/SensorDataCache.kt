package com.shestikpetr.meteo.cache

import android.util.Log
import com.shestikpetr.meteo.config.interfaces.ValidationConfigRepository
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thread-safe cache for storing sensor data values to reduce network requests and provide immediate feedback.
 *
 * This class handles caching of sensor data with cache keys based on station ID and parameter code.
 * It provides storage, retrieval, and cache management functionality with full thread safety
 * using ConcurrentHashMap for concurrent access from multiple coroutines.
 *
 * Updated to use dynamic parameter codes instead of hardcoded Parameters enum.
 */
@Singleton
class SensorDataCache @Inject constructor(
    private val validationConfigRepository: ValidationConfigRepository
) {

    private val sensorDataCache = ConcurrentHashMap<String, Double>()

    /**
     * Generates a cache key for storing/retrieving sensor data.
     *
     * @param stationId The station identifier
     * @param parameterCode The weather parameter code (e.g., "4402", "T", "H")
     * @return A unique cache key string
     */
    private fun getCacheKey(stationId: String, parameterCode: String): String {
        return "${stationId}_${parameterCode}"
    }

    /**
     * Stores a value in the cache for the given station and parameter.
     *
     * @param stationId The station identifier
     * @param parameterCode The weather parameter code
     * @param value The sensor data value to cache
     */
    fun putValue(stationId: String, parameterCode: String, value: Double) {
        val cacheKey = getCacheKey(stationId, parameterCode)
        sensorDataCache[cacheKey] = value
        Log.d("SensorDataCache", "Cached value for $stationId-$parameterCode: $value")
    }

    /**
     * Retrieves a cached value for the given station and parameter.
     *
     * @param stationId The station identifier
     * @param parameterCode The weather parameter code
     * @return The cached value or null if not found
     */
    fun getValue(stationId: String, parameterCode: String): Double? {
        val cacheKey = getCacheKey(stationId, parameterCode)
        return sensorDataCache[cacheKey]
    }

    /**
     * Checks if a value exists in the cache for the given station and parameter.
     *
     * @param stationId The station identifier
     * @param parameterCode The weather parameter code
     * @return true if cached data exists, false otherwise
     */
    fun hasValue(stationId: String, parameterCode: String): Boolean {
        val cacheKey = getCacheKey(stationId, parameterCode)
        return sensorDataCache.containsKey(cacheKey)
    }

    /**
     * Gets all cached values for the specified parameter across all stations.
     *
     * @param parameterCode The weather parameter code
     * @return A map of station IDs to their cached values
     */
    fun getValuesForParameter(parameterCode: String): Map<String, Double> {
        return sensorDataCache
            .filterKeys { it.endsWith("_$parameterCode") }
            .mapKeys { it.key.substringBefore("_$parameterCode") }
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
     * @param parameterCode The weather parameter code
     * @return A set of station IDs with cached data
     */
    fun getCachedStationsForParameter(parameterCode: String): Set<String> {
        return sensorDataCache.keys
            .filter { it.endsWith("_$parameterCode") }
            .map { it.substringBefore("_$parameterCode") }
            .toSet()
    }

    /**
     * Removes cached value for a specific station and parameter.
     *
     * @param stationId The station identifier
     * @param parameterCode The weather parameter code
     */
    fun removeValue(stationId: String, parameterCode: String) {
        val cacheKey = getCacheKey(stationId, parameterCode)
        sensorDataCache.remove(cacheKey)
        Log.d("SensorDataCache", "Removed cached value for $stationId-$parameterCode")
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
     * Uses dynamic validation configuration instead of hardcoded threshold.
     *
     * @param value The value to validate
     * @return true if the value is valid for display, false otherwise
     */
    suspend fun isValidValue(value: Double): Boolean {
        return validationConfigRepository.isValidSensorValue(value)
    }

}