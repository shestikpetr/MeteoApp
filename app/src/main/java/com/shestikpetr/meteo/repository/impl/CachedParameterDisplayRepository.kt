package com.shestikpetr.meteo.repository.impl

import android.util.Log
import com.shestikpetr.meteo.model.ParameterConfig
import com.shestikpetr.meteo.model.ParameterConfigSet
import com.shestikpetr.meteo.network.AuthManager
import com.shestikpetr.meteo.network.MeteoApiService
import com.shestikpetr.meteo.repository.interfaces.ParameterDisplayRepository
import com.shestikpetr.meteo.repository.interfaces.StationRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cached implementation of ParameterDisplayRepository with fallback support.
 * This implementation follows SOLID principles:
 * - SRP: Focuses solely on parameter display configuration management
 * - OCP: Extensible through strategy pattern for cache storage
 * - LSP: Consistent with base repository interface
 * - ISP: Implements only parameter display concerns
 * - DIP: Depends on abstractions (interfaces)
 */
@Singleton
class CachedParameterDisplayRepository @Inject constructor(
    private val meteoApiService: MeteoApiService,
    private val authManager: AuthManager,
    private val stationRepository: StationRepository
) : ParameterDisplayRepository {

    companion object {
        private const val TAG = "CachedParameterDisplayRepository"
        private const val GLOBAL_CACHE_KEY = "_global_"
        private val CACHE_DURATION_MS = 15 * 60 * 1000L // 15 minutes
    }

    // Station-specific parameter configuration cache
    private val stationConfigCache = mutableMapOf<String, CachedParameterConfigSet>()

    // Global parameter configuration cache
    private val globalConfigCache = mutableMapOf<String, CachedParameterConfigSet>()

    // Parameter config cache by individual parameter code
    private val parameterConfigCache = mutableMapOf<String, CachedParameterConfig>()

    private val cacheMutex = Mutex()

    override suspend fun getStationParameterConfig(stationNumber: String, locale: String): ParameterConfigSet {
        return cacheMutex.withLock {
            val cacheKey = "${stationNumber}_${locale}"

            // Check cache first
            stationConfigCache[cacheKey]?.let { cached ->
                if (System.currentTimeMillis() - cached.timestamp < CACHE_DURATION_MS) {
                    Log.d(TAG, "Using cached station parameter config for $stationNumber (${cached.configSet.parameters.size} parameters)")
                    return@withLock cached.configSet
                } else {
                    Log.d(TAG, "Cache expired for station $stationNumber, refreshing")
                    stationConfigCache.remove(cacheKey)
                }
            }

            // Load from API
            try {
                val configSet = loadStationParameterConfigFromApi(stationNumber, locale)
                stationConfigCache[cacheKey] = CachedParameterConfigSet(configSet, System.currentTimeMillis())
                Log.d(TAG, "Loaded ${configSet.parameters.size} parameters for station $stationNumber")
                configSet
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load parameters for station $stationNumber: ${e.message}")
                Log.d(TAG, "Using fallback configuration for station $stationNumber")
                ParameterConfigSet.fallback()
            }
        }
    }

    override suspend fun getGlobalParameterConfig(locale: String): ParameterConfigSet {
        return cacheMutex.withLock {
            val cacheKey = "${GLOBAL_CACHE_KEY}_${locale}"

            // Check cache first
            globalConfigCache[cacheKey]?.let { cached ->
                if (System.currentTimeMillis() - cached.timestamp < CACHE_DURATION_MS) {
                    Log.d(TAG, "Using cached global parameter config (${cached.configSet.parameters.size} parameters)")
                    return@withLock cached.configSet
                } else {
                    Log.d(TAG, "Global cache expired, refreshing")
                    globalConfigCache.remove(cacheKey)
                }
            }

            // Load from API
            try {
                val configSet = loadGlobalParameterConfigFromApi(locale)
                globalConfigCache[cacheKey] = CachedParameterConfigSet(configSet, System.currentTimeMillis())
                Log.d(TAG, "Loaded ${configSet.parameters.size} global parameters")
                configSet
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load global parameters: ${e.message}")
                Log.d(TAG, "Using fallback global configuration")
                ParameterConfigSet.fallback()
            }
        }
    }

    override suspend fun getParameterConfig(parameterCode: String, locale: String): ParameterConfig? {
        return cacheMutex.withLock {
            val cacheKey = "${parameterCode}_${locale}"

            // Check cache first
            parameterConfigCache[cacheKey]?.let { cached ->
                if (System.currentTimeMillis() - cached.timestamp < CACHE_DURATION_MS) {
                    Log.d(TAG, "Using cached parameter config for $parameterCode")
                    return@withLock cached.config
                } else {
                    parameterConfigCache.remove(cacheKey)
                }
            }

            // Load from API
            try {
                val config = loadParameterConfigFromApi(parameterCode, locale)
                if (config != null) {
                    parameterConfigCache[cacheKey] = CachedParameterConfig(config, System.currentTimeMillis())
                    Log.d(TAG, "Loaded parameter config for $parameterCode")
                }
                config
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load parameter config for $parameterCode: ${e.message}")

                // Try fallback from global config
                val fallbackConfig = ParameterConfigSet.fallback().getByCode(parameterCode)
                if (fallbackConfig != null) {
                    Log.d(TAG, "Using fallback config for parameter $parameterCode")
                }
                fallbackConfig
            }
        }
    }

    override suspend fun getMultipleParameterConfigs(parameterCodes: List<String>, locale: String): Map<String, ParameterConfig> {
        val result = mutableMapOf<String, ParameterConfig>()

        for (code in parameterCodes) {
            getParameterConfig(code, locale)?.let { config ->
                result[code] = config
            }
        }

        return result
    }

    override suspend fun isParameterAvailableForStation(stationNumber: String, parameterCode: String): Boolean {
        val stationConfig = getStationParameterConfig(stationNumber)
        return stationConfig.hasParameter(parameterCode)
    }

    override suspend fun getDefaultParameterForStation(stationNumber: String): ParameterConfig? {
        val stationConfig = getStationParameterConfig(stationNumber)
        return stationConfig.getDefault()
    }

    override suspend fun getParametersByCategory(stationNumber: String?, locale: String): Map<String, List<ParameterConfig>> {
        val configSet = if (stationNumber != null) {
            getStationParameterConfig(stationNumber, locale)
        } else {
            getGlobalParameterConfig(locale)
        }

        return configSet.getByCategory()
    }

    override suspend fun getParameterDisplayText(parameterCode: String, locale: String): String? {
        return getParameterConfig(parameterCode, locale)?.displayText
    }

    override suspend fun getParameterUnit(parameterCode: String): String? {
        // Units are generally not locale-specific, so we can use any locale
        return getParameterConfig(parameterCode, "ru")?.unit
    }

    override suspend fun searchParameters(query: String, stationNumber: String?, locale: String): List<ParameterConfig> {
        val configSet = if (stationNumber != null) {
            getStationParameterConfig(stationNumber, locale)
        } else {
            getGlobalParameterConfig(locale)
        }

        val lowerQuery = query.lowercase()
        return configSet.parameters.filter { param ->
            param.name.lowercase().contains(lowerQuery) ||
            param.description.lowercase().contains(lowerQuery) ||
            param.code.lowercase().contains(lowerQuery)
        }
    }

    override suspend fun refreshParameterConfigs(stationNumber: String?) {
        cacheMutex.withLock {
            if (stationNumber != null) {
                // Clear specific station cache
                val keysToRemove = stationConfigCache.keys.filter { it.startsWith("${stationNumber}_") }
                keysToRemove.forEach { stationConfigCache.remove(it) }
                Log.d(TAG, "Cleared parameter cache for station $stationNumber")
            } else {
                // Clear all caches
                stationConfigCache.clear()
                globalConfigCache.clear()
                parameterConfigCache.clear()
                Log.d(TAG, "Cleared all parameter caches")
            }
        }
    }

    override suspend fun clearParameterConfigCache() {
        cacheMutex.withLock {
            stationConfigCache.clear()
            globalConfigCache.clear()
            parameterConfigCache.clear()
            Log.d(TAG, "Cleared all parameter configuration caches")
        }
    }

    /**
     * Loads station parameter configuration from StationRepository (uses cached station data).
     */
    private suspend fun loadStationParameterConfigFromApi(stationNumber: String, locale: String): ParameterConfigSet {
        Log.d(TAG, "Loading station parameter config for $stationNumber (locale: $locale) from StationRepository")

        try {
            // First try to get parameters from station repository (uses /api/v1/stations data)
            val parameterInfoList = stationRepository.getStationParameters(stationNumber)
            Log.d(TAG, "Received ${parameterInfoList.size} active parameters for station $stationNumber from StationRepository")

            if (parameterInfoList.isNotEmpty()) {
                return createParameterConfigSetFromParameterInfo(parameterInfoList)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get parameters from StationRepository: ${e.message}, falling back to API")
        }

        // Fallback to direct API call if StationRepository fails
        val authToken = authManager.getAccessToken()
            ?: throw IllegalStateException("No access token available")

        Log.d(TAG, "Falling back to direct API call for station $stationNumber parameters")

        val response = meteoApiService.getStationParameters(stationNumber, "Bearer $authToken")

        if (!response.isSuccessful) {
            throw Exception("API request failed: ${response.code()} ${response.message()}")
        }

        val apiResponse = response.body()
        if (apiResponse?.success != true) {
            throw Exception("API response unsuccessful")
        }

        val parameterVisibilityList = apiResponse.data ?: emptyList()
        Log.d(TAG, "Received ${parameterVisibilityList.size} parameters for station $stationNumber from direct API")

        // Convert ParameterVisibilityInfo to ParameterInfo
        val parameterInfoList = parameterVisibilityList.map { visParam ->
            com.shestikpetr.meteo.network.ParameterInfo(
                code = visParam.code,
                name = visParam.name,
                unit = visParam.unit ?: "",
                description = visParam.description ?: "",
                category = visParam.category ?: ""
            )
        }

        return createParameterConfigSetFromParameterInfo(parameterInfoList)
    }

    /**
     * Loads global parameter configuration from API.
     */
    private suspend fun loadGlobalParameterConfigFromApi(locale: String): ParameterConfigSet {
        Log.d(TAG, "Loading global parameter config (locale: $locale) - using fallback implementation")

        // Since getParameterConfigs and getParametersMetadata don't exist in the API,
        // provide a fallback with common meteorological parameters
        return createFallbackParameterConfig()
    }

    /**
     * Loads parameter configuration for a specific parameter from API.
     */
    private suspend fun loadParameterConfigFromApi(parameterCode: String, locale: String): ParameterConfig? {
        Log.d(TAG, "Loading parameter config for $parameterCode (locale: $locale) - using fallback implementation")

        // Since getParameterConfig doesn't exist in the API, use fallback configuration
        return createFallbackParameterConfig().parameters.find { it.code == parameterCode }
    }

    /**
     * Creates ParameterConfigSet from API ParameterInfo list.
     */
    private fun createParameterConfigSetFromParameterInfo(parameterInfoList: List<com.shestikpetr.meteo.network.ParameterInfo>): ParameterConfigSet {
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
     */
    private fun isDefaultParameter(parameterInfo: com.shestikpetr.meteo.network.ParameterInfo): Boolean {
        val name = parameterInfo.name.lowercase()
        val code = parameterInfo.code.lowercase()

        // Temperature is preferred default
        return name.contains("температур") ||
               code == "t" ||
               code == "4402"
    }

    /**
     * Creates a fallback parameter configuration with common meteorological parameters.
     */
    private fun createFallbackParameterConfig(): ParameterConfigSet {
        val parameters = listOf(
            ParameterConfig(
                code = "4402",
                name = "Температура воздуха",
                unit = "°C",
                description = "Температура воздуха в градусах Цельсия",
                category = "Meteorological",
                displayOrder = 1,
                isDefault = true
            ),
            ParameterConfig(
                code = "4406",
                name = "Относительная влажность",
                unit = "%",
                description = "Относительная влажность воздуха в процентах",
                category = "Meteorological",
                displayOrder = 2,
                isDefault = false
            ),
            ParameterConfig(
                code = "4401",
                name = "Атмосферное давление",
                unit = "гПа",
                description = "Атмосферное давление в гектопаскалях",
                category = "Meteorological",
                displayOrder = 3,
                isDefault = false
            ),
            ParameterConfig(
                code = "wind_speed",
                name = "Скорость ветра",
                unit = "м/с",
                description = "Скорость ветра в метрах в секунду",
                category = "Wind",
                displayOrder = 4,
                isDefault = false
            ),
            ParameterConfig(
                code = "wind_direction",
                name = "Направление ветра",
                unit = "°",
                description = "Направление ветра в градусах",
                category = "Wind",
                displayOrder = 5,
                isDefault = false
            )
        )

        return ParameterConfigSet(parameters, "4402")
    }

    /**
     * Cached parameter configuration set with timestamp.
     */
    private data class CachedParameterConfigSet(
        val configSet: ParameterConfigSet,
        val timestamp: Long
    )

    /**
     * Cached parameter configuration with timestamp.
     */
    private data class CachedParameterConfig(
        val config: ParameterConfig,
        val timestamp: Long
    )
}