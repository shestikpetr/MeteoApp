package com.shestikpetr.meteo.network

import android.util.Log
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
        try {
            Log.d("AuthRepository", "Attempting login for user: $username")
            val credentials = UserCredentials(username, password)
            val response = meteoApiService.login(credentials)

            if (response.isSuccessful) {
                val loginResponse = response.body()
                if (loginResponse?.success == true) {
                    authManager.saveCredentials(username, password)
                    return LoginResult.Success(loginResponse.token ?: "")
                } else {
                    return LoginResult.Error(loginResponse?.error ?: "Ошибка авторизации")
                }
            } else {
                return LoginResult.Error("Ошибка сервера: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Login error", e)
            return LoginResult.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    override fun logout() {
        authManager.clearCredentials()
    }

    override fun isLoggedIn(): Boolean {
        return authManager.hasCredentials()
    }
}