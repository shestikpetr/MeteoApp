package com.shestikpetr.meteoapp.data.model

import com.google.gson.annotations.SerializedName

data class UserLoginRequest(
    val username: String,
    val password: String
)

data class UserRegisterRequest(
    val username: String,
    val email: String,
    val password: String
)

data class AuthResponse(
    val success: Boolean = true,
    val data: AuthData
)

data class AuthData(
    @SerializedName("user_id") val userId: String,
    val username: String,
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String
)

data class UserMeResponse(
    val success: Boolean = true,
    val data: UserData
)

data class UserData(
    val id: String,
    val username: String,
    val email: String,
    val role: String,
    @SerializedName("is_active") val isActive: Boolean
)

data class RefreshTokenResponse(
    val success: Boolean = true,
    @SerializedName("access_token") val accessToken: String
)

data class ApiError(
    val detail: List<ValidationError>? = null
)

data class ValidationError(
    val loc: List<Any>,
    val msg: String,
    val type: String
)
