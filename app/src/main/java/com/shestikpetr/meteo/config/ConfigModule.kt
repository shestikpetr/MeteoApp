package com.shestikpetr.meteo.config

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.shestikpetr.meteo.config.impl.DefaultConfigProvider
import com.shestikpetr.meteo.config.impl.NetworkConfigRepository
import com.shestikpetr.meteo.network.interfaces.HttpClient
import com.shestikpetr.meteo.network.interfaces.SecureStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Dependency injection module for configuration system.
 *
 * This module provides all necessary dependencies for the configuration system
 * following SOLID principles and clean architecture patterns.
 */
@Module
@InstallIn(SingletonComponent::class)
object ConfigModule {

    @Provides
    @Singleton
    fun providesGson(): Gson {
        return GsonBuilder()
            .setPrettyPrinting()
            .create()
    }

    @Provides
    @Singleton
    fun providesDefaultConfigProvider(): DefaultConfigProvider {
        return DefaultConfigProvider()
    }

    @Provides
    @Singleton
    fun providesConfigRepository(
        httpClient: HttpClient,
        secureStorage: SecureStorage,
        gson: Gson
    ): ConfigRepository {
        return NetworkConfigRepository(httpClient, secureStorage, gson)
    }

    @Provides
    @Singleton
    @DefaultConfig
    fun providesDefaultAppConfig(
        defaultConfigProvider: DefaultConfigProvider
    ): AppConfig {
        return defaultConfigProvider.getDefaultConfig()
    }

    @Provides
    @Singleton
    @EmergencyConfig
    fun providesEmergencyAppConfig(
        defaultConfigProvider: DefaultConfigProvider
    ): AppConfig {
        return defaultConfigProvider.getEmergencyConfig()
    }

    @Provides
    @Singleton
    fun providesConfigManager(
        configRepository: ConfigRepository,
        @DefaultConfig defaultConfig: AppConfig,
        @EmergencyConfig emergencyConfig: AppConfig
    ): ConfigManager {
        return ConfigManager(configRepository, defaultConfig, emergencyConfig)
    }

    @Provides
    @Singleton
    fun providesAppConfig(
        configManager: ConfigManager
    ): AppConfig {
        // This will return the current configuration from ConfigManager
        // It will be initialized with default config and updated as needed
        return configManager.getCurrentConfig()
    }
}

/**
 * Qualifier annotation for default configuration.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultConfig

/**
 * Qualifier annotation for emergency configuration.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class EmergencyConfig