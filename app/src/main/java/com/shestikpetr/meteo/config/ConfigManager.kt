package com.shestikpetr.meteo.config

import com.shestikpetr.meteo.common.logging.MeteoLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Configuration manager that handles runtime configuration loading and updates.
 *
 * This class provides a centralized way to manage application configuration,
 * including loading from remote sources with fallback mechanisms.
 * It follows SOLID principles:
 * - Single Responsibility: Manages configuration state and loading
 * - Dependency Inversion: Depends on abstractions (ConfigRepository)
 * - Open/Closed: Extensible for different configuration strategies
 */
@Singleton
class ConfigManager @Inject constructor(
    private val configRepository: ConfigRepository,
    @DefaultConfig private val defaultConfig: AppConfig,
    @EmergencyConfig private val emergencyConfig: AppConfig
) {
    private val logger = MeteoLogger.forClass(ConfigManager::class)

    private val _currentConfig = MutableStateFlow(defaultConfig)
    val currentConfig: StateFlow<AppConfig> = _currentConfig.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private var hasInitialized = false

    /**
     * Initializes the configuration manager by loading configuration.
     * This should be called early in the application lifecycle.
     */
    suspend fun initialize() {
        if (hasInitialized) {
            logger.d("ConfigManager already initialized")
            return
        }

        logger.d("Initializing ConfigManager")
        loadConfiguration()
        hasInitialized = true
    }

    /**
     * Loads configuration from the repository with fallback handling.
     */
    suspend fun loadConfiguration() {
        if (_isLoading.value) {
            logger.d("Configuration loading already in progress")
            return
        }

        _isLoading.value = true
        _lastError.value = null

        try {
            logger.d("Loading configuration...")

            // Try to load from repository with fallback
            val config = configRepository.loadConfigWithFallback(defaultConfig)
            _currentConfig.value = config

            logger.d("Configuration loaded successfully")
            logger.d("Base URL: ${config.network.baseUrl}")
            logger.d("Environment: ${config.environment}")

        } catch (e: Exception) {
            logger.e("Critical error loading configuration, using emergency config", e)
            _currentConfig.value = emergencyConfig
            _lastError.value = "Failed to load configuration: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Forces a refresh of the configuration from remote sources.
     */
    suspend fun refreshConfiguration() {
        logger.d("Refreshing configuration...")
        _isLoading.value = true
        _lastError.value = null

        try {
            val result = configRepository.refreshConfig()
            val config = result.getOrElse { error ->
                logger.w("Failed to refresh config, keeping current: ${error.message}")
                _lastError.value = "Refresh failed: ${error.message}"
                _currentConfig.value
            }
            _currentConfig.value = config

        } catch (e: Exception) {
            logger.e("Error during configuration refresh", e)
            _lastError.value = "Refresh error: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Checks if remote configuration is available.
     */
    suspend fun isRemoteConfigAvailable(): Boolean {
        return try {
            configRepository.isRemoteConfigAvailable()
        } catch (e: Exception) {
            logger.e("Error checking remote config availability", e)
            false
        }
    }

    /**
     * Gets the current configuration value.
     */
    fun getCurrentConfig(): AppConfig = _currentConfig.value

    /**
     * Gets specific network configuration.
     */
    fun getNetworkConfig(): NetworkConfig = _currentConfig.value.network

    /**
     * Gets specific map configuration.
     */
    fun getMapConfig(): MapConfig = _currentConfig.value.map

    /**
     * Gets specific security configuration.
     */
    fun getSecurityConfig(): SecurityConfig = _currentConfig.value.security

    /**
     * Updates configuration manually (for testing or emergency override).
     */
    fun updateConfig(config: AppConfig) {
        logger.d("Manually updating configuration")
        _currentConfig.value = config
    }

    /**
     * Resets configuration to default values.
     */
    fun resetToDefault() {
        logger.d("Resetting configuration to default")
        _currentConfig.value = defaultConfig
        _lastError.value = null
    }

    /**
     * Gets configuration status information.
     */
    fun getStatus(): ConfigStatus {
        return ConfigStatus(
            isInitialized = hasInitialized,
            isLoading = _isLoading.value,
            currentEnvironment = _currentConfig.value.environment,
            lastError = _lastError.value,
            baseUrl = _currentConfig.value.network.baseUrl
        )
    }
}

/**
 * Data class representing configuration manager status.
 */
data class ConfigStatus(
    val isInitialized: Boolean,
    val isLoading: Boolean,
    val currentEnvironment: Environment,
    val lastError: String?,
    val baseUrl: String
)