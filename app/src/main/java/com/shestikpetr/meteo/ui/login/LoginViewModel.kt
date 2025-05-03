package com.shestikpetr.meteo.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shestikpetr.meteo.network.AuthRepository
import com.shestikpetr.meteo.network.LoginResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _loginState = MutableStateFlow<LoginResult?>(null)
    val loginState: StateFlow<LoginResult?> = _loginState

    // Функция для аутентификации пользователя
    fun login(username: String, password: String) {
        viewModelScope.launch {
            try {
                val result = authRepository.login(username, password)
                _loginState.value = result
            } catch (e: Exception) {
                _loginState.value = LoginResult.Error(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    // Проверка, вошел ли пользователь в систему
    fun checkLoggedIn(): Boolean {
        return authRepository.isLoggedIn()
    }

    // Выход из системы
    fun logout() {
        authRepository.logout()
        _loginState.value = null
    }
}