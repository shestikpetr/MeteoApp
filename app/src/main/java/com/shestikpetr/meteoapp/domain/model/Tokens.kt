package com.shestikpetr.meteoapp.domain.model

/** Пара JWT-токенов, выданных сервером. */
data class Tokens(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long? = null
)

/** Результат успешной аутентификации: профиль + токены. */
data class AuthSession(
    val user: User,
    val tokens: Tokens
)
