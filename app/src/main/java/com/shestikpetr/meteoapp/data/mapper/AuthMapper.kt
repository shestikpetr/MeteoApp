package com.shestikpetr.meteoapp.data.mapper

import com.shestikpetr.meteoapp.data.remote.dto.AuthLoginDataDto
import com.shestikpetr.meteoapp.data.remote.dto.TokensDto
import com.shestikpetr.meteoapp.data.remote.dto.UserResponseDto
import com.shestikpetr.meteoapp.domain.model.AuthSession
import com.shestikpetr.meteoapp.domain.model.Tokens
import com.shestikpetr.meteoapp.domain.model.User

fun UserResponseDto.toDomain(): User = User(
    username = username,
    email = email,
    role = if (role.equals("admin", ignoreCase = true)) User.Role.ADMIN else User.Role.USER
)

fun TokensDto.toDomain(): Tokens = Tokens(
    accessToken = accessToken,
    refreshToken = refreshToken,
    expiresIn = expiresIn
)

fun AuthLoginDataDto.toDomain(): AuthSession = AuthSession(
    user = user.toDomain(),
    tokens = tokens.toDomain()
)
