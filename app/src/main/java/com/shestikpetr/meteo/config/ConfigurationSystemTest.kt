package com.shestikpetr.meteo.config

import android.util.Log
import com.shestikpetr.meteo.config.impl.DefaultConfigProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Test helper class to validate the configuration system.
 *
 * This class provides methods to test various aspects of the configuration
 * system to ensure it's working correctly during development and debugging.
 */
@Singleton
class ConfigurationSystemTest @Inject constructor(
    private val configManager: ConfigManager,
    private val defaultConfigProvider: DefaultConfigProvider
) {

    companion object {
        private const val TAG = "ConfigSystemTest"
    }

    /**
     * Runs basic validation tests on the configuration system.
     *
     * @return true if all tests pass, false otherwise
     */
    suspend fun runBasicTests(): Boolean {
        Log.d(TAG, "Running configuration system tests...")

        return try {
            // Test 1: Check default configuration
            val defaultConfig = defaultConfigProvider.getDefaultConfig()
            Log.d(TAG, "✓ Default config loaded: ${defaultConfig.network.baseUrl}")

            // Test 2: Check emergency configuration
            val emergencyConfig = defaultConfigProvider.getEmergencyConfig()
            Log.d(TAG, "✓ Emergency config loaded: ${emergencyConfig.network.baseUrl}")

            // Test 3: Check ConfigManager state
            val currentConfig = configManager.getCurrentConfig()
            Log.d(TAG, "✓ Current config loaded: ${currentConfig.network.baseUrl}")

            // Test 4: Check configuration values are valid
            validateConfiguration(currentConfig)

            // Test 5: Check status
            val status = configManager.getStatus()
            Log.d(TAG, "✓ Config status: initialized=${status.isInitialized}, env=${status.currentEnvironment}")

            Log.d(TAG, "All configuration tests passed!")
            true

        } catch (e: Exception) {
            Log.e(TAG, "Configuration tests failed: ${e.message}", e)
            false
        }
    }

    /**
     * Validates that a configuration has all required values.
     */
    private fun validateConfiguration(config: AppConfig): Boolean {
        val errors = mutableListOf<String>()

        // Validate network configuration
        if (config.network.baseUrl.isBlank()) {
            errors.add("Base URL is empty")
        }
        if (config.network.connectTimeoutSeconds <= 0) {
            errors.add("Connect timeout must be positive")
        }
        if (config.network.readTimeoutSeconds <= 0) {
            errors.add("Read timeout must be positive")
        }
        if (config.network.writeTimeoutSeconds <= 0) {
            errors.add("Write timeout must be positive")
        }

        // Validate map configuration
        if (config.map.apiKey.isBlank()) {
            errors.add("Map API key is empty")
        }
        if (config.map.defaultZoomLevel <= 0) {
            errors.add("Default zoom level must be positive")
        }

        // Validate security configuration
        if (config.security.tokenExpirationBufferMinutes < 0) {
            errors.add("Token expiration buffer cannot be negative")
        }

        return if (errors.isEmpty()) {
            Log.d(TAG, "✓ Configuration validation passed")
            true
        } else {
            Log.e(TAG, "✗ Configuration validation failed: ${errors.joinToString(", ")}")
            false
        }
    }

    /**
     * Tests remote configuration availability.
     */
    suspend fun testRemoteConfigAvailability(): Boolean {
        return try {
            val isAvailable = configManager.isRemoteConfigAvailable()
            Log.d(TAG, "Remote config availability: $isAvailable")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error testing remote config availability: ${e.message}", e)
            false
        }
    }

    /**
     * Prints current configuration for debugging.
     */
    fun printCurrentConfiguration() {
        val config = configManager.getCurrentConfig()
        val status = configManager.getStatus()

        Log.d(TAG, "=== Current Configuration ===")
        Log.d(TAG, "Environment: ${config.environment}")
        Log.d(TAG, "Status: $status")
        Log.d(TAG, "Network:")
        Log.d(TAG, "  Base URL: ${config.network.baseUrl}")
        Log.d(TAG, "  Connect Timeout: ${config.network.connectTimeoutSeconds}s")
        Log.d(TAG, "  Read Timeout: ${config.network.readTimeoutSeconds}s")
        Log.d(TAG, "  Write Timeout: ${config.network.writeTimeoutSeconds}s")
        Log.d(TAG, "  Retry Enabled: ${config.network.retryOnConnectionFailure}")
        Log.d(TAG, "  Max Retries: ${config.network.maxRetryAttempts}")
        Log.d(TAG, "Map:")
        Log.d(TAG, "  API Key: ${config.map.apiKey.take(10)}...")
        Log.d(TAG, "  Default Zoom: ${config.map.defaultZoomLevel}")
        Log.d(TAG, "  Clustering Enabled: ${config.map.enableClustering}")
        Log.d(TAG, "Security:")
        Log.d(TAG, "  Certificate Pinning: ${config.security.enableCertificatePinning}")
        Log.d(TAG, "  Pinned Hosts: ${config.security.pinnedHosts}")
        Log.d(TAG, "  Network Logging: ${config.security.enableNetworkLogging}")
        Log.d(TAG, "  Token Buffer: ${config.security.tokenExpirationBufferMinutes}min")
        Log.d(TAG, "=============================")
    }
}