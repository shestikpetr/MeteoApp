package com.shestikpetr.meteo.config.interfaces

/**
 * Repository interface for demo/development configuration following Interface Segregation Principle.
 * Provides secure demo account settings and development features.
 */
interface DemoConfigRepository {

    /**
     * Data class representing demo account credentials
     */
    data class DemoCredentials(
        val username: String,
        val password: String,
        val enabled: Boolean,
        val description: String? = null
    )

    /**
     * Data class representing development features configuration
     */
    data class DevelopmentFeatures(
        val enableDebugLogging: Boolean,
        val enableNetworkLogs: Boolean,
        val enablePerformanceMetrics: Boolean,
        val mockDataEnabled: Boolean,
        val showDeveloperOptions: Boolean
    )

    /**
     * Data class representing demo configuration
     */
    data class DemoConfiguration(
        val demoCredentials: DemoCredentials,
        val developmentFeatures: DevelopmentFeatures,
        val demoDataEnabled: Boolean,
        val environment: String // "development", "staging", "production"
    )

    /**
     * Gets demo account credentials
     */
    suspend fun getDemoCredentials(): Result<DemoCredentials>

    /**
     * Gets development features configuration
     */
    suspend fun getDevelopmentFeatures(): Result<DevelopmentFeatures>

    /**
     * Gets complete demo configuration
     */
    suspend fun getDemoConfiguration(): Result<DemoConfiguration>

    /**
     * Checks if demo mode is enabled
     */
    suspend fun isDemoModeEnabled(): Boolean

    /**
     * Checks if development features are enabled
     */
    suspend fun areDevelopmentFeaturesEnabled(): Boolean

    /**
     * Gets the current environment (development/staging/production)
     */
    suspend fun getCurrentEnvironment(): String

    /**
     * Gets default demo credentials for fallback
     */
    fun getDefaultDemoCredentials(): DemoCredentials

    /**
     * Forces refresh of demo configuration from remote source
     */
    suspend fun refreshConfiguration(): Result<Unit>
}