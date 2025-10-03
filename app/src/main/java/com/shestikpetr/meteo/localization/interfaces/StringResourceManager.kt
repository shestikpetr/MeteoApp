package com.shestikpetr.meteo.localization.interfaces

/**
 * Main interface for string resource management following Interface Segregation Principle
 * Provides a unified API for accessing localized strings across the application
 */
interface StringResourceManager {
    suspend fun getString(key: StringKey): String
    suspend fun getString(key: StringKey, vararg args: Any): String
    suspend fun getCurrentLocale(): String
    suspend fun setLocale(locale: String)
    suspend fun isLocaleSupported(locale: String): Boolean
    fun getStringSync(key: StringKey): String
    fun getStringSync(key: StringKey, vararg args: Any): String
}

/**
 * Type-safe string keys to prevent typos and enable IDE support
 */
sealed class StringKey(val key: String) {
    // Login Screen Keys
    object AppTitle : StringKey("app_title")
    object AppSubtitle : StringKey("app_subtitle")
    object LoginMode : StringKey("login_mode")
    object RegisterMode : StringKey("register_mode")
    object Username : StringKey("username")
    object Password : StringKey("password")
    object ConfirmPassword : StringKey("confirm_password")
    object Email : StringKey("email")
    object LoginButton : StringKey("login_button")
    object RegisterButton : StringKey("register_button")
    object DemoMode : StringKey("demo_mode")
    object DemoModeDescription : StringKey("demo_mode_description")
    object DemoModeButton : StringKey("demo_mode_button")
    object ShowPassword : StringKey("show_password")
    object HidePassword : StringKey("hide_password")
    object PasswordMismatch : StringKey("password_mismatch")

    // Login Error Messages
    object LoginErrorInvalidCredentials : StringKey("login_error_invalid_credentials")
    object RegisterErrorUserExists : StringKey("register_error_user_exists")
    object ErrorUnknown : StringKey("error_unknown")
    object ErrorRegistration : StringKey("error_registration")
    object ErrorNoParameterSelected : StringKey("error_no_parameter_selected")

    // Chart Screen Keys
    object WeatherStationTitle : StringKey("weather_station_title")
    object ChartSettings : StringKey("chart_settings")
    object SelectDataType : StringKey("select_data_type")
    object SelectPeriod : StringKey("select_period")
    object BuildChart : StringKey("build_chart")
    object BuildChartDescription : StringKey("build_chart_description")
    object LoadingData : StringKey("loading_data")
    object ChartTitle : StringKey("chart_title")
    object NoDataAvailable : StringKey("no_data_available")
    object DataSummary : StringKey("data_summary")
    object Period : StringKey("period")
    object TotalPoints : StringKey("total_points")

    // Station Management Keys
    object StationManagement : StringKey("station_management")
    object Back : StringKey("back")
    object AddStation : StringKey("add_station")
    object MyStations : StringKey("my_stations")
    object NoStationsAdded : StringKey("no_stations_added")
    object StationNumber : StringKey("station_number")
    object StationNumberPlaceholder : StringKey("station_number_placeholder")
    object StationNumberHelp : StringKey("station_number_help")
    object StationNameOptional : StringKey("station_name_optional")
    object StationNamePlaceholder : StringKey("station_name_placeholder")
    object AddStationButton : StringKey("add_station_button")
    object AddToFavorites : StringKey("add_to_favorites")
    object RemoveFromFavorites : StringKey("remove_from_favorites")
    object EditStation : StringKey("edit_station")
    object DeleteStation : StringKey("delete_station")
    object DeleteStationConfirmTitle : StringKey("delete_station_confirm_title")
    object DeleteStationConfirmMessage : StringKey("delete_station_confirm_message")
    object Delete : StringKey("delete")
    object Cancel : StringKey("cancel")
    object EditStationTitle : StringKey("edit_station_title")
    object StationName : StringKey("station_name")
    object Save : StringKey("save")
    object Parameters : StringKey("parameters")

    // Map Screen Keys
    object RetryLoad : StringKey("retry_load")
    object NoStationsAvailable : StringKey("no_stations_available")
    object ParametersNotAvailable : StringKey("parameters_not_available")
    object EightDigitNumber : StringKey("eight_digit_number")
    object UserCustomName : StringKey("user_custom_name")
    object MyStationPlaceholder : StringKey("my_station_placeholder")
    object Add : StringKey("add")

    // Common Keys
    object Loading : StringKey("loading")
    object Error : StringKey("error")
    object Retry : StringKey("retry")
    object Ok : StringKey("ok")
    object Yes : StringKey("yes")
    object No : StringKey("no")
    object Close : StringKey("close")
    object Language : StringKey("language")
    object Settings : StringKey("settings")
}