package com.shestikpetr.meteo.repository

import com.shestikpetr.meteo.repository.impl.CachedParameterDisplayRepository
import com.shestikpetr.meteo.repository.impl.NetworkParameterMetadataRepository
import com.shestikpetr.meteo.repository.impl.NetworkParameterVisibilityRepository
import com.shestikpetr.meteo.repository.impl.NetworkSensorDataRepository
import com.shestikpetr.meteo.repository.impl.NetworkStationRepository
import com.shestikpetr.meteo.repository.interfaces.ParameterDisplayRepository
import com.shestikpetr.meteo.repository.interfaces.ParameterMetadataRepository
import com.shestikpetr.meteo.repository.interfaces.ParameterVisibilityRepository
import com.shestikpetr.meteo.repository.interfaces.SensorDataRepository
import com.shestikpetr.meteo.repository.interfaces.StationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for binding repository interfaces to their concrete implementations.
 * This module follows the Dependency Inversion Principle by allowing ViewModels
 * to depend on abstractions (interfaces) rather than concrete implementations.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    /**
     * Binds the SensorDataRepository interface to its network implementation.
     * This allows ViewModels to depend on the interface while using the concrete network implementation.
     */
    @Binds
    @Singleton
    abstract fun bindSensorDataRepository(
        networkSensorDataRepository: NetworkSensorDataRepository
    ): SensorDataRepository

    /**
     * Binds the StationRepository interface to its network implementation.
     * This allows ViewModels to depend on the interface while using the concrete network implementation.
     */
    @Binds
    @Singleton
    abstract fun bindStationRepository(
        networkStationRepository: NetworkStationRepository
    ): StationRepository

    /**
     * Binds the ParameterMetadataRepository interface to its network implementation.
     * This allows ViewModels to depend on the interface while using the concrete network implementation.
     */
    @Binds
    @Singleton
    abstract fun bindParameterMetadataRepository(
        networkParameterMetadataRepository: NetworkParameterMetadataRepository
    ): ParameterMetadataRepository

    /**
     * Binds the ParameterDisplayRepository interface to its cached implementation.
     * This allows services to depend on the interface while using the concrete cached implementation.
     */
    @Binds
    @Singleton
    abstract fun bindParameterDisplayRepository(
        cachedParameterDisplayRepository: CachedParameterDisplayRepository
    ): ParameterDisplayRepository

    /**
     * Binds the ParameterVisibilityRepository interface to its network implementation.
     * This allows ViewModels to depend on the interface while using the concrete network implementation.
     */
    @Binds
    @Singleton
    abstract fun bindParameterVisibilityRepository(
        networkParameterVisibilityRepository: NetworkParameterVisibilityRepository
    ): ParameterVisibilityRepository
}