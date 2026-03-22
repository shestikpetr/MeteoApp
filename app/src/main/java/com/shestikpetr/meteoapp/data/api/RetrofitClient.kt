package com.shestikpetr.meteoapp.data.api

import com.google.gson.Gson
import com.shestikpetr.meteoapp.data.model.RefreshTokenResponse
import com.shestikpetr.meteoapp.util.TokenManager
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private const val BASE_URL = "http://your-backend.example.com/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private var _apiService: ApiService? = null

    fun init(tokenManager: TokenManager) {
        val authenticator = TokenAuthenticator(tokenManager)

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

    private class TokenAuthenticator(private val tokenManager: TokenManager) : Authenticator {
        private val lock = Any()

        override fun authenticate(route: Route?, response: Response): Request? {
            // Don't retry more than once
            if (response.priorResponse != null) return null

            synchronized(lock) {
                // Check if another thread already refreshed the token
                val currentToken = runBlocking { tokenManager.getAccessToken() }
                val requestToken = response.request.header("Authorization")
                    ?.removePrefix("Bearer ")

                if (currentToken != null && currentToken != requestToken) {
                    // Token was already refreshed by another request
                    return response.request.newBuilder()
                        .header("Authorization", "Bearer $currentToken")
                        .build()
                }

                // Refresh the token
                val newToken = runBlocking { refreshAccessToken() } ?: return null

                return response.request.newBuilder()
                    .header("Authorization", "Bearer $newToken")
                    .build()
            }
        }

        private suspend fun refreshAccessToken(): String? {
            val refreshToken = tokenManager.getRefreshToken() ?: return null
            return try {
                val client = OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .build()

                val request = Request.Builder()
                    .url("${BASE_URL}api/v1/auth/refresh")
                    .post("".toRequestBody("application/json".toMediaType()))
                    .addHeader("Authorization", "Bearer $refreshToken")
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    val tokenResponse = Gson().fromJson(body, RefreshTokenResponse::class.java)
                    tokenManager.updateAccessToken(tokenResponse.accessToken)
                    tokenResponse.accessToken
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }
}
