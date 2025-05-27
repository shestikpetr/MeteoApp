package com.shestikpetr.meteo.network

import android.content.Context
import android.util.Log
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
object MeteoModule {
    @Provides
    @Singleton
    fun providesOkHttpClient(): OkHttpClient {
        // Создаем селективный логгер
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
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

        // Простой логгер для отладки
        val customLoggingInterceptor = Interceptor { chain ->
            val request = chain.request()
            val response = chain.proceed(request)

            if (request.url.pathSegments.contains("latest")) {
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
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    @Provides
    @Singleton
    fun providesRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("http://84.237.1.131:8085/api/")
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