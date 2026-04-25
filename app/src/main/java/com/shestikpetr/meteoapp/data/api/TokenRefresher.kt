package com.shestikpetr.meteoapp.data.api

import com.google.gson.Gson
import com.shestikpetr.meteoapp.data.model.RefreshTokenResponse
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

    suspend fun refresh(): String? {
        val refreshToken = tokenStore.getRefreshToken() ?: return null
        return try {
            val request = Request.Builder()
                .url("${baseUrl}api/v1/auth/refresh")
                .post("".toRequestBody("application/json".toMediaType()))
                .addHeader("Authorization", "Bearer $refreshToken")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()
                val tokenResponse = Gson().fromJson(body, RefreshTokenResponse::class.java)
                tokenStore.updateAccessToken(tokenResponse.accessToken)
                tokenResponse.accessToken
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
