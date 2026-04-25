package com.shestikpetr.meteoapp.data.repository

import com.shestikpetr.meteoapp.data.api.RetrofitClient
import com.shestikpetr.meteoapp.data.model.AuthResponse
import com.shestikpetr.meteoapp.data.model.UserLoginRequest
import com.shestikpetr.meteoapp.data.model.UserRegisterRequest
import com.shestikpetr.meteoapp.util.TokenStore
import com.shestikpetr.meteoapp.util.UserSessionStore
import retrofit2.Response

class AuthRepository(
    private val tokenStore: TokenStore,
    private val userSessionStore: UserSessionStore
) {
    private val api = RetrofitClient.apiService

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
        runCatching { api.logout() }
        tokenStore.clear()
        userSessionStore.clear()
    }

    suspend fun isLoggedIn(): Boolean = tokenStore.isLoggedIn()

    private suspend fun handleAuthResponse(response: Response<AuthResponse>): AuthResult {
        val body = response.body()
        if (!response.isSuccessful || body == null) {
            val errorBody = response.errorBody()?.string()
            return AuthResult.Error(errorBody ?: "Authentication failed")
        }
        val data = body.data
        tokenStore.saveTokens(data.accessToken, data.refreshToken)
        userSessionStore.save(data.userId, data.username)
        return AuthResult.Success(data.username)
    }
}
