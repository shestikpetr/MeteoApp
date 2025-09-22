package com.shestikpetr.meteo.ui.login

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shestikpetr.meteo.network.AuthRepository
import com.shestikpetr.meteo.network.LoginResult
import com.shestikpetr.meteo.network.UserInfo
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
                _loginState.value = LoginResult.Loading

                val result = authRepository.login(username, password)
                _loginState.value = result

                if (result is LoginResult.Success) {
                    Log.d("LoginViewModel", "Авторизация успешна для user_id: ${result.userId}")
                }
            } catch (e: Exception) {
                Log.e("LoginViewModel", "Ошибка авторизации: ${e.message}", e)
                _loginState.value = LoginResult.Error(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    // Функция для регистрации пользователя
    fun register(username: String, password: String, email: String) {
        viewModelScope.launch {
            try {
                // Отображаем процесс загрузки
                _loginState.value = LoginResult.Loading

                val result = authRepository.register(username, password, email)
                _loginState.value = result

                if (result is LoginResult.Success) {
                    Log.d("LoginViewModel", "Регистрация успешна для user_id: ${result.userId}")
                }
            } catch (e: Exception) {
                Log.e("LoginViewModel", "Ошибка регистрации: ${e.message}", e)
                _loginState.value = LoginResult.Error(e.message ?: "Ошибка регистрации")
            }
        }
    }

    // Проверка, вошел ли пользователь в систему
    fun checkLoggedIn(callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            val isLoggedIn = authRepository.isLoggedIn()
            callback(isLoggedIn)
        }
    }

    // Выход из системы
    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _loginState.value = null
        }
    }

    // Получение информации о текущем пользователе
    fun getCurrentUser(callback: (UserInfo?) -> Unit) {
        viewModelScope.launch {
            val user = authRepository.getCurrentUser()
            callback(user)
        }
    }
}