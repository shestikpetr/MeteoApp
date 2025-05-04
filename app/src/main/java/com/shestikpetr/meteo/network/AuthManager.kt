package com.shestikpetr.meteo.network

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.content.edit

@Singleton
class AuthManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("MeteoPrefs", Context.MODE_PRIVATE)
    }

    companion object {
        private const val KEY_USERNAME = "username"
        private const val KEY_PASSWORD = "password"
    }

    // Сохранение учетных данных
    fun saveCredentials(username: String, password: String) {
        prefs.edit {
            putString(KEY_USERNAME, username)
                .putString(KEY_PASSWORD, password)
        }
    }

    // Получение токена авторизации в формате Base64
    fun getAuthToken(): String {
        val username = prefs.getString(KEY_USERNAME, "") ?: ""
        val password = prefs.getString(KEY_PASSWORD, "") ?: ""

        val credentials = "$username:$password"
        return Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)
    }

    fun getAuthTokenDebug(): String {
        val token = getAuthToken()
        Log.d("AuthManager", "Текущий токен: ${token.take(10)}...")
        return token
    }

    // Проверка наличия сохраненных учетных данных
    fun hasCredentials(): Boolean {
        val username = prefs.getString(KEY_USERNAME, "")
        val password = prefs.getString(KEY_PASSWORD, "")
        return !username.isNullOrEmpty() && !password.isNullOrEmpty()
    }

    // Очистка учетных данных
    fun clearCredentials() {
        prefs.edit {
            remove(KEY_USERNAME)
                .remove(KEY_PASSWORD)
        }
    }
}