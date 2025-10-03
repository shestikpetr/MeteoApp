package com.shestikpetr.meteo.config.impl

import com.shestikpetr.meteo.BuildConfig
import com.shestikpetr.meteo.common.constants.MeteoConstants
import com.shestikpetr.meteo.config.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default configuration provider with fallback values.
 *
 * This class provides sensible default configuration values that can be used
 * as fallbacks when remote configuration is unavailable. It follows SOLID principles:
 * - Single Responsibility: Only provides default configuration values
 * - Open/Closed: Can be extended for different environments
 * - Dependency Inversion: Implements AppConfig interface
 */
@Singleton
class DefaultConfigProvider @Inject constructor() {

    /**
     * Provides default configuration for debug environment.
     */
    fun getDebugConfig(): AppConfig = DefaultAppConfig(
        network = DefaultNetworkConfig(
            baseUrl = BuildConfig.DEFAULT_BASE_URL,
            connectTimeoutSeconds = MeteoConstants.Network.DEFAULT_CONNECT_TIMEOUT_SECONDS,
            readTimeoutSeconds = MeteoConstants.Network.DEFAULT_READ_TIMEOUT_SECONDS,
            writeTimeoutSeconds = MeteoConstants.Network.DEFAULT_WRITE_TIMEOUT_SECONDS,
            retryOnConnectionFailure = true,
            maxRetryAttempts = MeteoConstants.Network.DEFAULT_MAX_RETRY_ATTEMPTS
        ),
        map = DefaultMapConfig(
            apiKey = BuildConfig.YANDEX_MAPKIT_API_KEY,
            defaultZoomLevel = MeteoConstants.UI.DEFAULT_MAP_ZOOM,
            enableClustering = true
        ),
        security = DefaultSecurityConfig(
            enableCertificatePinning = false, // Disabled in debug for development
            pinnedHosts = emptyList(),
            enableNetworkLogging = BuildConfig.ENABLE_NETWORK_LOGGING, // From BuildConfig
            tokenExpirationBufferMinutes = MeteoConstants.Auth.TOKEN_EXPIRATION_BUFFER_MINUTES
        ),
        environment = Environment.DEBUG
    )

    /**
     * Provides default configuration for release environment.
     */
    fun getReleaseConfig(): AppConfig = DefaultAppConfig(
        network = DefaultNetworkConfig(
            baseUrl = BuildConfig.DEFAULT_BASE_URL,
            connectTimeoutSeconds = MeteoConstants.Network.DEFAULT_CONNECT_TIMEOUT_SECONDS,
            readTimeoutSeconds = MeteoConstants.Network.DEFAULT_READ_TIMEOUT_SECONDS,
            writeTimeoutSeconds = MeteoConstants.Network.DEFAULT_WRITE_TIMEOUT_SECONDS,
            retryOnConnectionFailure = true,
            maxRetryAttempts = MeteoConstants.Network.DEFAULT_MAX_RETRY_ATTEMPTS
        ),
        map = DefaultMapConfig(
            apiKey = BuildConfig.YANDEX_MAPKIT_API_KEY,
            defaultZoomLevel = MeteoConstants.UI.DEFAULT_MAP_ZOOM,
            enableClustering = true
        ),
        security = DefaultSecurityConfig(
            enableCertificatePinning = true, // Enabled in release for security
            pinnedHosts = listOf(BuildConfig.API_HOST), // From BuildConfig instead of hardcoded
            enableNetworkLogging = BuildConfig.ENABLE_NETWORK_LOGGING, // From BuildConfig
            tokenExpirationBufferMinutes = MeteoConstants.Auth.TOKEN_EXPIRATION_BUFFER_MINUTES
        ),
        environment = Environment.RELEASE
    )

    /**
     * Provides configuration based on current build variant.
     */
    fun getDefaultConfig(): AppConfig {
        return if (BuildConfig.DEBUG) {
            getDebugConfig()
        } else {
            getReleaseConfig()
        }
    }

    /**
     * Provides minimal safe configuration for emergency fallback.
     */
    fun getEmergencyConfig(): AppConfig = DefaultAppConfig(
        network = DefaultNetworkConfig(
            baseUrl = BuildConfig.DEFAULT_BASE_URL,
            connectTimeoutSeconds = MeteoConstants.Network.EMERGENCY_TIMEOUT_SECONDS,
            readTimeoutSeconds = MeteoConstants.Network.EMERGENCY_TIMEOUT_SECONDS,
            writeTimeoutSeconds = MeteoConstants.Network.EMERGENCY_TIMEOUT_SECONDS,
            retryOnConnectionFailure = false, // No retries in emergency
            maxRetryAttempts = 1
        ),
        map = DefaultMapConfig(
            apiKey = BuildConfig.YANDEX_MAPKIT_API_KEY, // From BuildConfig instead of hardcoded
            defaultZoomLevel = MeteoConstants.Map.EMERGENCY_MAP_ZOOM,
            enableClustering = false // Disabled for better performance
        ),
        security = DefaultSecurityConfig(
            enableCertificatePinning = false,
            pinnedHosts = emptyList(),
            enableNetworkLogging = false,
            tokenExpirationBufferMinutes = MeteoConstants.Auth.EMERGENCY_TOKEN_BUFFER_MINUTES
        ),
        environment = Environment.DEBUG
    )
}

/**
 * Default implementation of AppConfig.
 */
data class DefaultAppConfig(
    override val network: NetworkConfig,
    override val map: MapConfig,
    override val security: SecurityConfig,
    override val environment: Environment
) : AppConfig

/**
 * Default implementation of NetworkConfig.
 */
data class DefaultNetworkConfig(
    override val baseUrl: String,
    override val connectTimeoutSeconds: Long,
    override val readTimeoutSeconds: Long,
    override val writeTimeoutSeconds: Long,
    override val retryOnConnectionFailure: Boolean,
    override val maxRetryAttempts: Int
) : NetworkConfig

/**
 * Default implementation of MapConfig.
 */
data class DefaultMapConfig(
    override val apiKey: String,
    override val defaultZoomLevel: Float,
    override val enableClustering: Boolean
) : MapConfig

/**
 * Default implementation of SecurityConfig.
 */
data class DefaultSecurityConfig(
    override val enableCertificatePinning: Boolean,
    override val pinnedHosts: List<String>,
    override val enableNetworkLogging: Boolean,
    override val tokenExpirationBufferMinutes: Long
) : SecurityConfig