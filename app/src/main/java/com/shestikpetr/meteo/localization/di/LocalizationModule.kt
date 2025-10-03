package com.shestikpetr.meteo.localization.di

import com.shestikpetr.meteo.localization.cache.LocalizationCacheImpl
import com.shestikpetr.meteo.localization.embedded.EmbeddedStringProviderImpl
import com.shestikpetr.meteo.localization.formatter.StringFormatterImpl
import com.shestikpetr.meteo.localization.interfaces.*
import com.shestikpetr.meteo.localization.network.LocalizationApiService
import com.shestikpetr.meteo.localization.network.NetworkStringLoaderImpl
import com.shestikpetr.meteo.localization.preferences.LocalePreferencesImpl
import com.shestikpetr.meteo.localization.repository.CachedStringRepository
import com.shestikpetr.meteo.localization.repository.NetworkStringRepository
import com.shestikpetr.meteo.localization.service.LocalizationServiceImpl
import com.shestikpetr.meteo.localization.service.StringResourceManagerImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Named
import javax.inject.Singleton

/**
 * Hilt module for localization dependency injection
 * Follows SOLID principles by properly abstracting dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class LocalizationModule {

    // Interface bindings
    @Binds
    @Singleton
    abstract fun bindStringResourceManager(
        impl: StringResourceManagerImpl
    ): StringResourceManager

    @Binds
    @Singleton
    abstract fun bindLocalizationService(
        impl: LocalizationServiceImpl
    ): LocalizationService

    @Binds
    @Singleton
    abstract fun bindStringFormatter(
        impl: StringFormatterImpl
    ): StringFormatter

    @Binds
    @Singleton
    abstract fun bindLocalizationCache(
        impl: LocalizationCacheImpl
    ): LocalizationCache

    @Binds
    @Singleton
    abstract fun bindEmbeddedStringProvider(
        impl: EmbeddedStringProviderImpl
    ): EmbeddedStringProvider

    @Binds
    @Singleton
    abstract fun bindNetworkStringLoader(
        impl: NetworkStringLoaderImpl
    ): NetworkStringLoader

    @Binds
    @Singleton
    abstract fun bindLocalePreferences(
        impl: LocalePreferencesImpl
    ): LocalePreferences

    @Binds
    @Singleton
    @Named("network")
    abstract fun bindNetworkLocalizationRepository(
        impl: NetworkStringRepository
    ): LocalizationRepository

    companion object {
        @Provides
        @Singleton
        fun provideLocalizationApiService(retrofit: Retrofit): LocalizationApiService {
            return retrofit.create(LocalizationApiService::class.java)
        }

        @Provides
        @Singleton
        fun provideLocalizationRepository(
            @Named("network") networkRepository: LocalizationRepository,
            cache: LocalizationCache,
            embeddedProvider: EmbeddedStringProvider
        ): LocalizationRepository {
            return CachedStringRepository(networkRepository, cache, embeddedProvider)
        }
    }
}