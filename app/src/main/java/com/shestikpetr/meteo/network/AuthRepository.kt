package com.shestikpetr.meteo.network

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

// Интерфейс репозитория аутентификации для API v1
interface AuthRepository {
    suspend fun login(username: String, password: String): LoginResult
    suspend fun register(username: String, password: String, email: String): LoginResult
    suspend fun logout()
    suspend fun isLoggedIn(): Boolean
    suspend fun getCurrentUser(): UserInfo?
}

// Результат аутентификации для API v1
sealed class LoginResult {
    data class Success(val accessToken: String, val refreshToken: String, val userId: Int) : LoginResult()
    data class Error(val message: String) : LoginResult()
    object Loading : LoginResult()
}

// Реализация репозитория аутентификации
@Singleton
class NetworkAuthRepository @Inject constructor(
    private val meteoApiService: MeteoApiService,
    private val authManager: AuthManager
) : AuthRepository {

    override suspend fun login(username: String, password: String): LoginResult {
        try {
            Log.d("AuthRepository", "Попытка входа для пользователя: $username")
            val credentials = UserCredentials(username, password)
            val response = meteoApiService.login(credentials)

            if (response.isSuccessful) {
                val apiResponse = response.body()
                if (apiResponse?.success == true && apiResponse.data != null) {
                    val authTokens = apiResponse.data
                    Log.d("AuthRepository", "Успешный вход в систему для user_id: ${authTokens.user_id}")

                    // Сохраняем токены
                    authManager.saveAuthTokens(authTokens)
                    authManager.saveUserInfo(username, "")

                    return LoginResult.Success(
                        accessToken = authTokens.access_token,
                        refreshToken = authTokens.refresh_token,
                        userId = authTokens.user_id
                    )
                } else {
                    Log.e("AuthRepository", "Ошибка входа: API returned success=false")
                    return LoginResult.Error("Ошибка авторизации")
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("AuthRepository", "Ошибка сервера: ${response.code()}, $errorBody")
                return LoginResult.Error("Ошибка сервера: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Ошибка входа", e)
            return LoginResult.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    override suspend fun register(username: String, password: String, email: String): LoginResult {
        try {
            Log.d("AuthRepository", "Попытка регистрации для пользователя: $username")
            val registrationData = UserRegistrationData(username, email, password)
            val response = meteoApiService.register(registrationData)

            if (response.isSuccessful) {
                val apiResponse = response.body()
                if (apiResponse?.success == true && apiResponse.data != null) {
                    val authTokens = apiResponse.data
                    Log.d("AuthRepository", "Успешная регистрация для user_id: ${authTokens.user_id}")

                    // Сохраняем токены и информацию о пользователе
                    authManager.saveAuthTokens(authTokens)
                    authManager.saveUserInfo(username, email)

                    return LoginResult.Success(
                        accessToken = authTokens.access_token,
                        refreshToken = authTokens.refresh_token,
                        userId = authTokens.user_id
                    )
                } else {
                    Log.e("AuthRepository", "Ошибка регистрации: API returned success=false")
                    return LoginResult.Error("Ошибка регистрации")
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("AuthRepository", "Ошибка сервера при регистрации: ${response.code()}, $errorBody")
                return LoginResult.Error("Ошибка сервера: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Ошибка регистрации", e)
            return LoginResult.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    override suspend fun logout() {
        authManager.logout()
    }

    override suspend fun isLoggedIn(): Boolean {
        return authManager.isLoggedIn()
    }

    override suspend fun getCurrentUser(): UserInfo? {
        return try {
            val authHeader = authManager.getAuthorizationHeader()
            if (authHeader != null) {
                val response = meteoApiService.getCurrentUser(authHeader)
                if (response.isSuccessful && response.body()?.success == true) {
                    response.body()?.data
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Failed to get current user", e)
            null
        }
    }
}