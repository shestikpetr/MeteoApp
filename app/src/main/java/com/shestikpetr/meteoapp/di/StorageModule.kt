package com.shestikpetr.meteoapp.di

import android.content.Context
import com.shestikpetr.meteoapp.data.local.SessionStorage
import com.shestikpetr.meteoapp.data.local.SessionStorageDataStore
import com.shestikpetr.meteoapp.data.local.SettingsStorage
import com.shestikpetr.meteoapp.data.local.SettingsStorageDataStore
import com.shestikpetr.meteoapp.data.local.TokenStorage
import com.shestikpetr.meteoapp.data.local.TokenStorageDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object StorageModule {

    @Provides
    @Singleton
    fun provideTokenStorage(@ApplicationContext context: Context): TokenStorage =
        TokenStorageDataStore(context)

    @Provides
    @Singleton
    fun provideSessionStorage(@ApplicationContext context: Context): SessionStorage =
        SessionStorageDataStore(context)

    @Provides
    @Singleton
    fun provideSettingsStorage(@ApplicationContext context: Context): SettingsStorage =
        SettingsStorageDataStore(context)
}
