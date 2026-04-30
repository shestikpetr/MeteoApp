package com.shestikpetr.meteoapp.data.repository

import com.shestikpetr.meteoapp.data.local.SessionStorage
import com.shestikpetr.meteoapp.data.local.TokenStorage
import com.shestikpetr.meteoapp.data.mapper.toDomain
import com.shestikpetr.meteoapp.data.remote.api.MeteoApi
import com.shestikpetr.meteoapp.data.remote.dto.ApiResponseAuthLoginData
import com.shestikpetr.meteoapp.data.remote.dto.ChangePasswordRequest
import com.shestikpetr.meteoapp.data.remote.dto.UpdateMeRequest
import com.shestikpetr.meteoapp.data.remote.dto.UserLoginRequest
import com.shestikpetr.meteoapp.data.remote.dto.UserRegisterRequest
import com.shestikpetr.meteoapp.domain.model.AuthSession
import com.shestikpetr.meteoapp.domain.model.User
import com.shestikpetr.meteoapp.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val api: MeteoApi,
    private val tokenStorage: TokenStorage,
    private val sessionStorage: SessionStorage
) : AuthRepository {

    override val username: Flow<String?> = sessionStorage.username
    override val email: Flow<String?> = sessionStorage.email

    override suspend fun isLoggedIn(): Boolean = tokenStorage.isLoggedIn()

    override suspend fun login(username: String, password: String): Result<AuthSession> = runCatching {
        consumeAuthResponse(api.login(UserLoginRequest(username, password)))
    }

    override suspend fun register(username: String, email: String, password: String): Result<AuthSession> =
        runCatching {
            consumeAuthResponse(api.register(UserRegisterRequest(username, email, password)))
        }

    override suspend fun logout() {
        // Сервер для logout JWT-stateless ничего не делает, но всё равно дёрнем эндпоинт
        runCatching { api.logout() }
        tokenStorage.clear()
        sessionStorage.clear()
    }

    override suspend fun me(): Result<User> = runCatching {
        val response = api.me()
        val body = response.body()
        if (!response.isSuccessful || body == null || !body.success || body.data == null) {
            error(body?.error ?: response.errorBody()?.string() ?: "Failed to fetch profile")
        }
        body.data.toDomain()
    }

    override suspend fun updateProfile(username: String?, email: String?): Result<User> = runCatching {
        val response = api.updateMe(UpdateMeRequest(username, email))
        val body = response.body()
        if (!response.isSuccessful || body == null || !body.success || body.data == null) {
            error(body?.error ?: response.errorBody()?.string() ?: "Failed to update profile")
        }
        val user = body.data.toDomain()
        sessionStorage.save(user.username, user.email)
        user
    }

    override suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> =
        runCatching {
            val response = api.changePassword(ChangePasswordRequest(currentPassword, newPassword))
            val body = response.body()
            if (!response.isSuccessful || body == null || !body.success) {
                error(body?.error ?: response.errorBody()?.string() ?: "Failed to change password")
            }
        }

    private suspend fun consumeAuthResponse(response: Response<ApiResponseAuthLoginData>): AuthSession {
        val body = response.body()
        if (!response.isSuccessful || body == null || !body.success || body.data == null) {
            error(body?.error ?: response.errorBody()?.string() ?: "Authentication failed")
        }
        val session = body.data.toDomain()
        tokenStorage.saveTokens(session.tokens.accessToken, session.tokens.refreshToken)
        sessionStorage.save(session.user.username, session.user.email)
        return session
    }
}
