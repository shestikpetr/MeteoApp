package com.shestikpetr.meteoapp.data.remote.interceptor

import com.shestikpetr.meteoapp.data.local.TokenStorage
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Добавляет заголовок `Authorization: Bearer <accessToken>` ко всем запросам,
 * кроме явных публичных эндпоинтов аутентификации.
 *
 * Запрос к `auth/login`, `auth/register`, `auth/refresh`, `health` отправляется без токена,
 * даже если токен в хранилище есть.
 */
class AuthInterceptor(
    private val tokenStorage: TokenStorage
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath
        val needsAuth = !PUBLIC_PATHS.any { path.endsWith(it) }

        val authedRequest = if (needsAuth) {
            val token = runBlocking { tokenStorage.getAccessToken() }
            if (token.isNullOrBlank()) request
            else request.newBuilder().header("Authorization", "Bearer $token").build()
        } else request

        return chain.proceed(authedRequest)
    }

    private companion object {
        val PUBLIC_PATHS = listOf(
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/refresh",
            "/health"
        )
    }
}
