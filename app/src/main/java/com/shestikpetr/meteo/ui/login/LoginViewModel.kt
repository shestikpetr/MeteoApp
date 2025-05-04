package com.shestikpetr.meteo.ui.login

import android.util.Log
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
                // Отображаем процесс загрузки
                _loginState.value = null

                val result = authRepository.login(username, password)
                _loginState.value = result

                if (result is LoginResult.Success) {
                    Log.d("LoginViewModel", "Авторизация успешна")
                }
            } catch (e: Exception) {
                Log.e("LoginViewModel", "Ошибка авторизации: ${e.message}", e)
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