package com.shestikpetr.meteoapp.util

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

enum class ThemeMode(val label: String) {
    SYSTEM("Системная"),
    LIGHT("Светлая"),
    DARK("Тёмная")
}

class SettingsManager(private val context: Context) {

    companion object {
        private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        private val TOOLTIPS_ENABLED_KEY = booleanPreferencesKey("tooltips_enabled")
        private val HIDDEN_STATIONS_KEY = stringSetPreferencesKey("hidden_stations")
        private val HIDDEN_PARAMETERS_KEY = stringSetPreferencesKey("hidden_parameters")
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        prefs[THEME_MODE_KEY]?.let { ThemeMode.valueOf(it) } ?: ThemeMode.SYSTEM
    }

    val tooltipsEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[TOOLTIPS_ENABLED_KEY] ?: true
    }

    val hiddenStations: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[HIDDEN_STATIONS_KEY] ?: emptySet()
    }

    val hiddenParameters: Flow<Set<Int>> = context.dataStore.data.map { prefs ->
        prefs[HIDDEN_PARAMETERS_KEY]
            ?.mapNotNull { it.toIntOrNull() }
            ?.toSet()
            ?: emptySet()
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[THEME_MODE_KEY] = mode.name
        }
    }

    suspend fun setTooltipsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[TOOLTIPS_ENABLED_KEY] = enabled
        }
    }

    suspend fun toggleStationHidden(stationNumber: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[HIDDEN_STATIONS_KEY] ?: emptySet()
            prefs[HIDDEN_STATIONS_KEY] = if (stationNumber in current) {
                current - stationNumber
            } else {
                current + stationNumber
            }
        }
    }

    suspend fun toggleParameterHidden(parameterCode: Int) {
        context.dataStore.edit { prefs ->
            val key = parameterCode.toString()
            val current = prefs[HIDDEN_PARAMETERS_KEY] ?: emptySet()
            prefs[HIDDEN_PARAMETERS_KEY] = if (key in current) {
                current - key
            } else {
                current + key
            }
        }
    }

    suspend fun getHiddenStations(): Set<String> = hiddenStations.first()
    suspend fun getHiddenParameters(): Set<Int> = hiddenParameters.first()
}
