package com.shestikpetr.meteoapp.data.remote.refresh

import com.google.gson.Gson
import com.shestikpetr.meteoapp.data.local.TokenStorage
import com.shestikpetr.meteoapp.data.remote.dto.ApiResponseRefreshTokenData
import com.shestikpetr.meteoapp.data.remote.dto.RefreshTokenRequest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Отдельный «голый» OkHttp-клиент без AuthInterceptor/Authenticator, чтобы избежать
 * рекурсии при попытке обновить токен.
 */
class TokenRefresher(
    private val baseUrl: String,
    private val tokenStorage: TokenStorage,
    private val gson: Gson = Gson(),
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) {
    suspend fun refresh(): String? {
        val refreshToken = tokenStorage.getRefreshToken() ?: return null
        return try {
            val body = gson.toJson(RefreshTokenRequest(refreshToken))
                .toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("${baseUrl}api/v1/auth/refresh")
                .post(body)
                .build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return null
            val parsed = gson.fromJson(response.body?.string(), ApiResponseRefreshTokenData::class.java)
            val newAccessToken = parsed.data?.accessToken ?: return null
            tokenStorage.updateAccessToken(newAccessToken)
            newAccessToken
        } catch (e: Exception) {
            null
        }
    }
}
