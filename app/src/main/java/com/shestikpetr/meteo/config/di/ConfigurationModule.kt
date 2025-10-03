package com.shestikpetr.meteo.config.di

import com.shestikpetr.meteo.config.cache.ConfigurationCache
import com.shestikpetr.meteo.config.impl.*
import com.shestikpetr.meteo.config.interfaces.*
import com.shestikpetr.meteo.config.network.ConfigurationApiService
import com.shestikpetr.meteo.config.ui.DynamicMeteoColors
import com.shestikpetr.meteo.config.utils.ValidationUtils
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

/**
 * Hilt module for configuration dependency injection.
 * Provides all configuration-related dependencies following SOLID principles.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ConfigurationModule {

    /**
     * Binds ValidationConfigRepository implementation.
     * Follows Dependency Inversion Principle by depending on abstractions.
     */
    @Binds
    @Singleton
    abstract fun bindValidationConfigRepository(
        networkValidationConfigRepository: NetworkValidationConfigRepository
    ): ValidationConfigRepository

    /**
     * Binds RetryConfigRepository implementation.
     */
    @Binds
    @Singleton
    abstract fun bindRetryConfigRepository(
        networkRetryConfigRepository: NetworkRetryConfigRepository
    ): RetryConfigRepository

    /**
     * Binds ThemeConfigRepository implementation.
     */
    @Binds
    @Singleton
    abstract fun bindThemeConfigRepository(
        networkThemeConfigRepository: NetworkThemeConfigRepository
    ): ThemeConfigRepository

    /**
     * Binds DemoConfigRepository implementation.
     */
    @Binds
    @Singleton
    abstract fun bindDemoConfigRepository(
        networkDemoConfigRepository: NetworkDemoConfigRepository
    ): DemoConfigRepository

    companion object {
        /**
         * Provides ConfigurationApiService using the main Retrofit instance.
         */
        @Provides
        @Singleton
        fun provideConfigurationApiService(retrofit: Retrofit): ConfigurationApiService {
            return retrofit.create(ConfigurationApiService::class.java)
        }

        /**
         * Provides ConfigurationCache singleton.
         */
        @Provides
        @Singleton
        fun provideConfigurationCache(): ConfigurationCache {
            return ConfigurationCache()
        }

        /**
         * Provides DynamicMeteoColors singleton.
         */
        @Provides
        @Singleton
        fun provideDynamicMeteoColors(
            themeConfigRepository: ThemeConfigRepository
        ): DynamicMeteoColors {
            return DynamicMeteoColors(themeConfigRepository)
        }

        /**
         * Provides ValidationUtils singleton.
         */
        @Provides
        @Singleton
        fun provideValidationUtils(
            validationConfigRepository: ValidationConfigRepository
        ): ValidationUtils {
            return ValidationUtils(validationConfigRepository)
        }
    }
}