package com.shestikpetr.meteo.service

import android.util.Log
import com.shestikpetr.meteo.model.ParameterConfig
import com.shestikpetr.meteo.model.ParameterConfigSet
import com.shestikpetr.meteo.network.AuthManager
import com.shestikpetr.meteo.network.MeteoApiService
import com.shestikpetr.meteo.network.ParameterInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for managing dynamic parameter configurations loaded from API.
 * Follows Single Responsibility Principle by focusing solely on parameter configuration management.
 *
 * This service handles:
 * - Loading parameter configurations from API for specific stations
 * - Caching parameter configurations per station and globally
 * - Providing fallback configurations when API is unavailable
 * - Managing default parameter selection
 * - Thread-safe operations for configuration access
 */
@Singleton
class ParameterConfigService @Inject constructor(
    private val meteoApiService: MeteoApiService,
    private val authManager: AuthManager
) {
    companion object {
        private const val TAG = "ParameterConfigService"
        private const val GLOBAL_CACHE_KEY = "_global_"
    }

    // Cache for parameter configurations per station
    private val stationConfigCache = mutableMapOf<String, ParameterConfigSet>()
    private val cacheMutex = Mutex()

    // Global parameter configuration state
    private val _globalParameterConfig = MutableStateFlow(ParameterConfigSet.fallback())
    val globalParameterConfig: StateFlow<ParameterConfigSet> = _globalParameterConfig.asStateFlow()

    // Current station parameter configuration state
    private val _currentStationConfig = MutableStateFlow(ParameterConfigSet.fallback())
    val currentStationConfig: StateFlow<ParameterConfigSet> = _currentStationConfig.asStateFlow()

    /**
     * Gets parameter configuration for a specific station.
     * Uses cached data when available, loads from API otherwise.
     *
     * @param stationNumber The station number
     * @return ParameterConfigSet for the station
     */
    suspend fun getStationParameterConfig(stationNumber: String): ParameterConfigSet {
        return cacheMutex.withLock {
            // Check cache first
            stationConfigCache[stationNumber]?.let { cachedConfig ->
                Log.d(TAG, "Using cached parameter config for station $stationNumber (${cachedConfig.parameters.size} parameters)")
                return@withLock cachedConfig
            }

            // Load from API if not cached
            try {
                val configSet = loadStationParameterConfig(stationNumber)
                stationConfigCache[stationNumber] = configSet
                Log.d(TAG, "Loaded ${configSet.parameters.size} parameters for station $stationNumber")
                configSet
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load parameters for station $stationNumber: ${e.message}")
                ParameterConfigSet.fallback()
            }
        }
    }

    /**
     * Sets the current station and updates the current station config state.
     *
     * @param stationNumber The station number to set as current
     */
    suspend fun setCurrentStation(stationNumber: String?) {
        if (stationNumber == null) {
            _currentStationConfig.value = ParameterConfigSet.fallback()
            return
        }

        val configSet = getStationParameterConfig(stationNumber)
        _currentStationConfig.value = configSet
        Log.d(TAG, "Set current station $stationNumber with ${configSet.parameters.size} parameters")
    }

    /**
     * Gets parameter code for legacy Parameters enum compatibility.
     * This method provides backward compatibility during migration.
     *
     * @param stationNumber The station number
     * @param legacyParameter The legacy parameter type name
     * @return The parameter code string
     */
    suspend fun getLegacyParameterCode(stationNumber: String, legacyParameter: String): String {
        val configSet = getStationParameterConfig(stationNumber)

        // Try to find parameter by name matching
        val matchingParam = configSet.parameters.find { param ->
            when (legacyParameter.uppercase()) {
                "TEMPERATURE" -> param.name.lowercase().contains("температур") ||
                                param.code.uppercase() == "T" ||
                                param.code == "4402"
                "HUMIDITY" -> param.name.lowercase().contains("влажность") ||
                             param.code.uppercase() == "H" ||
                             param.code == "5402"
                "PRESSURE" -> param.name.lowercase().contains("давление") ||
                             param.code.uppercase() == "P" ||
                             param.code == "700"
                else -> false
            }
        }

        return matchingParam?.code ?: run {
            Log.w(TAG, "Legacy parameter $legacyParameter not found for station $stationNumber, using fallback")
            getFallbackCode(legacyParameter)
        }
    }

    /**
     * Preloads parameter configurations for a station.
     * Useful for preemptive loading when station is selected.
     *
     * @param stationNumber The station number to preload parameters for
     */
    suspend fun preloadStationParameters(stationNumber: String) {
        cacheMutex.withLock {
            if (!stationConfigCache.containsKey(stationNumber)) {
                try {
                    val configSet = loadStationParameterConfig(stationNumber)
                    stationConfigCache[stationNumber] = configSet
                    Log.d(TAG, "Preloaded ${configSet.parameters.size} parameters for station $stationNumber")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to preload parameters for station $stationNumber: ${e.message}")
                }
            }
        }
    }

    /**
     * Loads global parameter configuration.
     * This can be used to get all available parameter types across all stations.
     */
    suspend fun loadGlobalParameterConfig() {
        try {
            // For now, use fallback global config
            // In future, this could load from a global parameters endpoint
            val globalConfig = ParameterConfigSet.fallback()
            _globalParameterConfig.value = globalConfig
            Log.d(TAG, "Loaded global parameter config with ${globalConfig.parameters.size} parameters")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load global parameter config: ${e.message}")
            _globalParameterConfig.value = ParameterConfigSet.fallback()
        }
    }

    /**
     * Checks if a station has a specific parameter available by code.
     *
     * @param stationNumber The station number
     * @param parameterCode The parameter code to check
     * @return true if parameter is available, false otherwise
     */
    suspend fun hasParameter(stationNumber: String, parameterCode: String): Boolean {
        val configSet = getStationParameterConfig(stationNumber)
        return configSet.hasParameter(parameterCode)
    }

    /**
     * Gets parameter config by code for a specific station.
     *
     * @param stationNumber The station number
     * @param parameterCode The parameter code
     * @return ParameterConfig if found, null otherwise
     */
    suspend fun getParameterConfig(stationNumber: String, parameterCode: String): ParameterConfig? {
        val configSet = getStationParameterConfig(stationNumber)
        return configSet.getByCode(parameterCode)
    }

    /**
     * Clears the cache for a specific station.
     * Useful when station parameters might have changed.
     *
     * @param stationNumber The station number to clear cache for
     */
    suspend fun clearStationCache(stationNumber: String) {
        cacheMutex.withLock {
            stationConfigCache.remove(stationNumber)
            Log.d(TAG, "Cleared parameter cache for station $stationNumber")
        }
    }

    /**
     * Clears all cached parameter configurations.
     */
    suspend fun clearAllCache() {
        cacheMutex.withLock {
            stationConfigCache.clear()
            Log.d(TAG, "Cleared all parameter cache")
        }

        // Reset to fallback configurations
        _globalParameterConfig.value = ParameterConfigSet.fallback()
        _currentStationConfig.value = ParameterConfigSet.fallback()
    }

    /**
     * Loads parameter configuration from API for a specific station.
     *
     * @param stationNumber The station number to load parameters for
     * @return ParameterConfigSet with loaded configurations
     */
    private suspend fun loadStationParameterConfig(stationNumber: String): ParameterConfigSet {
        val authToken = authManager.getAccessToken()
            ?: throw IllegalStateException("No access token available")

        Log.d(TAG, "Loading parameter config for station $stationNumber from API")

        val response = meteoApiService.getStationParameters(stationNumber, "Bearer $authToken")

        if (!response.isSuccessful) {
            throw Exception("API request failed: ${response.code()} ${response.message()}")
        }

        val apiResponse = response.body()
        if (apiResponse?.success != true) {
            throw Exception("API response unsuccessful")
        }

        val parameterInfoList = apiResponse.data ?: emptyList()
        Log.d(TAG, "Received ${parameterInfoList.size} parameters for station $stationNumber")

        return createParameterConfigSet(parameterInfoList)
    }

    /**
     * Creates ParameterConfigSet from API ParameterInfo list.
     *
     * @param parameterInfoList List of parameter information from API
     * @return ParameterConfigSet with proper ordering and defaults
     */
    private fun createParameterConfigSet(parameterInfoList: List<ParameterInfo>): ParameterConfigSet {
        val configs = parameterInfoList.mapIndexed { index, parameterInfo ->
            ParameterConfig.fromParameterInfo(
                parameterInfo = parameterInfo,
                displayOrder = index + 1,
                isDefault = isDefaultParameter(parameterInfo)
            )
        }

        // Find default parameter (temperature preferred, then first available)
        val defaultCode = configs.find { it.isDefault }?.code
            ?: configs.find { it.name.lowercase().contains("температур") }?.code
            ?: configs.firstOrNull()?.code

        return ParameterConfigSet(
            parameters = configs,
            defaultParameterCode = defaultCode
        )
    }

    /**
     * Determines if a parameter should be marked as default.
     *
     * @param parameterInfo The parameter information
     * @return true if this parameter should be default
     */
    private fun isDefaultParameter(parameterInfo: ParameterInfo): Boolean {
        val name = parameterInfo.name.lowercase()
        val code = parameterInfo.code.lowercase()

        // Temperature is preferred default
        return name.contains("температур") ||
               code == "t" ||
               code == "4402"
    }

    /**
     * Gets fallback parameter code for legacy compatibility.
     *
     * @param legacyParameter The legacy parameter name
     * @return Fallback parameter code
     */
    private fun getFallbackCode(legacyParameter: String): String {
        return when (legacyParameter.uppercase()) {
            "TEMPERATURE" -> "4402"
            "HUMIDITY" -> "5402"
            "PRESSURE" -> "700"
            else -> throw IllegalArgumentException("No fallback code available for parameter: $legacyParameter")
        }
    }
}