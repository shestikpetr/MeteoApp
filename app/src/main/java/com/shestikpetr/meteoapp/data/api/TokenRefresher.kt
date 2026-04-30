package com.shestikpetr.meteoapp.data.api

import com.google.gson.Gson
import com.shestikpetr.meteoapp.data.model.ApiResponseRefreshTokenData
import com.shestikpetr.meteoapp.data.model.RefreshTokenRequest
import com.shestikpetr.meteoapp.util.TokenStore
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class TokenRefresher(
    private val baseUrl: String,
    private val tokenStore: TokenStore
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    suspend fun refresh(): String? {
        val refreshToken = tokenStore.getRefreshToken() ?: return null
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
            tokenStore.updateAccessToken(newAccessToken)
            newAccessToken
        } catch (e: Exception) {
            null
        }
    }
}
