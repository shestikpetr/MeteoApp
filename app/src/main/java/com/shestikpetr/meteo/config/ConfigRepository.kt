package com.shestikpetr.meteo.config

/**
 * Repository interface for loading configuration data.
 *
 * This interface follows the Repository pattern and Dependency Inversion Principle,
 * allowing for different sources of configuration (network, local storage, etc.)
 */
interface ConfigRepository {
    /**
     * Loads the application configuration.
     *
     * @return Result containing AppConfig on success or error information on failure
     */
    suspend fun loadConfig(): Result<AppConfig>

    /**
     * Loads configuration with a fallback strategy.
     *
     * @param fallbackConfig The fallback configuration to use if loading fails
     * @return AppConfig - either loaded from source or fallback
     */
    suspend fun loadConfigWithFallback(fallbackConfig: AppConfig): AppConfig

    /**
     * Checks if remote configuration is available.
     *
     * @return true if remote config can be loaded, false otherwise
     */
    suspend fun isRemoteConfigAvailable(): Boolean

    /**
     * Forces a refresh of the configuration cache.
     */
    suspend fun refreshConfig(): Result<AppConfig>
}

/**
 * Data class for remote configuration response.
 *
 * This represents the structure expected from the server's
 * /api/v1/config/client endpoint.
 */
data class RemoteConfigResponse(
    val success: Boolean,
    val data: RemoteConfigData?
)

/**
 * Remote configuration data structure.
 */
data class RemoteConfigData(
    val network: RemoteNetworkConfig,
    val map: RemoteMapConfig?,
    val security: RemoteSecurityConfig?
)

/**
 * Remote network configuration structure.
 */
data class RemoteNetworkConfig(
    val baseUrl: String,
    val timeouts: RemoteTimeoutConfig?,
    val retry: RemoteRetryConfig?
)

/**
 * Remote timeout configuration structure.
 */
data class RemoteTimeoutConfig(
    val connectSeconds: Long?,
    val readSeconds: Long?,
    val writeSeconds: Long?
)

/**
 * Remote retry configuration structure.
 */
data class RemoteRetryConfig(
    val enabled: Boolean?,
    val maxAttempts: Int?
)

/**
 * Remote map configuration structure.
 */
data class RemoteMapConfig(
    val defaultZoom: Float?,
    val enableClustering: Boolean?
)

/**
 * Remote security configuration structure.
 */
data class RemoteSecurityConfig(
    val certificatePinning: RemoteCertificatePinningConfig?,
    val logging: RemoteLoggingConfig?,
    val tokenBufferMinutes: Long?
)

/**
 * Remote certificate pinning configuration structure.
 */
data class RemoteCertificatePinningConfig(
    val enabled: Boolean?,
    val hosts: List<String>?
)

/**
 * Remote logging configuration structure.
 */
data class RemoteLoggingConfig(
    val enabled: Boolean?
)