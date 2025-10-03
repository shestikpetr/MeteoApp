package com.shestikpetr.meteo.localization.embedded

import com.shestikpetr.meteo.localization.interfaces.EmbeddedStringProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation providing embedded fallback strings
 * These strings are used when network is unavailable or API fails
 * Ensures the app always has working strings
 */
@Singleton
class EmbeddedStringProviderImpl @Inject constructor() : EmbeddedStringProvider {

    private val russianStrings = mapOf(
        // Login Screen
        "app_title" to "Метео",
        "app_subtitle" to "Метеорологическая система",
        "login_mode" to "Вход",
        "register_mode" to "Регистрация",
        "username" to "Имя пользователя",
        "password" to "Пароль",
        "confirm_password" to "Подтвердите пароль",
        "email" to "Email",
        "login_button" to "Войти",
        "register_button" to "Зарегистрироваться",
        "demo_mode" to "Демо режим",
        "demo_mode_description" to "Попробуйте приложение без регистрации",
        "demo_mode_button" to "Войти в демо режим",
        "show_password" to "Показать пароль",
        "hide_password" to "Скрыть пароль",
        "password_mismatch" to "Пароли не совпадают",

        // Login Errors
        "login_error_invalid_credentials" to "Неверные учетные данные. Проверьте логин и пароль.",
        "register_error_user_exists" to "Ошибка регистрации. Пользователь уже существует или данные некорректны.",

        // Chart Screen
        "weather_station_title" to "Метеостанция",
        "chart_settings" to "Настройки графика",
        "select_data_type" to "Выберите тип данных",
        "select_period" to "Выберите период",
        "build_chart" to "Построить график",
        "build_chart_description" to "Построить график",
        "loading_data" to "Загрузка данных...",
        "chart_title" to "График {0}",
        "no_data_available" to "Данные для построения графика отсутствуют",
        "data_summary" to "Сводка данных:",
        "period" to "Период: {0} - {1}",
        "total_points" to "Всего точек: {0}",

        // Station Management
        "station_management" to "Управление станциями",
        "back" to "Назад",
        "add_station" to "Добавить станцию",
        "my_stations" to "Мои станции",
        "no_stations_added" to "У вас пока нет добавленных станций",
        "station_number" to "Номер станции (8 цифр)",
        "station_number_placeholder" to "12345678",
        "station_number_help" to "Введите 8-значный номер станции",
        "station_name_optional" to "Название (необязательно)",
        "station_name_placeholder" to "Моя станция",
        "add_station_button" to "Добавить станцию",
        "add_to_favorites" to "Добавить в избранное",
        "remove_from_favorites" to "Убрать из избранного",
        "edit_station" to "Редактировать",
        "delete_station" to "Удалить",
        "delete_station_confirm_title" to "Удалить станцию?",
        "delete_station_confirm_message" to "Вы уверены, что хотите удалить станцию \"{0}\"?",
        "delete" to "Удалить",
        "cancel" to "Отмена",
        "edit_station_title" to "Изменить название",
        "station_name" to "Название станции",
        "save" to "Сохранить",
        "parameters" to "Параметры: {0}",

        // Map Screen
        "retry_load" to "Повторить загрузку",
        "no_stations_available" to "Нет доступных метеостанций",
        "parameters_not_available" to "Параметры не доступны",
        "eight_digit_number" to "8-значный номер",
        "user_custom_name" to "Пользовательское название",
        "my_station_placeholder" to "Моя станция",
        "add" to "Добавить",

        // Common
        "loading" to "Загрузка",
        "error" to "Ошибка",
        "retry" to "Повторить",
        "ok" to "ОК",
        "yes" to "Да",
        "no" to "Нет",
        "close" to "Закрыть",
        "language" to "Язык",
        "settings" to "Настройки"
    )

    private val englishStrings = mapOf(
        // Login Screen
        "app_title" to "Meteo",
        "app_subtitle" to "Meteorological System",
        "login_mode" to "Login",
        "register_mode" to "Register",
        "username" to "Username",
        "password" to "Password",
        "confirm_password" to "Confirm Password",
        "email" to "Email",
        "login_button" to "Login",
        "register_button" to "Register",
        "demo_mode" to "Demo Mode",
        "demo_mode_description" to "Try the app without registration",
        "demo_mode_button" to "Enter Demo Mode",
        "show_password" to "Show Password",
        "hide_password" to "Hide Password",
        "password_mismatch" to "Passwords do not match",

        // Login Errors
        "login_error_invalid_credentials" to "Invalid credentials. Please check your username and password.",
        "register_error_user_exists" to "Registration error. User already exists or data is invalid.",

        // Chart Screen
        "weather_station_title" to "Weather Station",
        "chart_settings" to "Chart Settings",
        "select_data_type" to "Select Data Type",
        "select_period" to "Select Period",
        "build_chart" to "Build Chart",
        "build_chart_description" to "Build Chart",
        "loading_data" to "Loading data...",
        "chart_title" to "Chart of {0}",
        "no_data_available" to "No data available for chart",
        "data_summary" to "Data Summary:",
        "period" to "Period: {0} - {1}",
        "total_points" to "Total points: {0}",

        // Station Management
        "station_management" to "Station Management",
        "back" to "Back",
        "add_station" to "Add Station",
        "my_stations" to "My Stations",
        "no_stations_added" to "You haven't added any stations yet",
        "station_number" to "Station Number (8 digits)",
        "station_number_placeholder" to "12345678",
        "station_number_help" to "Enter 8-digit station number",
        "station_name_optional" to "Name (optional)",
        "station_name_placeholder" to "My Station",
        "add_station_button" to "Add Station",
        "add_to_favorites" to "Add to Favorites",
        "remove_from_favorites" to "Remove from Favorites",
        "edit_station" to "Edit",
        "delete_station" to "Delete",
        "delete_station_confirm_title" to "Delete Station?",
        "delete_station_confirm_message" to "Are you sure you want to delete station \"{0}\"?",
        "delete" to "Delete",
        "cancel" to "Cancel",
        "edit_station_title" to "Edit Name",
        "station_name" to "Station Name",
        "save" to "Save",
        "parameters" to "Parameters: {0}",

        // Map Screen
        "retry_load" to "Retry Loading",
        "no_stations_available" to "No weather stations available",
        "parameters_not_available" to "Parameters not available",
        "eight_digit_number" to "8-digit number",
        "user_custom_name" to "Custom name",
        "my_station_placeholder" to "My Station",
        "add" to "Add",

        // Common
        "loading" to "Loading",
        "error" to "Error",
        "retry" to "Retry",
        "ok" to "OK",
        "yes" to "Yes",
        "no" to "No",
        "close" to "Close",
        "language" to "Language",
        "settings" to "Settings"
    )

    private val supportedLocales = listOf("ru", "en")

    override fun getEmbeddedStrings(locale: String): Map<String, String> {
        return when (locale) {
            "ru" -> russianStrings
            "en" -> englishStrings
            else -> russianStrings // Default fallback
        }
    }

    override fun getSupportedLocales(): List<String> {
        return supportedLocales
    }

    override fun getDefaultLocale(): String {
        return "ru"
    }
}