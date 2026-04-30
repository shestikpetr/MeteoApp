package com.shestikpetr.meteoapp.domain.model

/** Профиль пользователя, как его видит UI. */
data class User(
    val username: String,
    val email: String,
    val role: Role
) {
    enum class Role { USER, ADMIN }
}
