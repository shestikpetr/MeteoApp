package com.shestikpetr.meteoapp.data.model

// --- Requests ---

data class UserLoginRequest(
    val username: String,
    val password: String
)

data class UserRegisterRequest(
    val username: String,
    val email: String,
    val password: String
)

data class RefreshTokenRequest(
    val refreshToken: String
)

data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)

data class UpdateMeRequest(
    val username: String? = null,
    val email: String? = null
)

// --- Domain DTOs ---

data class UserResponse(
    val username: String,
    val email: String,
    val role: String
)

data class Tokens(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long? = null
)

data class AuthLoginData(
    val user: UserResponse,
    val tokens: Tokens
)

data class RefreshTokenData(
    val accessToken: String
)

// --- API wrappers ---

data class ApiResponseAuthLoginData(
    val success: Boolean = false,
    val data: AuthLoginData? = null,
    val error: String? = null
)

data class ApiResponseRefreshTokenData(
    val success: Boolean = false,
    val data: RefreshTokenData? = null,
    val error: String? = null
)

data class ApiResponseUserResponse(
    val success: Boolean = false,
    val data: UserResponse? = null,
    val error: String? = null
)
