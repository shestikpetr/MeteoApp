package com.shestikpetr.meteo.service

import android.util.Log
import com.shestikpetr.meteo.model.ParameterConfig
import com.shestikpetr.meteo.model.ParameterConfigSet
import com.shestikpetr.meteo.network.AuthManager
import com.shestikpetr.meteo.network.MeteoApiService
import com.shestikpetr.meteo.network.ParameterInfo
import com.shestikpetr.meteo.repository.interfaces.ParameterDisplayRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for managing dynamic parameter configurations using repository pattern.
 * Follows SOLID principles:
 * - SRP: Focuses solely on parameter configuration service layer
 * - OCP: Extensible through repository interface
 * - LSP: Consistent service interface
 * - ISP: Uses segregated repository interfaces
 * - DIP: Depends on repository abstraction, not implementation
 *
 * This service handles:
 * - High-level parameter configuration operations
 * - State management for current station configuration
 * - Global parameter configuration state
 * - Service-layer caching and optimization
 */
@Singleton
class ParameterConfigService @Inject constructor(
    private val parameterDisplayRepository: ParameterDisplayRepository
) {
    companion object {
        private const val TAG = "ParameterConfigService"
        private const val DEFAULT_LOCALE = "ru"
    }

    // Global parameter configuration state
    private val _globalParameterConfig = MutableStateFlow(ParameterConfigSet.fallback())
    val globalParameterConfig: StateFlow<ParameterConfigSet> = _globalParameterConfig.asStateFlow()

    // Current station parameter configuration state
    private val _currentStationConfig = MutableStateFlow(ParameterConfigSet.fallback())
    val currentStationConfig: StateFlow<ParameterConfigSet> = _currentStationConfig.asStateFlow()

    // Current station number for state management
    private var currentStationNumber: String? = null

    // Current locale for localization
    private var currentLocale: String = DEFAULT_LOCALE

    /**
     * Gets parameter configuration for a specific station.
     * Uses repository layer with caching and fallback support.
     *
     * @param stationNumber The station number
     * @param locale The locale for localized parameter names (default: current locale)
     * @return ParameterConfigSet for the station
     */
    suspend fun getStationParameterConfig(stationNumber: String, locale: String = currentLocale): ParameterConfigSet {
        return try {
            val configSet = parameterDisplayRepository.getStationParameterConfig(stationNumber, locale)
            Log.d(TAG, "Retrieved ${configSet.parameters.size} parameters for station $stationNumber")
            configSet
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load parameters for station $stationNumber: ${e.message}")
            Log.d(TAG, "Using fallback configuration")
            ParameterConfigSet.fallback()
        }
    }

    /**
     * Sets the current station and updates the current station config state.
     *
     * @param stationNumber The station number to set as current
     * @param locale The locale for localized parameter names (optional)
     */
    suspend fun setCurrentStation(stationNumber: String?, locale: String? = null) {
        currentStationNumber = stationNumber
        if (locale != null) {
            currentLocale = locale
        }

        if (stationNumber == null) {
            _currentStationConfig.value = ParameterConfigSet.fallback()
            Log.d(TAG, "Cleared current station")
            return
        }

        val configSet = getStationParameterConfig(stationNumber, currentLocale)
        _currentStationConfig.value = configSet
        Log.d(TAG, "Set current station $stationNumber with ${configSet.parameters.size} parameters")
    }

    /**
     * Sets the current locale for parameter localization.
     *
     * @param locale The locale to use (e.g., "ru", "en")
     */
    suspend fun setLocale(locale: String) {
        if (currentLocale != locale) {
            currentLocale = locale
            Log.d(TAG, "Changed locale to $locale")

            // Refresh current station config with new locale
            currentStationNumber?.let { stationNumber ->
                val configSet = getStationParameterConfig(stationNumber, locale)
                _currentStationConfig.value = configSet
            }

            // Refresh global config with new locale
            loadGlobalParameterConfig()
        }
    }

    /**
     * Preloads parameter configurations for a station.
     * This calls the repository which handles its own caching.
     *
     * @param stationNumber The station number to preload parameters for
     * @param locale The locale for localized parameter names (optional)
     */
    suspend fun preloadStationParameters(stationNumber: String, locale: String = currentLocale) {
        try {
            val configSet = parameterDisplayRepository.getStationParameterConfig(stationNumber, locale)
            Log.d(TAG, "Preloaded ${configSet.parameters.size} parameters for station $stationNumber")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to preload parameters for station $stationNumber: ${e.message}")
        }
    }

    /**
     * Loads global parameter configuration using repository.
     * This gets all available parameter types across all stations.
     */
    suspend fun loadGlobalParameterConfig() {
        try {
            val globalConfig = parameterDisplayRepository.getGlobalParameterConfig(currentLocale)
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
        return parameterDisplayRepository.isParameterAvailableForStation(stationNumber, parameterCode)
    }

    /**
     * Gets parameter config by code for a specific station.
     *
     * @param stationNumber The station number
     * @param parameterCode The parameter code
     * @param locale The locale for localized parameter names (optional)
     * @return ParameterConfig if found, null otherwise
     */
    suspend fun getParameterConfig(stationNumber: String, parameterCode: String, locale: String = currentLocale): ParameterConfig? {
        val configSet = getStationParameterConfig(stationNumber, locale)
        return configSet.getByCode(parameterCode)
    }

    /**
     * Gets parameter configuration by code (not station-specific).
     *
     * @param parameterCode The parameter code
     * @param locale The locale for localized parameter names (optional)
     * @return ParameterConfig if found, null otherwise
     */
    suspend fun getParameterConfigByCode(parameterCode: String, locale: String = currentLocale): ParameterConfig? {
        return parameterDisplayRepository.getParameterConfig(parameterCode, locale)
    }

    /**
     * Clears the cache for a specific station.
     * Useful when station parameters might have changed.
     *
     * @param stationNumber The station number to clear cache for
     */
    suspend fun clearStationCache(stationNumber: String) {
        parameterDisplayRepository.refreshParameterConfigs(stationNumber)
        Log.d(TAG, "Cleared parameter cache for station $stationNumber")
    }

    /**
     * Clears all cached parameter configurations.
     */
    suspend fun clearAllCache() {
        parameterDisplayRepository.clearParameterConfigCache()

        // Reset to fallback configurations
        _globalParameterConfig.value = ParameterConfigSet.fallback()
        _currentStationConfig.value = ParameterConfigSet.fallback()

        Log.d(TAG, "Cleared all parameter cache")
    }

}