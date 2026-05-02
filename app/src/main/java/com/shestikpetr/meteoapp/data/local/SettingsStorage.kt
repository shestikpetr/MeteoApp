package com.shestikpetr.meteoapp.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.shestikpetr.meteoapp.domain.model.AppSettings
import com.shestikpetr.meteoapp.domain.model.ThemeMode
import com.shestikpetr.meteoapp.util.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface SettingsStorage {
    val settings: Flow<AppSettings>

    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun toggleStationHidden(stationNumber: String)
    suspend fun toggleParameterHidden(parameterCode: Int)
}

class SettingsStorageDataStore(private val context: Context) : SettingsStorage {

    override val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            themeMode = prefs[THEME_MODE_KEY]
                ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            hiddenStations = prefs[HIDDEN_STATIONS_KEY] ?: emptySet(),
            hiddenParameters = prefs[HIDDEN_PARAMETERS_KEY]
                ?.mapNotNull { it.toIntOrNull() }
                ?.toSet()
                ?: emptySet()
        )
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[THEME_MODE_KEY] = mode.name }
    }

    override suspend fun toggleStationHidden(stationNumber: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[HIDDEN_STATIONS_KEY] ?: emptySet()
            prefs[HIDDEN_STATIONS_KEY] = if (stationNumber in current) {
                current - stationNumber
            } else {
                current + stationNumber
            }
        }
    }

    override suspend fun toggleParameterHidden(parameterCode: Int) {
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

    private companion object {
        val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        val HIDDEN_STATIONS_KEY = stringSetPreferencesKey("hidden_stations")
        val HIDDEN_PARAMETERS_KEY = stringSetPreferencesKey("hidden_parameters")
    }
}
