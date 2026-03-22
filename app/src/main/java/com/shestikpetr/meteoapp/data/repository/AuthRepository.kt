package com.shestikpetr.meteoapp.data.repository

import com.shestikpetr.meteoapp.data.api.RetrofitClient
import com.shestikpetr.meteoapp.data.model.UserLoginRequest
import com.shestikpetr.meteoapp.data.model.UserRegisterRequest
import com.shestikpetr.meteoapp.util.TokenManager

sealed class AuthResult {
    data class Success(val username: String) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

class AuthRepository(private val tokenManager: TokenManager) {

    private val api = RetrofitClient.apiService

    suspend fun login(username: String, password: String): AuthResult {
        return try {
            val response = api.login(UserLoginRequest(username, password))
            if (response.isSuccessful && response.body() != null) {
                val authData = response.body()!!.data
                tokenManager.saveTokens(
                    accessToken = authData.accessToken,
                    refreshToken = authData.refreshToken,
                    userId = authData.userId,
                    username = authData.username
                )
                AuthResult.Success(authData.username)
            } else {
                val errorBody = response.errorBody()?.string()
                AuthResult.Error(errorBody ?: "Login failed")
            }
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun register(username: String, email: String, password: String): AuthResult {
        return try {
            val response = api.register(UserRegisterRequest(username, email, password))
            if (response.isSuccessful && response.body() != null) {
                val authData = response.body()!!.data
                tokenManager.saveTokens(
                    accessToken = authData.accessToken,
                    refreshToken = authData.refreshToken,
                    userId = authData.userId,
                    username = authData.username
                )
                AuthResult.Success(authData.username)
            } else {
                val errorBody = response.errorBody()?.string()
                AuthResult.Error(errorBody ?: "Registration failed")
            }
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun logout() {
        try {
            api.logout()
        } catch (_: Exception) {
            // Ignore network errors on logout
        }
        tokenManager.clearTokens()
    }

    suspend fun isLoggedIn(): Boolean {
        return tokenManager.isLoggedIn()
    }
}
