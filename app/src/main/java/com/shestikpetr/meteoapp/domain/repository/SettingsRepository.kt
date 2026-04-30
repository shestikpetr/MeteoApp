package com.shestikpetr.meteoapp.domain.repository

import com.shestikpetr.meteoapp.domain.model.AppSettings
import com.shestikpetr.meteoapp.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

/**
 * Управление пользовательскими настройками отображения.
 */
interface SettingsRepository {

    val settings: Flow<AppSettings>

    suspend fun setThemeMode(mode: ThemeMode)

    suspend fun setTooltipsEnabled(enabled: Boolean)

    suspend fun toggleStationHidden(stationNumber: String)

    suspend fun toggleParameterHidden(parameterCode: Int)
}
