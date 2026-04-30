package com.shestikpetr.meteoapp.data.api

import com.shestikpetr.meteoapp.util.TokenStore
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private const val BASE_URL = "https://your-backend.example.com/"

    private var _apiService: ApiService? = null

    fun init(tokenStore: TokenStore) {
        val tokenRefresher = TokenRefresher(BASE_URL, tokenStore)
        val authenticator = TokenAuthenticator(tokenStore, tokenRefresher)

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .authenticator(authenticator)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        _apiService = retrofit.create(ApiService::class.java)
    }

    val apiService: ApiService
        get() = _apiService
            ?: throw IllegalStateException("RetrofitClient.init() must be called first")
}
