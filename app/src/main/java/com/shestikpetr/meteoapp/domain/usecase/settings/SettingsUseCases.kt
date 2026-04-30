package com.shestikpetr.meteoapp.domain.usecase.settings

import com.shestikpetr.meteoapp.domain.model.AppSettings
import com.shestikpetr.meteoapp.domain.model.ThemeMode
import com.shestikpetr.meteoapp.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveSettingsUseCase @Inject constructor(
    private val repo: SettingsRepository
) {
    operator fun invoke(): Flow<AppSettings> = repo.settings
}

class SetThemeModeUseCase @Inject constructor(
    private val repo: SettingsRepository
) {
    suspend operator fun invoke(mode: ThemeMode) = repo.setThemeMode(mode)
}

class SetTooltipsEnabledUseCase @Inject constructor(
    private val repo: SettingsRepository
) {
    suspend operator fun invoke(enabled: Boolean) = repo.setTooltipsEnabled(enabled)
}

class ToggleStationHiddenUseCase @Inject constructor(
    private val repo: SettingsRepository
) {
    suspend operator fun invoke(stationNumber: String) = repo.toggleStationHidden(stationNumber)
}

class ToggleParameterHiddenUseCase @Inject constructor(
    private val repo: SettingsRepository
) {
    suspend operator fun invoke(parameterCode: Int) = repo.toggleParameterHidden(parameterCode)
}
