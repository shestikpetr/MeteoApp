package com.shestikpetr.meteoapp.domain.repository

import com.shestikpetr.meteoapp.domain.model.AuthSession
import com.shestikpetr.meteoapp.domain.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Аутентификация и управление профилем.
 * Реализация скрывает работу с токенами и /me-эндпоинтом.
 */
interface AuthRepository {

    /** Текущий зарегистрированный username, если пользователь залогинен. */
    val username: Flow<String?>

    /** Текущий email пользователя, если известен. */
    val email: Flow<String?>

    suspend fun isLoggedIn(): Boolean

    suspend fun login(username: String, password: String): Result<AuthSession>

    suspend fun register(username: String, email: String, password: String): Result<AuthSession>

    suspend fun logout()

    suspend fun me(): Result<User>

    suspend fun updateProfile(username: String? = null, email: String? = null): Result<User>

    suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit>
}
