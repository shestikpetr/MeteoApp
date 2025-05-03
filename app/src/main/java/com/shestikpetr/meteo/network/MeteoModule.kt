package com.shestikpetr.meteo.network

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

// Инъекция зависимостей
@Module
@InstallIn(SingletonComponent::class)
object MeteoModule {
    @Provides
    @Singleton
    fun providesOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun providesRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("http://84.237.1.131:8085/api/") // Обновленный URL из app.py
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun providesMeteoApiService(retrofit: Retrofit): MeteoApiService {
        return retrofit.create(MeteoApiService::class.java)
    }

    @Provides
    @Singleton
    fun providesMeteoRepository(
        meteoApiService: MeteoApiService,
        authManager: AuthManager
    ): MeteoRepository {
        return NetworkMeteoRepository(meteoApiService, authManager)
    }

    @Provides
    @Singleton
    fun providesAuthRepository(
        meteoApiService: MeteoApiService,
        authManager: AuthManager
    ): AuthRepository {
        return NetworkAuthRepository(meteoApiService, authManager)
    }

    @Provides
    @Singleton
    fun providesAuthManager(@ApplicationContext context: Context): AuthManager {
        return AuthManager(context)
    }
}