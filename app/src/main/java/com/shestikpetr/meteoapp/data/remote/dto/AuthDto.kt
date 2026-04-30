package com.shestikpetr.meteoapp.data.remote.dto

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

// --- Server payloads ---

data class UserResponseDto(
    val username: String,
    val email: String,
    val role: String
)

data class TokensDto(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long? = null
)

data class AuthLoginDataDto(
    val user: UserResponseDto,
    val tokens: TokensDto
)

data class RefreshTokenDataDto(
    val accessToken: String
)

// --- Envelopes ---

data class ApiResponseAuthLoginData(
    val success: Boolean = false,
    val data: AuthLoginDataDto? = null,
    val error: String? = null
)

data class ApiResponseRefreshTokenData(
    val success: Boolean = false,
    val data: RefreshTokenDataDto? = null,
    val error: String? = null
)

data class ApiResponseUserResponse(
    val success: Boolean = false,
    val data: UserResponseDto? = null,
    val error: String? = null
)

data class ApiResponseUnit(
    val success: Boolean = false,
    val error: String? = null
)

data class ApiResponseMapStringString(
    val success: Boolean = false,
    val data: Map<String, String>? = null,
    val error: String? = null
)
