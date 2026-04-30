package com.shestikpetr.meteoapp.data.remote.interceptor

import com.shestikpetr.meteoapp.data.local.TokenStorage
import com.shestikpetr.meteoapp.data.remote.refresh.TokenRefresher
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * При получении 401 пробует обновить access-token через refresh-token и повторить запрос.
 * Если параллельно несколько запросов получили 401 — обновляет токен один раз
 * (синхронизируется через [lock]) и переиспользует свежий токен.
 */
class TokenAuthenticator(
    private val tokenStorage: TokenStorage,
    private val tokenRefresher: TokenRefresher
) : Authenticator {

    private val lock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.priorResponse != null) return null // одна попытка обновления

        synchronized(lock) {
            val current = runBlocking { tokenStorage.getAccessToken() }
            val requestToken = response.request.header("Authorization")?.removePrefix("Bearer ")

            if (current != null && current != requestToken) {
                // токен уже обновили в параллельном потоке — просто повторяем с ним
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $current")
                    .build()
            }

            val newToken = runBlocking { tokenRefresher.refresh() } ?: return null
            return response.request.newBuilder()
                .header("Authorization", "Bearer $newToken")
                .build()
        }
    }
}
