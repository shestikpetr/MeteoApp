package com.shestikpetr.meteoapp.di

import com.shestikpetr.meteoapp.data.repository.AuthRepositoryImpl
import com.shestikpetr.meteoapp.data.repository.SettingsRepositoryImpl
import com.shestikpetr.meteoapp.data.repository.StationRepositoryImpl
import com.shestikpetr.meteoapp.domain.repository.AuthRepository
import com.shestikpetr.meteoapp.domain.repository.SettingsRepository
import com.shestikpetr.meteoapp.domain.repository.StationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindStationRepository(impl: StationRepositoryImpl): StationRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}
