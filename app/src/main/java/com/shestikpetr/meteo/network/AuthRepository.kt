package com.shestikpetr.meteo.network

import javax.inject.Inject
import javax.inject.Singleton

// Интерфейс репозитория аутентификации
interface AuthRepository {
    suspend fun login(username: String, password: String): LoginResult
    fun logout()
    fun isLoggedIn(): Boolean
}

// Результат аутентификации
sealed class LoginResult {
    data class Success(val token: String) : LoginResult()
    data class Error(val message: String) : LoginResult()
}

// Реализация репозитория аутентификации
@Singleton
class NetworkAuthRepository @Inject constructor(
    private val meteoApiService: MeteoApiService,
    private val authManager: AuthManager
) : AuthRepository {

    override suspend fun login(username: String, password: String): LoginResult {
        return try {
            val credentials = UserCredentials(username, password)
            val response = meteoApiService.login(credentials)

            if (response.success && response.token != null) {
                authManager.saveCredentials(username, password)
                LoginResult.Success(response.token)
            } else {
                LoginResult.Error(response.error ?: "Ошибка авторизации")
            }
        } catch (e: Exception) {
            LoginResult.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    override fun logout() {
        authManager.clearCredentials()
    }

    override fun isLoggedIn(): Boolean {
        return authManager.hasCredentials()
    }
}