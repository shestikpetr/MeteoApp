package com.shestikpetr.meteoapp.di

import com.google.gson.Gson
import com.shestikpetr.meteoapp.data.local.TokenStorage
import com.shestikpetr.meteoapp.data.remote.api.MeteoApi
import com.shestikpetr.meteoapp.data.remote.interceptor.AuthInterceptor
import com.shestikpetr.meteoapp.data.remote.interceptor.TokenAuthenticator
import com.shestikpetr.meteoapp.data.remote.refresh.TokenRefresher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val BASE_URL = "https://your-backend.example.com/"

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()

    @Provides
    @Singleton
    fun provideTokenRefresher(
        tokenStorage: TokenStorage,
        gson: Gson
    ): TokenRefresher = TokenRefresher(BASE_URL, tokenStorage, gson)

    @Provides
    @Singleton
    fun provideAuthInterceptor(tokenStorage: TokenStorage): AuthInterceptor =
        AuthInterceptor(tokenStorage)

    @Provides
    @Singleton
    fun provideAuthenticator(
        tokenStorage: TokenStorage,
        tokenRefresher: TokenRefresher
    ): TokenAuthenticator = TokenAuthenticator(tokenStorage, tokenRefresher)

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        authenticator: TokenAuthenticator
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .authenticator(authenticator)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, gson: Gson): Retrofit =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    @Provides
    @Singleton
    fun provideMeteoApi(retrofit: Retrofit): MeteoApi = retrofit.create(MeteoApi::class.java)
}
