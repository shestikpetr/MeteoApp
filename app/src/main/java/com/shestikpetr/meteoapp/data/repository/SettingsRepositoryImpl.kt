package com.shestikpetr.meteoapp.data.repository

import com.shestikpetr.meteoapp.data.local.SettingsStorage
import com.shestikpetr.meteoapp.domain.model.AppSettings
import com.shestikpetr.meteoapp.domain.model.ThemeMode
import com.shestikpetr.meteoapp.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val storage: SettingsStorage
) : SettingsRepository {

    override val settings: Flow<AppSettings> = storage.settings

    override suspend fun setThemeMode(mode: ThemeMode) = storage.setThemeMode(mode)
    override suspend fun setTooltipsEnabled(enabled: Boolean) = storage.setTooltipsEnabled(enabled)
    override suspend fun toggleStationHidden(stationNumber: String) = storage.toggleStationHidden(stationNumber)
    override suspend fun toggleParameterHidden(parameterCode: Int) = storage.toggleParameterHidden(parameterCode)
}
