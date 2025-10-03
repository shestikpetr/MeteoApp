package com.shestikpetr.meteo.config

/**
 * Main application configuration interface.
 *
 * This interface defines all configuration values needed by the application.
 * Following Dependency Inversion Principle, all components depend on this
 * abstraction rather than concrete configuration implementations.
 */
interface AppConfig {
    /**
     * Network configuration settings
     */
    val network: NetworkConfig

    /**
     * Map configuration settings
     */
    val map: MapConfig

    /**
     * Security configuration settings
     */
    val security: SecurityConfig

    /**
     * Application environment (debug, release, etc.)
     */
    val environment: Environment
}

/**
 * Network-related configuration interface.
 *
 * Separated following Interface Segregation Principle - clients only
 * depend on the network configuration they need.
 */
interface NetworkConfig {
    /**
     * Base URL for the API
     */
    val baseUrl: String

    /**
     * Connection timeout in seconds
     */
    val connectTimeoutSeconds: Long

    /**
     * Read timeout in seconds
     */
    val readTimeoutSeconds: Long

    /**
     * Write timeout in seconds
     */
    val writeTimeoutSeconds: Long

    /**
     * Enable retry on connection failure
     */
    val retryOnConnectionFailure: Boolean

    /**
     * Maximum number of retry attempts
     */
    val maxRetryAttempts: Int
}

/**
 * Map-related configuration interface.
 *
 * Separated following Interface Segregation Principle.
 */
interface MapConfig {
    /**
     * Yandex MapKit API key
     */
    val apiKey: String

    /**
     * Default map zoom level
     */
    val defaultZoomLevel: Float

    /**
     * Enable map clustering
     */
    val enableClustering: Boolean
}

/**
 * Security-related configuration interface.
 *
 * Separated following Interface Segregation Principle.
 */
interface SecurityConfig {
    /**
     * Enable certificate pinning
     */
    val enableCertificatePinning: Boolean

    /**
     * Certificate pinning hosts
     */
    val pinnedHosts: List<String>

    /**
     * Enable request/response logging
     */
    val enableNetworkLogging: Boolean

    /**
     * JWT token expiration buffer in minutes
     */
    val tokenExpirationBufferMinutes: Long
}

/**
 * Application environment enumeration
 */
enum class Environment {
    DEBUG,
    STAGING,
    RELEASE
}