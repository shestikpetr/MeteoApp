package com.shestikpetr.meteoapp.presentation.auth

import android.util.Patterns

/**
 * Чистые функции-валидаторы форм авторизации. Без зависимости от ViewModel,
 * чтобы можно было использовать в Composable для подсказок.
 */
object AuthFormValidator {
    private const val MIN_PASSWORD_LENGTH = 6

    fun isEmailValid(email: String): Boolean =
        email.isEmpty() || Patterns.EMAIL_ADDRESS.matcher(email).matches()

    fun isPasswordValid(password: String): Boolean =
        password.length >= MIN_PASSWORD_LENGTH

    fun isLoginFormValid(username: String, password: String): Boolean =
        username.isNotBlank() && isPasswordValid(password)

    fun isRegisterFormValid(username: String, email: String, password: String): Boolean =
        username.isNotBlank() && email.isNotBlank() && isEmailValid(email) && isPasswordValid(password)
}
