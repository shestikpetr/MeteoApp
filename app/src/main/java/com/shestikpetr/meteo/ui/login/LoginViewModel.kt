package com.shestikpetr.meteo.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shestikpetr.meteo.common.error.MeteoResult
import com.shestikpetr.meteo.common.error.MeteoError
import com.shestikpetr.meteo.common.logging.MeteoLogger
import com.shestikpetr.meteo.network.AuthRepository
import com.shestikpetr.meteo.network.AuthTokens
import com.shestikpetr.meteo.network.UserInfo
import com.shestikpetr.meteo.localization.interfaces.LocalizationService
import com.shestikpetr.meteo.localization.interfaces.StringKey
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for login and registration operations.
 * Handles authentication state management with unified error handling.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val localizationService: LocalizationService
) : ViewModel() {

    private val logger = MeteoLogger.forClass(LoginViewModel::class)

    private val _loginState = MutableStateFlow<MeteoResult<AuthTokens>?>(null)
    val loginState: StateFlow<MeteoResult<AuthTokens>?> = _loginState

    /**
     * Authenticates user with username and password.
     */
    fun login(username: String, password: String) {
        viewModelScope.launch {
            logger.startOperation("login", "username" to username)

            // Show loading state
            _loginState.value = MeteoResult.loading()

            val result = authRepository.login(username, password)
            _loginState.value = result

            result.onSuccess { tokens ->
                logger.completeOperation("login", "user_id=${tokens.user_id}")
            }.onError { error ->
                logger.failOperation("login", error)
            }
        }
    }

    /**
     * Registers new user account.
     */
    fun register(username: String, password: String, email: String) {
        viewModelScope.launch {
            logger.startOperation("register", "username" to username, "email" to email)

            // Show loading state
            _loginState.value = MeteoResult.loading()

            val result = authRepository.register(username, password, email)
            _loginState.value = result

            result.onSuccess { tokens ->
                logger.completeOperation("register", "user_id=${tokens.user_id}")
            }.onError { error ->
                logger.failOperation("register", error)
            }
        }
    }

    /**
     * Checks if user is currently logged in.
     */
    fun checkLoggedIn(callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            val isLoggedIn = authRepository.isLoggedIn()
            callback(isLoggedIn)
        }
    }

    /**
     * Logs out current user.
     */
    fun logout() {
        viewModelScope.launch {
            logger.startOperation("logout")
            val result = authRepository.logout()

            result.onSuccess {
                _loginState.value = null
                logger.completeOperation("logout")
            }.onError { error ->
                logger.failOperation("logout", error)
                // Even if logout fails, clear the UI state
                _loginState.value = null
            }
        }
    }

    /**
     * Gets current user information.
     */
    fun getCurrentUser(callback: (MeteoResult<UserInfo>) -> Unit) {
        viewModelScope.launch {
            logger.startOperation("getCurrentUser")
            val result = authRepository.getCurrentUser()

            result.onSuccess { user ->
                logger.completeOperation("getCurrentUser", user.username)
            }.onError { error ->
                logger.failOperation("getCurrentUser", error)
            }

            callback(result)
        }
    }

    /**
     * Forces logout and clears all authentication data.
     * Used for handling authentication errors.
     */
    fun forceLogout() {
        viewModelScope.launch {
            logger.startOperation("forceLogout")
            val result = authRepository.forceLogout()

            result.onSuccess {
                _loginState.value = null
                logger.completeOperation("forceLogout")
            }.onError { error ->
                logger.failOperation("forceLogout", error)
                // Even if force logout fails, clear the UI state
                _loginState.value = null
            }
        }
    }

    /**
     * Gets JWT token debug information.
     */
    fun debugJwtToken(callback: (String) -> Unit) {
        viewModelScope.launch {
            val debugInfo = authRepository.debugJwtToken()
            callback(debugInfo)
        }
    }

    /**
     * Gets user-friendly error message from MeteoError.
     */
    fun getErrorMessage(error: MeteoError): String {
        return when (error) {
            is MeteoError.Auth.InvalidCredentials -> localizationService.getString(StringKey.LoginErrorInvalidCredentials)
            is MeteoError.Auth.UserAlreadyExists -> localizationService.getString(StringKey.RegisterErrorUserExists)
            is MeteoError.Network.NoConnection -> localizationService.getString(StringKey.ErrorUnknown)
            is MeteoError.Network.Timeout -> localizationService.getString(StringKey.ErrorUnknown)
            else -> error.getUserMessage()
        }
    }
}