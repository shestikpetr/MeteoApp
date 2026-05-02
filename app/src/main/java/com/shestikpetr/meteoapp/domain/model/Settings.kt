package com.shestikpetr.meteoapp.domain.model

enum class ThemeMode(val label: String) {
    SYSTEM("Системная"),
    LIGHT("Светлая"),
    DARK("Тёмная")
}

/** Пользовательские UI-настройки приложения. */
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val hiddenStations: Set<String> = emptySet(),
    val hiddenParameters: Set<Int> = emptySet()
)
