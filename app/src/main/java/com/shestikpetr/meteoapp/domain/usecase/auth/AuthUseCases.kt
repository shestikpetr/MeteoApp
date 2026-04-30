package com.shestikpetr.meteoapp.domain.usecase.auth

import com.shestikpetr.meteoapp.domain.model.AuthSession
import com.shestikpetr.meteoapp.domain.model.User
import com.shestikpetr.meteoapp.domain.repository.AuthRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val repo: AuthRepository
) {
    suspend operator fun invoke(username: String, password: String): Result<AuthSession> =
        repo.login(username, password)
}

class RegisterUseCase @Inject constructor(
    private val repo: AuthRepository
) {
    suspend operator fun invoke(username: String, email: String, password: String): Result<AuthSession> =
        repo.register(username, email, password)
}

class LogoutUseCase @Inject constructor(
    private val repo: AuthRepository
) {
    suspend operator fun invoke() = repo.logout()
}

class IsLoggedInUseCase @Inject constructor(
    private val repo: AuthRepository
) {
    suspend operator fun invoke(): Boolean = repo.isLoggedIn()
}

class GetMeUseCase @Inject constructor(
    private val repo: AuthRepository
) {
    suspend operator fun invoke(): Result<User> = repo.me()
}

class UpdateProfileUseCase @Inject constructor(
    private val repo: AuthRepository
) {
    suspend operator fun invoke(username: String? = null, email: String? = null): Result<User> =
        repo.updateProfile(username, email)
}

class ChangePasswordUseCase @Inject constructor(
    private val repo: AuthRepository
) {
    suspend operator fun invoke(currentPassword: String, newPassword: String): Result<Unit> =
        repo.changePassword(currentPassword, newPassword)
}
