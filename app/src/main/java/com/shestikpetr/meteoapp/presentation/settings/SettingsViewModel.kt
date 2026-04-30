package com.shestikpetr.meteoapp.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shestikpetr.meteoapp.domain.model.AppSettings
import com.shestikpetr.meteoapp.domain.model.ParameterMeta
import com.shestikpetr.meteoapp.domain.model.Station
import com.shestikpetr.meteoapp.domain.model.ThemeMode
import com.shestikpetr.meteoapp.domain.model.User
import com.shestikpetr.meteoapp.domain.repository.AuthRepository
import com.shestikpetr.meteoapp.domain.usecase.auth.LogoutUseCase
import com.shestikpetr.meteoapp.domain.usecase.settings.ObserveSettingsUseCase
import com.shestikpetr.meteoapp.domain.usecase.settings.SetThemeModeUseCase
import com.shestikpetr.meteoapp.domain.usecase.settings.SetTooltipsEnabledUseCase
import com.shestikpetr.meteoapp.domain.usecase.settings.ToggleParameterHiddenUseCase
import com.shestikpetr.meteoapp.domain.usecase.settings.ToggleStationHiddenUseCase
import com.shestikpetr.meteoapp.domain.usecase.stations.AttachStationUseCase
import com.shestikpetr.meteoapp.domain.usecase.stations.DetachStationUseCase
import com.shestikpetr.meteoapp.domain.usecase.stations.GetAllParametersUseCase
import com.shestikpetr.meteoapp.domain.usecase.stations.GetUserStationsUseCase
import com.shestikpetr.meteoapp.domain.usecase.stations.RenameStationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val user: User? = null,
    val stations: List<Station> = emptyList(),
    val parameters: List<ParameterMeta> = emptyList(),
    val isLoading: Boolean = true,
    val settings: AppSettings = AppSettings()
)

sealed interface SettingsEffect {
    data class Toast(val message: String) : SettingsEffect
    data object NavigateToAuth : SettingsEffect
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val getUserStations: GetUserStationsUseCase,
    private val getAllParameters: GetAllParametersUseCase,
    private val attachStation: AttachStationUseCase,
    private val detachStation: DetachStationUseCase,
    private val renameStation: RenameStationUseCase,
    private val toggleStationHidden: ToggleStationHiddenUseCase,
    private val toggleParameterHidden: ToggleParameterHiddenUseCase,
    private val setThemeMode: SetThemeModeUseCase,
    private val setTooltipsEnabled: SetTooltipsEnabledUseCase,
    private val logout: LogoutUseCase,
    observeSettings: ObserveSettingsUseCase
) : ViewModel() {

    private val raw = MutableStateFlow(SettingsUiState())

    val state: StateFlow<SettingsUiState> = combine(raw, observeSettings()) { s, settings ->
        s.copy(settings = settings)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SettingsUiState())

    private val _effects = Channel<SettingsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        reload()
        viewModelScope.launch {
            authRepository.me().getOrNull()?.let { user -> raw.update { it.copy(user = user) } }
        }
    }

    fun reload() {
        raw.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val stations = getUserStations().getOrElse { emptyList() }
            val parameters = getAllParameters(stations).getOrElse { emptyList() }
            raw.update {
                it.copy(stations = stations, parameters = parameters, isLoading = false)
            }
        }
    }

    fun onAddStation(stationNumber: String) {
        viewModelScope.launch {
            attachStation(stationNumber).fold(
                onSuccess = {
                    _effects.send(SettingsEffect.Toast("Станция добавлена"))
                    reload()
                },
                onFailure = { e ->
                    _effects.send(SettingsEffect.Toast("Ошибка: ${e.message}"))
                }
            )
        }
    }

    fun onDeleteStation(stationNumber: String) {
        viewModelScope.launch {
            detachStation(stationNumber).fold(
                onSuccess = {
                    _effects.send(SettingsEffect.Toast("Станция удалена"))
                    reload()
                },
                onFailure = { e ->
                    _effects.send(SettingsEffect.Toast("Ошибка: ${e.message}"))
                }
            )
        }
    }

    fun onRenameStation(stationNumber: String, newName: String) {
        viewModelScope.launch {
            renameStation(stationNumber, newName).fold(
                onSuccess = {
                    _effects.send(SettingsEffect.Toast("Станция переименована"))
                    reload()
                },
                onFailure = { e ->
                    _effects.send(SettingsEffect.Toast("Ошибка: ${e.message}"))
                }
            )
        }
    }

    fun onToggleStationHidden(stationNumber: String) {
        viewModelScope.launch { toggleStationHidden(stationNumber) }
    }

    fun onToggleParameterHidden(parameterCode: Int) {
        viewModelScope.launch { toggleParameterHidden(parameterCode) }
    }

    fun onSetThemeMode(mode: ThemeMode) {
        viewModelScope.launch { setThemeMode(mode) }
    }

    fun onSetTooltipsEnabled(enabled: Boolean) {
        viewModelScope.launch { setTooltipsEnabled(enabled) }
    }

    fun onLogout() {
        viewModelScope.launch {
            logout()
            _effects.send(SettingsEffect.NavigateToAuth)
        }
    }
}
