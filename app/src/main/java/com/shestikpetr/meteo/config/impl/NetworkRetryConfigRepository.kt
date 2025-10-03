package com.shestikpetr.meteo.config.impl

import android.util.Log
import com.shestikpetr.meteo.config.cache.ConfigurationCache
import com.shestikpetr.meteo.config.data.RetryConfigResponse
import com.shestikpetr.meteo.config.interfaces.RetryConfigRepository
import com.shestikpetr.meteo.config.network.ConfigurationApiService
import kotlinx.coroutines.delay
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Network-based implementation of RetryConfigRepository with caching and fallback support.
 * Follows SOLID principles with proper dependency injection and error handling.
 * Uses simple retry logic to avoid circular dependency with RetryPolicy.
 */
@Singleton
class NetworkRetryConfigRepository @Inject constructor(
    private val configApiService: ConfigurationApiService,
    private val cache: ConfigurationCache
) : RetryConfigRepository {

    /**
     * Gets retry configuration for sensor data operations
     */
    override suspend fun getSensorDataRetryConfig(): Result<RetryConfigRepository.RetryConfiguration> {
        return getRetryConfig().mapCatching { config ->
            mapToRetryConfiguration(config.sensorData)
        }
    }

    /**
     * Gets retry configuration for station data operations
     */
    override suspend fun getStationDataRetryConfig(): Result<RetryConfigRepository.RetryConfiguration> {
        return getRetryConfig().mapCatching { config ->
            mapToRetryConfiguration(config.stationData)
        }
    }

    /**
     * Gets retry configuration for authentication operations
     */
    override suspend fun getAuthRetryConfig(): Result<RetryConfigRepository.RetryConfiguration> {
        return getRetryConfig().mapCatching { config ->
            mapToRetryConfiguration(config.authentication)
        }
    }

    /**
     * Gets retry configuration for configuration loading operations
     */
    override suspend fun getConfigRetryConfig(): Result<RetryConfigRepository.RetryConfiguration> {
        return getRetryConfig().mapCatching { config ->
            mapToRetryConfiguration(config.configuration)
        }
    }

    /**
     * Gets retry configuration for parameter metadata operations
     */
    override suspend fun getParameterMetadataRetryConfig(): Result<RetryConfigRepository.RetryConfiguration> {
        return getRetryConfig().mapCatching { config ->
            mapToRetryConfiguration(config.parameterMetadata)
        }
    }

    /**
     * Gets retry configuration for a specific operation type
     */
    override suspend fun getRetryConfig(operationType: RetryConfigRepository.OperationType): Result<RetryConfigRepository.RetryConfiguration> {
        return when (operationType) {
            RetryConfigRepository.OperationType.SENSOR_DATA -> getSensorDataRetryConfig()
            RetryConfigRepository.OperationType.STATION_DATA -> getStationDataRetryConfig()
            RetryConfigRepository.OperationType.AUTHENTICATION -> getAuthRetryConfig()
            RetryConfigRepository.OperationType.CONFIGURATION -> getConfigRetryConfig()
            RetryConfigRepository.OperationType.PARAMETER_METADATA -> getParameterMetadataRetryConfig()
        }
    }

    /**
     * Gets the default fallback retry configuration
     */
    override fun getDefaultRetryConfig(): RetryConfigRepository.RetryConfiguration {
        return RetryConfigRepository.RetryConfiguration(
            maxAttempts = 3, // Current hardcoded value
            baseDelayMs = 1000L, // Current hardcoded value
            maxDelayMs = 5000L, // Current hardcoded value
            useExponentialBackoff = false,
            retryOnHttpErrors = true,
            retryOnNetworkErrors = true,
            retryOnJsonErrors = true,
            backoffMultiplier = 2.0
        )
    }

    /**
     * Forces refresh of retry configuration from remote source
     */
    override suspend fun refreshConfiguration(): Result<Unit> {
        return try {
            cache.invalidate(ConfigurationCache.RETRY_CONFIG_KEY)
            getRetryConfig()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("RetryConfig", "Failed to refresh configuration", e)
            Result.failure(e)
        }
    }

    /**
     * Gets retry configuration with caching and retry logic
     */
    private suspend fun getRetryConfig(): Result<RetryConfigResponse> {
        // Try cache first
        cache.get<RetryConfigResponse>(ConfigurationCache.RETRY_CONFIG_KEY)?.let { cached ->
            Log.d("RetryConfig", "Using cached retry config")
            return Result.success(cached)
        }

        // Server config endpoints don't exist yet, use defaults immediately
        Log.d("RetryConfig", "Using default retry config (server endpoints not available)")
        val defaultConfig = getDefaultRetryConfigResponse()

        // Cache the default config for consistency
        cache.put(ConfigurationCache.RETRY_CONFIG_KEY, defaultConfig)

        return Result.success(defaultConfig)
    }

    /**
     * Maps DTO to domain model
     */
    private fun mapToRetryConfiguration(dto: com.shestikpetr.meteo.config.data.RetryPolicyDto): RetryConfigRepository.RetryConfiguration {
        return RetryConfigRepository.RetryConfiguration(
            maxAttempts = dto.maxAttempts,
            baseDelayMs = dto.baseDelayMs,
            maxDelayMs = dto.maxDelayMs,
            useExponentialBackoff = dto.useExponentialBackoff,
            retryOnHttpErrors = dto.retryOnHttpErrors,
            retryOnNetworkErrors = dto.retryOnNetworkErrors,
            retryOnJsonErrors = dto.retryOnJsonErrors,
            backoffMultiplier = dto.backoffMultiplier
        )
    }


    /**
     * Provides default retry configuration response as fallback
     */
    private fun getDefaultRetryConfigResponse(): RetryConfigResponse {
        val defaultConfig = getDefaultRetryConfig()
        val defaultDto = com.shestikpetr.meteo.config.data.RetryPolicyDto(
            maxAttempts = defaultConfig.maxAttempts,
            baseDelayMs = defaultConfig.baseDelayMs,
            maxDelayMs = defaultConfig.maxDelayMs,
            useExponentialBackoff = defaultConfig.useExponentialBackoff,
            retryOnHttpErrors = defaultConfig.retryOnHttpErrors,
            retryOnNetworkErrors = defaultConfig.retryOnNetworkErrors,
            retryOnJsonErrors = defaultConfig.retryOnJsonErrors,
            backoffMultiplier = defaultConfig.backoffMultiplier
        )

        return RetryConfigResponse(
            sensorData = defaultDto.copy(
                maxAttempts = 3,
                baseDelayMs = 1000L,
                maxDelayMs = 3000L,
                useExponentialBackoff = false
            ),
            stationData = defaultDto.copy(
                maxAttempts = 2,
                baseDelayMs = 500L,
                maxDelayMs = 2000L,
                useExponentialBackoff = true,
                retryOnJsonErrors = false
            ),
            authentication = defaultDto.copy(
                maxAttempts = 2,
                baseDelayMs = 1000L,
                maxDelayMs = 2000L,
                useExponentialBackoff = false
            ),
            configuration = defaultDto.copy(
                maxAttempts = 2,
                baseDelayMs = 500L,
                maxDelayMs = 2000L,
                useExponentialBackoff = true,
                retryOnJsonErrors = false
            ),
            parameterMetadata = defaultDto.copy(
                maxAttempts = 3,
                baseDelayMs = 1000L,
                maxDelayMs = 3000L,
                useExponentialBackoff = false
            ),
            default = defaultDto
        )
    }
}