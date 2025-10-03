package com.shestikpetr.meteo.network

import com.shestikpetr.meteo.common.logging.MeteoLogger
import com.shestikpetr.meteo.storage.impl.SharedPreferencesStorage
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AuthManager for API v1 with JWT tokens and refresh token support.
 * Handles authentication token management with unified logging.
 */
@Singleton
class AuthManager @Inject constructor(
    private val storage: SharedPreferencesStorage,
    private val apiService: MeteoApiService
) {
    private val mutex = Mutex()
    private val logger = MeteoLogger.forClass(AuthManager::class)

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_EMAIL = "email"
    }

    /**
     * Register new user
     */
    suspend fun register(username: String, email: String, password: String): Boolean {
        return try {
            val registrationData = UserRegistrationData(username, email, password)
            val response = apiService.register(registrationData)

            if (response.isSuccessful && response.body()?.success == true) {
                val authTokens = response.body()?.data
                if (authTokens != null) {
                    saveAuthTokens(authTokens)
                    saveUserInfo(username, email)
                    logger.d("User registered successfully: $username")
                    true
                } else {
                    logger.e("Registration response data is null")
                    false
                }
            } else {
                logger.e("Registration failed: ${response.code()}")
                false
            }
        } catch (e: Exception) {
            logger.e("Registration error: ${e.message}", e)
            false
        }
    }

    /**
     * Save authentication tokens after successful login/register
     */
    suspend fun saveAuthTokens(authTokens: AuthTokens) {
        mutex.withLock {
            storage.putString(KEY_ACCESS_TOKEN, authTokens.access_token)
            storage.putString(KEY_REFRESH_TOKEN, authTokens.refresh_token)
            storage.putString(KEY_USER_ID, authTokens.user_id)  // user_id is now String, no need for toString()
            logger.d("Authentication tokens saved successfully")
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
     * Refresh access token using refresh token
     */
    suspend fun refreshAccessToken(): Boolean {
        return try {
            val refreshToken = getRefreshToken()
            if (refreshToken == null) {
                logger.e("No refresh token available")
                return false
            }

            val response = apiService.refreshToken("Bearer $refreshToken")
            if (response.isSuccessful && response.body()?.success == true) {
                val refreshTokenData = response.body()?.data
                if (refreshTokenData != null) {
                    // Update only the access token
                    storage.putString(KEY_ACCESS_TOKEN, refreshTokenData.access_token)
                    logger.d("Access token refreshed successfully")
                    true
                } else {
                    logger.e("Refresh token response data is null")
                    false
                }
            } else {
                logger.e("Token refresh failed: ${response.code()}")
                // If refresh failed, clear all tokens
                clearAuthData()
                false
            }
        } catch (e: Exception) {
            logger.e("Token refresh error: ${e.message}", e)
            // If refresh failed, clear all tokens
            clearAuthData()
            false
        }
    }

    /**
     * Get authorization header for API requests with automatic token refresh
     */
    suspend fun getAuthorizationHeader(): String? {
        mutex.withLock {
            var accessToken = getAccessToken()
            val refreshToken = getRefreshToken()

            // If no access token, we're not logged in
            if (accessToken == null) {
                logger.d("No access token found")
                if (refreshToken != null) {
                    logger.w("Refresh token exists but access token is missing")
                }
                return null
            }

            // Note: In production, you should check if the token is expired before refreshing
            // For now, we'll use the existing token without auto-refresh to avoid clearing tokens unnecessarily

            // Only try to refresh if we explicitly know the token is expired
            // This prevents aggressive token clearing that was happening before
            logger.d("Using existing access token without auto-refresh")

            return accessToken?.let { "Bearer $it" }
        }
    }

    /**
     * Check if user is currently logged in
     */
    suspend fun isLoggedIn(): Boolean {
        val accessToken = getAccessToken()
        val refreshToken = getRefreshToken()
        val isLoggedIn = accessToken != null && refreshToken != null

        logger.d("Checking login status: access_token=${if (accessToken != null) "present" else "null"}, refresh_token=${if (refreshToken != null) "present" else "null"}, isLoggedIn=$isLoggedIn")

        return isLoggedIn
    }

    /**
     * Fetch current user information from API
     */
    suspend fun fetchCurrentUserInfo(): UserInfo? {
        return try {
            val authHeader = getAuthorizationHeader()
            if (authHeader == null) {
                logger.w("No auth token available for fetching user info")
                return null
            }

            val response = apiService.getCurrentUser(authHeader)
            if (response.isSuccessful && response.body()?.success == true) {
                val userInfo = response.body()?.data
                if (userInfo != null) {
                    // Save user info to local storage
                    saveUserInfo(userInfo.username, userInfo.email)
                    logger.d("User info fetched successfully: ${userInfo.username}")
                    userInfo
                } else {
                    logger.e("User info response data is null")
                    null
                }
            } else {
                logger.e("Failed to fetch user info: ${response.code()}")
                null
            }
        } catch (e: Exception) {
            logger.e("Error fetching user info: ${e.message}", e)
            null
        }
    }

    /**
     * Get current user information from local storage
     */
    suspend fun getCurrentUser(): Triple<String?, String?, String?> {
        val userId = storage.getString(KEY_USER_ID)  // user_id is now String
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
            logger.d("User logged out, all auth data cleared")
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
            "Token present (${token.length} chars)"
        } else {
            "No token"
        }
        logger.d("Token status: $preview")
        return preview
    }

    /**
     * Force clear all authentication data - useful when JWT tokens are malformed
     */
    suspend fun forceLogout() {
        mutex.withLock {
            clearAuthData()
            logger.w("Force logout - all auth data cleared due to token issues")
        }
    }

    /**
     * Get JWT token payload for debugging (decode without verification)
     */
    suspend fun debugJwtToken(): String {
        val token = getAccessToken()
        if (token == null) {
            return "No token available"
        }

        try {
            // JWT tokens have 3 parts separated by dots: header.payload.signature
            val parts = token.split(".")
            if (parts.size != 3) {
                return "Invalid JWT format (expected 3 parts, got ${parts.size})"
            }

            // Decode the payload (second part)
            val payload = parts[1]

            // Add padding if needed for Base64 decoding
            val paddedPayload = payload + "=".repeat((4 - payload.length % 4) % 4)

            val decodedBytes = android.util.Base64.decode(paddedPayload, android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP)
            val decodedPayload = String(decodedBytes, Charsets.UTF_8)

            logger.d("JWT token decoded successfully")
            return decodedPayload

        } catch (e: Exception) {
            val error = "Failed to decode JWT: ${e.message}"
            logger.e(error)
            return error
        }
    }
}