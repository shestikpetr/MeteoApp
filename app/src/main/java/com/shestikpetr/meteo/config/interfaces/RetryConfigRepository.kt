package com.shestikpetr.meteo.config.interfaces

/**
 * Repository interface for retry policy configuration following Interface Segregation Principle.
 * Provides dynamic retry configurations for different operation types.
 */
interface RetryConfigRepository {

    /**
     * Data class representing retry policy configuration
     */
    data class RetryConfiguration(
        val maxAttempts: Int,
        val baseDelayMs: Long,
        val maxDelayMs: Long,
        val useExponentialBackoff: Boolean,
        val retryOnHttpErrors: Boolean,
        val retryOnNetworkErrors: Boolean,
        val retryOnJsonErrors: Boolean,
        val backoffMultiplier: Double = 2.0
    )

    /**
     * Enum representing different operation types that may need different retry configs
     */
    enum class OperationType {
        SENSOR_DATA,
        STATION_DATA,
        AUTHENTICATION,
        CONFIGURATION,
        PARAMETER_METADATA
    }

    /**
     * Gets retry configuration for sensor data operations
     */
    suspend fun getSensorDataRetryConfig(): Result<RetryConfiguration>

    /**
     * Gets retry configuration for station data operations
     */
    suspend fun getStationDataRetryConfig(): Result<RetryConfiguration>

    /**
     * Gets retry configuration for authentication operations
     */
    suspend fun getAuthRetryConfig(): Result<RetryConfiguration>

    /**
     * Gets retry configuration for configuration loading operations
     */
    suspend fun getConfigRetryConfig(): Result<RetryConfiguration>

    /**
     * Gets retry configuration for parameter metadata operations
     */
    suspend fun getParameterMetadataRetryConfig(): Result<RetryConfiguration>

    /**
     * Gets retry configuration for a specific operation type
     */
    suspend fun getRetryConfig(operationType: OperationType): Result<RetryConfiguration>

    /**
     * Gets the default fallback retry configuration
     */
    fun getDefaultRetryConfig(): RetryConfiguration

    /**
     * Forces refresh of retry configuration from remote source
     */
    suspend fun refreshConfiguration(): Result<Unit>
}