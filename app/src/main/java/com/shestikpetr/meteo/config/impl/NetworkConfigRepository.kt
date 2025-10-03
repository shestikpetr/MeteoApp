package com.shestikpetr.meteo.config.impl

import android.util.Log
import com.shestikpetr.meteo.config.*
import com.shestikpetr.meteo.network.interfaces.HttpClient
import com.shestikpetr.meteo.network.interfaces.SecureStorage
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException

/**
 * Network-based implementation of ConfigRepository.
 *
 * This class loads configuration from a remote API endpoint while providing
 * fallback mechanisms. It follows SOLID principles:
 * - Single Responsibility: Only handles config loading from network
 * - Dependency Inversion: Depends on abstractions (HttpClient, SecureStorage)
 * - Open/Closed: Extensible for different config sources
 */
@Singleton
class NetworkConfigRepository @Inject constructor(
    private val httpClient: HttpClient,
    private val secureStorage: SecureStorage,
    private val gson: Gson
) : ConfigRepository {

    companion object {
        private const val TAG = "NetworkConfigRepository"
        private const val CONFIG_ENDPOINT = "/config/client"
        private const val CACHE_KEY = "app_config_cache"
        private const val CACHE_TIMESTAMP_KEY = "app_config_cache_timestamp"
        private const val CACHE_VALIDITY_HOURS = 24
        private const val NETWORK_TIMEOUT_MS = 10_000L
    }

    private var cachedConfig: AppConfig? = null
    private var lastLoadTime: Long = 0

    override suspend fun loadConfig(): Result<AppConfig> {
        return try {
            Log.d(TAG, "Loading configuration from network")

            // Check if we have a valid cached config
            val cached = getCachedConfig()
            if (cached != null && isCacheValid()) {
                Log.d(TAG, "Using cached configuration")
                return Result.success(cached)
            }

            // Try to load from network
            val networkConfig = loadFromNetwork()
            if (networkConfig != null) {
                Log.d(TAG, "Successfully loaded configuration from network")
                cacheConfig(networkConfig)
                cachedConfig = networkConfig
                lastLoadTime = System.currentTimeMillis()
                Result.success(networkConfig)
            } else {
                // If network fails, try to use cached config even if expired
                val expiredCache = getCachedConfig()
                if (expiredCache != null) {
                    Log.w(TAG, "Network failed, using expired cached configuration")
                    Result.success(expiredCache)
                } else {
                    Result.failure(ConfigLoadException("Failed to load configuration from network and no cache available"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading configuration", e)
            Result.failure(e)
        }
    }

    override suspend fun loadConfigWithFallback(fallbackConfig: AppConfig): AppConfig {
        return loadConfig().getOrElse { error ->
            Log.w(TAG, "Failed to load config, using fallback: ${error.message}")
            fallbackConfig
        }
    }

    override suspend fun isRemoteConfigAvailable(): Boolean {
        return try {
            withTimeout(NETWORK_TIMEOUT_MS) {
                val request = httpClient.newRequestBuilder()
                    .url("$CONFIG_ENDPOINT/health")
                    .get()
                    .build()
                val response = httpClient.executeRequest(request)
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.w(TAG, "Remote config health check failed: ${e.message}")
            false
        }
    }

    override suspend fun refreshConfig(): Result<AppConfig> {
        Log.d(TAG, "Forcing configuration refresh")
        cachedConfig = null
        lastLoadTime = 0
        clearCache()
        return loadConfig()
    }

    private suspend fun loadFromNetwork(): AppConfig? {
        return try {
            withTimeout(NETWORK_TIMEOUT_MS) {
                Log.d(TAG, "Fetching config from $CONFIG_ENDPOINT")
                val request = httpClient.newRequestBuilder()
                    .url(CONFIG_ENDPOINT)
                    .get()
                    .build()
                val response = httpClient.executeRequest(request)

                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                        Log.d(TAG, "Received config response: ${responseBody.take(200)}...")
                        parseRemoteConfig(responseBody)
                    } else {
                        Log.e(TAG, "Empty response body")
                        null
                    }
                } else {
                    Log.e(TAG, "Network request failed: ${response.code} ${response.message}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network request exception: ${e.message}", e)
            null
        }
    }

    private fun parseRemoteConfig(responseBody: String): AppConfig? {
        return try {
            val remoteResponse = gson.fromJson(responseBody, RemoteConfigResponse::class.java)

            if (remoteResponse.success && remoteResponse.data != null) {
                Log.d(TAG, "Successfully parsed remote configuration")
                mapRemoteConfigToAppConfig(remoteResponse.data)
            } else {
                Log.e(TAG, "Remote config response indicates failure or missing data")
                null
            }
        } catch (e: JsonSyntaxException) {
            Log.e(TAG, "Failed to parse remote config JSON: ${e.message}")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error parsing remote config: ${e.message}")
            null
        }
    }

    private fun mapRemoteConfigToAppConfig(remoteData: RemoteConfigData): AppConfig {
        return DefaultAppConfig(
            network = DefaultNetworkConfig(
                baseUrl = remoteData.network.baseUrl,
                connectTimeoutSeconds = remoteData.network.timeouts?.connectSeconds ?: 30L,
                readTimeoutSeconds = remoteData.network.timeouts?.readSeconds ?: 30L,
                writeTimeoutSeconds = remoteData.network.timeouts?.writeSeconds ?: 30L,
                retryOnConnectionFailure = remoteData.network.retry?.enabled ?: true,
                maxRetryAttempts = remoteData.network.retry?.maxAttempts ?: 3
            ),
            map = DefaultMapConfig(
                apiKey = "", // API key should come from secure storage or BuildConfig
                defaultZoomLevel = remoteData.map?.defaultZoom ?: 10.0f,
                enableClustering = remoteData.map?.enableClustering ?: true
            ),
            security = DefaultSecurityConfig(
                enableCertificatePinning = remoteData.security?.certificatePinning?.enabled ?: false,
                pinnedHosts = remoteData.security?.certificatePinning?.hosts ?: emptyList(),
                enableNetworkLogging = remoteData.security?.logging?.enabled ?: false,
                tokenExpirationBufferMinutes = remoteData.security?.tokenBufferMinutes ?: 5L
            ),
            environment = Environment.RELEASE // This should be determined by build variant
        )
    }

    private suspend fun getCachedConfig(): AppConfig? {
        if (cachedConfig != null) {
            return cachedConfig
        }

        // Try to load from persistent storage
        return try {
            val cachedJson = secureStorage.getString(CACHE_KEY)
            if (cachedJson != null) {
                Log.d(TAG, "Found cached configuration in storage")
                gson.fromJson(cachedJson, DefaultAppConfig::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading cached config: ${e.message}")
            null
        }
    }

    private suspend fun isCacheValid(): Boolean {
        val lastCacheTime = secureStorage.getLong(CACHE_TIMESTAMP_KEY, 0L)
        val currentTime = System.currentTimeMillis()
        val cacheAgeHours = (currentTime - lastCacheTime) / (1000 * 60 * 60)

        val isValid = cacheAgeHours < CACHE_VALIDITY_HOURS
        Log.d(TAG, "Cache age: ${cacheAgeHours}h, valid: $isValid")
        return isValid
    }

    private suspend fun cacheConfig(config: AppConfig) {
        try {
            val configJson = gson.toJson(config)
            secureStorage.putString(CACHE_KEY, configJson)
            secureStorage.putLong(CACHE_TIMESTAMP_KEY, System.currentTimeMillis())
            Log.d(TAG, "Configuration cached successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error caching configuration: ${e.message}")
        }
    }

    private suspend fun clearCache() {
        try {
            secureStorage.remove(CACHE_KEY)
            secureStorage.remove(CACHE_TIMESTAMP_KEY)
            Log.d(TAG, "Configuration cache cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing cache: ${e.message}")
        }
    }
}

/**
 * Exception thrown when configuration loading fails.
 */
class ConfigLoadException(message: String, cause: Throwable? = null) : Exception(message, cause)