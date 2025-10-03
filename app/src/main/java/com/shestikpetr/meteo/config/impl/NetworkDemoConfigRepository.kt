package com.shestikpetr.meteo.config.impl

import android.util.Log
import com.shestikpetr.meteo.config.cache.ConfigurationCache
import com.shestikpetr.meteo.config.data.DemoConfigResponse
import com.shestikpetr.meteo.config.interfaces.DemoConfigRepository
import com.shestikpetr.meteo.config.network.ConfigurationApiService
import kotlinx.coroutines.delay
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Network-based implementation of DemoConfigRepository with caching and fallback support.
 * Follows SOLID principles with proper dependency injection and error handling.
 * Uses simple retry logic to avoid circular dependency.
 */
@Singleton
class NetworkDemoConfigRepository @Inject constructor(
    private val configApiService: ConfigurationApiService,
    private val cache: ConfigurationCache
) : DemoConfigRepository {

    /**
     * Gets demo account credentials
     */
    override suspend fun getDemoCredentials(): Result<DemoConfigRepository.DemoCredentials> {
        return getDemoConfig().mapCatching { config ->
            DemoConfigRepository.DemoCredentials(
                username = config.demoCredentials.username,
                password = config.demoCredentials.password,
                enabled = config.demoCredentials.enabled,
                description = config.demoCredentials.description
            )
        }
    }

    /**
     * Gets development features configuration
     */
    override suspend fun getDevelopmentFeatures(): Result<DemoConfigRepository.DevelopmentFeatures> {
        return getDemoConfig().mapCatching { config ->
            DemoConfigRepository.DevelopmentFeatures(
                enableDebugLogging = config.developmentFeatures.enableDebugLogging,
                enableNetworkLogs = config.developmentFeatures.enableNetworkLogs,
                enablePerformanceMetrics = config.developmentFeatures.enablePerformanceMetrics,
                mockDataEnabled = config.developmentFeatures.mockDataEnabled,
                showDeveloperOptions = config.developmentFeatures.showDeveloperOptions
            )
        }
    }

    /**
     * Gets complete demo configuration
     */
    override suspend fun getDemoConfiguration(): Result<DemoConfigRepository.DemoConfiguration> {
        return getDemoConfig().mapCatching { config ->
            DemoConfigRepository.DemoConfiguration(
                demoCredentials = DemoConfigRepository.DemoCredentials(
                    username = config.demoCredentials.username,
                    password = config.demoCredentials.password,
                    enabled = config.demoCredentials.enabled,
                    description = config.demoCredentials.description
                ),
                developmentFeatures = DemoConfigRepository.DevelopmentFeatures(
                    enableDebugLogging = config.developmentFeatures.enableDebugLogging,
                    enableNetworkLogs = config.developmentFeatures.enableNetworkLogs,
                    enablePerformanceMetrics = config.developmentFeatures.enablePerformanceMetrics,
                    mockDataEnabled = config.developmentFeatures.mockDataEnabled,
                    showDeveloperOptions = config.developmentFeatures.showDeveloperOptions
                ),
                demoDataEnabled = config.demoDataEnabled,
                environment = config.environment
            )
        }
    }

    /**
     * Checks if demo mode is enabled
     */
    override suspend fun isDemoModeEnabled(): Boolean {
        return getDemoCredentials().fold(
            onSuccess = { it.enabled },
            onFailure = {
                Log.w("DemoConfig", "Failed to get demo config, using fallback", it)
                getDefaultDemoCredentials().enabled
            }
        )
    }

    /**
     * Checks if development features are enabled
     */
    override suspend fun areDevelopmentFeaturesEnabled(): Boolean {
        return getDevelopmentFeatures().fold(
            onSuccess = { features ->
                features.enableDebugLogging ||
                features.enableNetworkLogs ||
                features.enablePerformanceMetrics ||
                features.mockDataEnabled ||
                features.showDeveloperOptions
            },
            onFailure = {
                Log.w("DemoConfig", "Failed to get development features, using fallback", it)
                false
            }
        )
    }

    /**
     * Gets the current environment (development/staging/production)
     */
    override suspend fun getCurrentEnvironment(): String {
        return getDemoConfig().fold(
            onSuccess = { it.environment },
            onFailure = {
                Log.w("DemoConfig", "Failed to get environment, using fallback", it)
                "production" // Safe default
            }
        )
    }

    /**
     * Gets default demo credentials for fallback
     */
    override fun getDefaultDemoCredentials(): DemoConfigRepository.DemoCredentials {
        return DemoConfigRepository.DemoCredentials(
            username = "user", // Current hardcoded value
            password = "user", // Current hardcoded value
            enabled = true,
            description = "Default demo account for testing"
        )
    }

    /**
     * Forces refresh of demo configuration from remote source
     */
    override suspend fun refreshConfiguration(): Result<Unit> {
        return try {
            cache.invalidate(ConfigurationCache.DEMO_CONFIG_KEY)
            getDemoConfig()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("DemoConfig", "Failed to refresh configuration", e)
            Result.failure(e)
        }
    }

    /**
     * Gets demo configuration with caching and retry logic
     */
    private suspend fun getDemoConfig(): Result<DemoConfigResponse> {
        // Try cache first
        cache.get<DemoConfigResponse>(ConfigurationCache.DEMO_CONFIG_KEY)?.let { cached ->
            Log.d("DemoConfig", "Using cached demo config")
            return Result.success(cached)
        }

        // Server config endpoints don't exist yet, use defaults immediately
        Log.d("DemoConfig", "Using default demo config (server endpoints not available)")
        val defaultConfig = getDefaultDemoConfigResponse()

        // Cache the default config for consistency
        cache.put(ConfigurationCache.DEMO_CONFIG_KEY, defaultConfig)

        return Result.success(defaultConfig)
    }

    /**
     * Provides default demo configuration response as fallback
     */
    private fun getDefaultDemoConfigResponse(): DemoConfigResponse {
        val defaultCredentials = getDefaultDemoCredentials()

        return DemoConfigResponse(
            demoCredentials = com.shestikpetr.meteo.config.data.DemoCredentialsDto(
                username = defaultCredentials.username,
                password = defaultCredentials.password,
                enabled = defaultCredentials.enabled,
                description = defaultCredentials.description
            ),
            developmentFeatures = com.shestikpetr.meteo.config.data.DevelopmentFeaturesDto(
                enableDebugLogging = true,
                enableNetworkLogs = false,
                enablePerformanceMetrics = false,
                mockDataEnabled = false,
                showDeveloperOptions = true
            ),
            demoDataEnabled = true,
            environment = "production"
        )
    }
}