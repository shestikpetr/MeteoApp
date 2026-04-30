package com.shestikpetr.meteoapp.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shestikpetr.meteoapp.domain.usecase.auth.LoginUseCase
import com.shestikpetr.meteoapp.domain.usecase.auth.RegisterUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Что отображает экран авторизации. */
data class AuthUiState(
    val mode: Mode = Mode.LOGIN,
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    enum class Mode { LOGIN, REGISTER }
}

/** Однократные события — переход на главный экран после успеха. */
sealed interface AuthEffect {
    data object NavigateToMain : AuthEffect
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val login: LoginUseCase,
    private val register: RegisterUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    private val _effects = Channel<AuthEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun setMode(mode: AuthUiState.Mode) = _state.update {
        it.copy(mode = mode, errorMessage = null)
    }

    fun setUsername(value: String) = _state.update { it.copy(username = value, errorMessage = null) }
    fun setEmail(value: String) = _state.update { it.copy(email = value, errorMessage = null) }
    fun setPassword(value: String) = _state.update { it.copy(password = value, errorMessage = null) }
    fun togglePasswordVisible() = _state.update { it.copy(passwordVisible = !it.passwordVisible) }

    /**
     * Не блокирует submit — это про дисабл-кнопку: вызывающая сторона должна сама проверить.
     */
    fun isFormValid(): Boolean {
        val s = _state.value
        return when (s.mode) {
            AuthUiState.Mode.LOGIN ->
                s.username.length in MIN_USERNAME..MAX_USERNAME && s.password.length >= MIN_PASSWORD
            AuthUiState.Mode.REGISTER ->
                s.username.length in MIN_USERNAME..MAX_USERNAME &&
                        AuthFormValidator.isEmailValid(s.email) &&
                        s.password.length >= MIN_PASSWORD
        }
    }

    fun submit() {
        val current = _state.value
        if (current.isLoading) return
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val result = when (current.mode) {
                AuthUiState.Mode.LOGIN -> login(current.username.trim(), current.password)
                AuthUiState.Mode.REGISTER -> register(
                    current.username.trim(),
                    current.email.trim(),
                    current.password
                )
            }
            result.fold(
                onSuccess = {
                    _state.update { it.copy(isLoading = false) }
                    _effects.send(AuthEffect.NavigateToMain)
                },
                onFailure = { e ->
                    _state.update { it.copy(isLoading = false, errorMessage = e.message ?: "Ошибка") }
                }
            )
        }
    }

    private companion object {
        const val MIN_USERNAME = 3
        const val MAX_USERNAME = 50
        const val MIN_PASSWORD = 6
    }
}
