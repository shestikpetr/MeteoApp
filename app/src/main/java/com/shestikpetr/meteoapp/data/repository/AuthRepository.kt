package com.shestikpetr.meteoapp.data.repository

import com.shestikpetr.meteoapp.data.api.RetrofitClient
import com.shestikpetr.meteoapp.data.model.ApiResponseAuthLoginData
import com.shestikpetr.meteoapp.data.model.ChangePasswordRequest
import com.shestikpetr.meteoapp.data.model.UpdateMeRequest
import com.shestikpetr.meteoapp.data.model.UserLoginRequest
import com.shestikpetr.meteoapp.data.model.UserRegisterRequest
import com.shestikpetr.meteoapp.data.model.UserResponse
import com.shestikpetr.meteoapp.util.TokenStore
import com.shestikpetr.meteoapp.util.UserSessionStore
import retrofit2.Response

class AuthRepository(
    private val tokenStore: TokenStore,
    private val userSessionStore: UserSessionStore
) {
    private val api = RetrofitClient.apiService

    private suspend fun getAuthHeader(): String {
        val token = tokenStore.getAccessToken() ?: ""
        return "Bearer $token"
    }

    suspend fun login(username: String, password: String): AuthResult = try {
        handleAuthResponse(api.login(UserLoginRequest(username, password)))
    } catch (e: Exception) {
        AuthResult.Error(e.message ?: "Network error")
    }

    suspend fun register(username: String, email: String, password: String): AuthResult = try {
        handleAuthResponse(api.register(UserRegisterRequest(username, email, password)))
    } catch (e: Exception) {
        AuthResult.Error(e.message ?: "Network error")
    }

    suspend fun logout() {
        // Сервер для logout JWT-stateless ничего не делает, но всё равно дёрнем эндпоинт
        runCatching { api.logout(getAuthHeader()) }
        tokenStore.clear()
        userSessionStore.clear()
    }

    suspend fun isLoggedIn(): Boolean = tokenStore.isLoggedIn()

    suspend fun me(): Result<UserResponse> = runCatching {
        val response = api.me(getAuthHeader())
        val body = response.body()
        if (!response.isSuccessful || body == null || !body.success || body.data == null) {
            error(body?.error ?: response.errorBody()?.string() ?: "Failed to fetch profile")
        }
        body.data
    }

    suspend fun updateProfile(username: String? = null, email: String? = null): Result<UserResponse> =
        runCatching {
            val response = api.updateMe(getAuthHeader(), UpdateMeRequest(username, email))
            val body = response.body()
            if (!response.isSuccessful || body == null || !body.success || body.data == null) {
                error(body?.error ?: response.errorBody()?.string() ?: "Failed to update profile")
            }
            // Сохраняем обновлённое имя пользователя в локальную сессию
            userSessionStore.save(body.data.username, body.data.email)
            body.data
        }

    suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> =
        runCatching {
            val response = api.changePassword(
                getAuthHeader(),
                ChangePasswordRequest(currentPassword, newPassword)
            )
            val body = response.body()
            if (!response.isSuccessful || body == null || !body.success) {
                error(body?.error ?: response.errorBody()?.string() ?: "Failed to change password")
            }
        }

    private suspend fun handleAuthResponse(response: Response<ApiResponseAuthLoginData>): AuthResult {
        val body = response.body()
        if (!response.isSuccessful || body == null || !body.success || body.data == null) {
            val message = body?.error ?: response.errorBody()?.string() ?: "Authentication failed"
            return AuthResult.Error(message)
        }
        val data = body.data
        tokenStore.saveTokens(data.tokens.accessToken, data.tokens.refreshToken)
        userSessionStore.save(data.user.username, data.user.email)
        return AuthResult.Success(data.user.username)
    }
}
