package com.shestikpetr.meteo.network

import com.shestikpetr.meteo.common.error.MeteoResult
import com.shestikpetr.meteo.common.error.MeteoError
import com.shestikpetr.meteo.common.error.UnifiedRetryPolicy
import com.shestikpetr.meteo.common.error.retrofitCall
import com.shestikpetr.meteo.common.logging.MeteoLogger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository interface for authentication operations using API v1.
 * All methods return MeteoResult for consistent error handling.
 */
interface AuthRepository {
    suspend fun login(username: String, password: String): MeteoResult<AuthTokens>
    suspend fun register(username: String, password: String, email: String): MeteoResult<AuthTokens>
    suspend fun logout(): MeteoResult<Unit>
    suspend fun isLoggedIn(): Boolean
    suspend fun getCurrentUser(): MeteoResult<UserInfo>
    suspend fun forceLogout(): MeteoResult<Unit>
    suspend fun debugJwtToken(): String
}


/**
 * Network implementation of AuthRepository interface.
 * Handles authentication operations with unified error handling and retry logic.
 */
@Singleton
class NetworkAuthRepository @Inject constructor(
    private val meteoApiService: MeteoApiService,
    private val authManager: AuthManager,
    private val retryPolicy: UnifiedRetryPolicy
) : AuthRepository {

    private val logger = MeteoLogger.forClass(NetworkAuthRepository::class)

    override suspend fun login(username: String, password: String): MeteoResult<AuthTokens> {
        logger.startOperation("login", "username" to username)

        return retryPolicy.executeWithRetry(retryPolicy.createAuthRetryConfig()) { attempt ->
            val credentials = UserCredentials(username, password)
            val response = meteoApiService.login(credentials)

            if (response.isSuccessful && response.body()?.success == true) {
                val tokens = response.body()?.data
                if (tokens != null) {
                    // Save authentication data
                    authManager.saveAuthTokens(tokens)
                    authManager.saveUserInfo(username, "")

                    logger.completeOperation("login", "user_id=${tokens.user_id}")
                    tokens
                } else {
                    throw IllegalStateException("Login response data is null")
                }
            } else {
                throw IllegalStateException("Login failed: ${response.code()}")
            }
        }
    }

    override suspend fun register(username: String, password: String, email: String): MeteoResult<AuthTokens> {
        logger.startOperation("register", "username" to username, "email" to email)

        return retryPolicy.executeWithRetry(retryPolicy.createAuthRetryConfig()) { attempt ->
            val registrationData = UserRegistrationData(username, email, password)
            val response = meteoApiService.register(registrationData)

            if (response.isSuccessful && response.body()?.success == true) {
                val tokens = response.body()?.data
                if (tokens != null) {
                    // Save authentication data
                    authManager.saveAuthTokens(tokens)
                    authManager.saveUserInfo(username, email)

                    logger.completeOperation("register", "user_id=${tokens.user_id}")
                    tokens
                } else {
                    throw IllegalStateException("Registration response data is null")
                }
            } else {
                throw IllegalStateException("Registration failed: ${response.code()}")
            }
        }
    }

    override suspend fun logout(): MeteoResult<Unit> {
        logger.startOperation("logout")
        return try {
            authManager.logout()
            logger.completeOperation("logout")
            MeteoResult.success(Unit)
        } catch (e: Exception) {
            val error = MeteoError.fromException(e, "LOGOUT_FAILED")
            logger.failOperation("logout", error)
            MeteoResult.error(error)
        }
    }

    override suspend fun isLoggedIn(): Boolean {
        return authManager.isLoggedIn()
    }

    override suspend fun getCurrentUser(): MeteoResult<UserInfo> {
        logger.startOperation("getCurrentUser")

        return retryPolicy.executeWithErrorHandling {
            val authHeader = authManager.getAuthorizationHeader()
                ?: throw SecurityException("No valid authentication token")

            val response = meteoApiService.getCurrentUser(authHeader)

            if (response.isSuccessful && response.body()?.success == true) {
                val userInfo = response.body()?.data
                if (userInfo != null) {
                    logger.completeOperation("getCurrentUser", userInfo.username)
                    userInfo
                } else {
                    throw IllegalStateException("User info response data is null")
                }
            } else {
                throw IllegalStateException("Get current user failed: ${response.code()}")
            }
        }
    }

    override suspend fun forceLogout(): MeteoResult<Unit> {
        logger.startOperation("forceLogout")
        return try {
            authManager.forceLogout()
            logger.completeOperation("forceLogout")
            MeteoResult.success(Unit)
        } catch (e: Exception) {
            val error = MeteoError.fromException(e, "FORCE_LOGOUT_FAILED")
            logger.failOperation("forceLogout", error)
            MeteoResult.error(error)
        }
    }

    override suspend fun debugJwtToken(): String {
        return authManager.debugJwtToken()
    }
}