package com.shestikpetr.meteo.network

import android.content.Context
import android.util.Log
import com.shestikpetr.meteo.cache.SensorDataCache
import com.shestikpetr.meteo.common.error.UnifiedRetryPolicy
import com.shestikpetr.meteo.config.AppConfig
import com.shestikpetr.meteo.config.ConfigManager
import com.shestikpetr.meteo.network.impl.OkHttpClientWrapper
import com.shestikpetr.meteo.network.interfaces.HttpClient
import com.shestikpetr.meteo.network.interfaces.SecureStorage
import com.shestikpetr.meteo.storage.impl.SharedPreferencesStorage
import com.shestikpetr.meteo.utils.LocationPermissionManager
import com.shestikpetr.meteo.utils.MapKitLifecycleManager
import com.shestikpetr.meteo.utils.RetryPolicy
import com.shestikpetr.meteo.utils.StationDataTransformer
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MeteoModule {

    @Binds
    @Singleton
    abstract fun bindSecureStorage(
        sharedPreferencesStorage: SharedPreferencesStorage
    ): SecureStorage

    companion object {
        @Provides
        @Singleton
        fun providesOkHttpClient(appConfig: AppConfig): OkHttpClient {
        // Создаем селективный логгер на основе конфигурации
        val logging = HttpLoggingInterceptor().apply {
            level = if (appConfig.security.enableNetworkLogging) {
                HttpLoggingInterceptor.Level.HEADERS
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        // Интерцептор для исправления проблем с Content-Length
        val contentLengthFixInterceptor = Interceptor { chain ->
            val request = chain.request()
            val response = chain.proceed(request)

            if (request.url.pathSegments.contains("latest")) {
                try {
                    val responseBody = response.body
                    if (responseBody != null) {
                        // Читаем все доступные данные
                        val bodyBytes = responseBody.bytes()
                        val actualLength = bodyBytes.size
                        val declaredLength = response.header("Content-Length")?.toIntOrNull() ?: 0

                        Log.d("CONTENT_FIX", "URL: ${request.url}")
                        Log.d(
                            "CONTENT_FIX",
                            "Заявленная длина: $declaredLength, фактическая: $actualLength"
                        )

                        // Если есть несоответствие - исправляем
                        if (declaredLength != actualLength) {
                            Log.w(
                                "CONTENT_FIX",
                                "Исправляем Content-Length: $declaredLength -> $actualLength"
                            )
                        }

                        // Создаем новый ответ с правильными данными
                        response.newBuilder()
                            .body(bodyBytes.toResponseBody(responseBody.contentType()))
                            .removeHeader("Content-Length")
                            .addHeader("Content-Length", actualLength.toString())
                            .removeHeader("Connection") // Убираем противоречивые заголовки
                            .addHeader("Connection", "keep-alive")
                            .build()
                    } else {
                        response
                    }
                } catch (e: Exception) {
                    Log.e("CONTENT_FIX", "Ошибка исправления Content-Length: ${e.message}")
                    response
                }
            } else {
                response
            }
        }

        // Простой логгер для отладки (только если включено в конфигурации)
        val customLoggingInterceptor = Interceptor { chain ->
            val request = chain.request()
            val response = chain.proceed(request)

            if (appConfig.security.enableNetworkLogging && request.url.pathSegments.contains("latest")) {
                Log.d("CUSTOM_HTTP", "URL: ${request.url}")
                Log.d("CUSTOM_HTTP", "Response code: ${response.code}")
                Log.d("CUSTOM_HTTP", "Content-Length: ${response.header("Content-Length")}")
                Log.d("CUSTOM_HTTP", "Content-Type: ${response.header("Content-Type")}")
                Log.d("CUSTOM_HTTP", "Connection: ${response.header("Connection")}")
            }

            response
        }

            return OkHttpClient.Builder()
                .addInterceptor(contentLengthFixInterceptor) // ПЕРВЫМ - исправляем данные
                .addInterceptor(customLoggingInterceptor)    // ВТОРЫМ - логируем уже исправленные
                .addInterceptor(logging)                     // ТРЕТЬИМ - стандартное логирование
                .connectTimeout(appConfig.network.connectTimeoutSeconds, TimeUnit.SECONDS)
                .readTimeout(appConfig.network.readTimeoutSeconds, TimeUnit.SECONDS)
                .writeTimeout(appConfig.network.writeTimeoutSeconds, TimeUnit.SECONDS)
                .retryOnConnectionFailure(appConfig.network.retryOnConnectionFailure)
                .build()
        }

        @Provides
        @Singleton
        fun providesRetrofit(okHttpClient: OkHttpClient, appConfig: AppConfig): Retrofit {
            return Retrofit.Builder()
                .baseUrl(appConfig.network.baseUrl)
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
        fun providesAuthRepository(
            meteoApiService: MeteoApiService,
            authManager: AuthManager,
            retryPolicy: UnifiedRetryPolicy
        ): AuthRepository {
            return NetworkAuthRepository(meteoApiService, authManager, retryPolicy)
        }

        @Provides
        @Singleton
        fun providesAuthManager(
            storage: SharedPreferencesStorage,
            meteoApiService: MeteoApiService
        ): AuthManager {
            return AuthManager(storage, meteoApiService)
        }

        // Utility classes are already annotated with @Singleton and @Inject constructor,
        // so Hilt will automatically provide them without explicit @Provides methods.

        @Provides
        @Singleton
        fun providesLocationPermissionManager(): LocationPermissionManager {
            return LocationPermissionManager()
        }

        @Provides
        @Singleton
        fun providesMapKitLifecycleManager(appConfig: AppConfig): MapKitLifecycleManager {
            return MapKitLifecycleManager(appConfig.map)
        }

        @Provides
        @Singleton
        fun providesHttpClient(): HttpClient {
            return OkHttpClientWrapper()
        }
    }
}