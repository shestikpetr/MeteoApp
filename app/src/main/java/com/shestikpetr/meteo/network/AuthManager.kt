package com.shestikpetr.meteo.network

import android.util.Log
import com.shestikpetr.meteo.storage.impl.SharedPreferencesStorage
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AuthManager for API v1 with JWT tokens and refresh token support
 */
@Singleton
class AuthManager @Inject constructor(
    private val storage: SharedPreferencesStorage,
    private val apiService: MeteoApiService
) {
    private val mutex = Mutex()

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_EMAIL = "email"
        private const val TAG = "AuthManager"
    }

    /**
     * Save authentication tokens after successful login/register
     */
    suspend fun saveAuthTokens(authTokens: AuthTokens) {
        mutex.withLock {
            storage.putString(KEY_ACCESS_TOKEN, authTokens.access_token)
            storage.putString(KEY_REFRESH_TOKEN, authTokens.refresh_token)
            storage.putString(KEY_USER_ID, authTokens.user_id.toString())
            Log.d(TAG, "Tokens saved for user ID: ${authTokens.user_id}")
        }
    }

    /**
     * Save user information from /auth/me or login response
     */
    suspend fun saveUserInfo(username: String, email: String) {
        storage.putString(KEY_USERNAME, username)
        storage.putString(KEY_EMAIL, email)
    }

    /**
     * Get current access token for API requests
     */
    suspend fun getAccessToken(): String? {
        return storage.getString(KEY_ACCESS_TOKEN)
    }

    /**
     * Get current refresh token
     */
    private suspend fun getRefreshToken(): String? {
        return storage.getString(KEY_REFRESH_TOKEN)
    }

    /**
     * Get authorization header for API requests with automatic token refresh
     */
    suspend fun getAuthorizationHeader(): String? {
        mutex.withLock {
            var accessToken = getAccessToken()

            // If no access token, we're not logged in
            if (accessToken == null) {
                Log.d(TAG, "No access token found")
                return null
            }

            // Try to refresh token if we have a refresh token
            // In a real implementation, you'd check if the token is expired first
            val refreshToken = getRefreshToken()
            if (refreshToken != null) {
                try {
                    val response = apiService.refreshToken("Bearer $refreshToken")
                    if (response.isSuccessful && response.body()?.success == true) {
                        val newAccessToken = response.body()?.access_token
                        if (newAccessToken != null) {
                            storage.putString(KEY_ACCESS_TOKEN, newAccessToken)
                            accessToken = newAccessToken
                            Log.d(TAG, "Access token refreshed successfully")
                        }
                    } else {
                        Log.w(TAG, "Failed to refresh token: ${response.code()}")
                        // If refresh fails, user needs to login again
                        clearAuthData()
                        return null
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error refreshing token", e)
                    // On network error, use existing token
                }
            }

            return accessToken?.let { "Bearer $it" }
        }
    }

    /**
     * Check if user is currently logged in
     */
    suspend fun isLoggedIn(): Boolean {
        return getAccessToken() != null && getRefreshToken() != null
    }

    /**
     * Get current user information
     */
    suspend fun getCurrentUser(): Triple<Int?, String?, String?> {
        val userId = storage.getString(KEY_USER_ID)?.toIntOrNull()
        val username = storage.getString(KEY_USERNAME)
        val email = storage.getString(KEY_EMAIL)
        return Triple(userId, username, email)
    }

    /**
     * Logout and clear all authentication data
     */
    suspend fun logout() {
        mutex.withLock {
            clearAuthData()
            Log.d(TAG, "User logged out, all auth data cleared")
        }
    }

    /**
     * Clear all authentication related data
     */
    private suspend fun clearAuthData() {
        storage.remove(KEY_ACCESS_TOKEN)
        storage.remove(KEY_REFRESH_TOKEN)
        storage.remove(KEY_USER_ID)
        storage.remove(KEY_USERNAME)
        storage.remove(KEY_EMAIL)
    }

    /**
     * Get auth token for debugging (first 10 chars)
     */
    suspend fun getAuthTokenDebug(): String {
        val token = getAccessToken()
        val preview = if (token != null && token.length > 10) {
            "${token.take(10)}..."
        } else {
            "No token"
        }
        Log.d(TAG, "Current token: $preview")
        return preview
    }
}