package com.shestikpetr.meteoapp.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shestikpetr.meteoapp.domain.model.AppSettings
import com.shestikpetr.meteoapp.domain.model.ParameterMeta
import com.shestikpetr.meteoapp.domain.model.Station
import com.shestikpetr.meteoapp.domain.model.StationLatest
import com.shestikpetr.meteoapp.domain.usecase.settings.ObserveSettingsUseCase
import com.shestikpetr.meteoapp.domain.usecase.stations.GetStationLatestUseCase
import com.shestikpetr.meteoapp.domain.usecase.stations.GetUserStationsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Состояние карты: список привязанных станций со свежим срезом данных.
 *
 * `latestByStation` — по одному /data на станцию. Если станция давно не передавала, поле time=null и values=null.
 */
data class MainUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val stations: List<Station> = emptyList(),
    val latestByStation: Map<String, StationLatest> = emptyMap(),
    val allParameters: List<ParameterMeta> = emptyList(),
    val selectedParameter: ParameterMeta? = null,
    val selectedStationNumber: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val getUserStations: GetUserStationsUseCase,
    private val getStationLatest: GetStationLatestUseCase,
    observeSettings: ObserveSettingsUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(MainUiState())

    /**
     * Скомбинированное состояние, фильтрующее станции и параметры по hidden* настройкам пользователя.
     * UI-слой использует именно его — сами скрытые в локальных полях не лежат.
     */
    val state: StateFlow<VisibleMainUiState> = combine(_state, observeSettings()) { raw, settings ->
        val visibleStations = raw.stations.filter { it.stationNumber !in settings.hiddenStations }
        val visibleParameters = raw.allParameters.filter { it.code !in settings.hiddenParameters }
        VisibleMainUiState(
            isLoading = raw.isLoading,
            isRefreshing = raw.isRefreshing,
            stations = visibleStations,
            latestByStation = raw.latestByStation,
            allParameters = visibleParameters,
            selectedParameter = raw.selectedParameter
                ?.takeIf { it.code !in settings.hiddenParameters },
            selectedStationNumber = raw.selectedStationNumber
                ?.takeIf { it !in settings.hiddenStations },
            settings = settings,
            errorMessage = raw.errorMessage
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, VisibleMainUiState())

    init {
        loadAll(initial = true)
    }

    fun refresh() = loadAll(initial = false)

    fun selectParameter(param: ParameterMeta?) = _state.update {
        // Тапнули на тот же параметр — снимаем выделение.
        val next = if (it.selectedParameter?.code == param?.code) null else param
        it.copy(selectedParameter = next)
    }

    fun selectStation(stationNumber: String?) = _state.update {
        // Повторный тап по той же станции закрывает карточку.
        val next = if (it.selectedStationNumber == stationNumber) null else stationNumber
        it.copy(selectedStationNumber = next)
    }

    private fun loadAll(initial: Boolean) {
        _state.update {
            if (initial) it.copy(isLoading = true, errorMessage = null)
            else it.copy(isRefreshing = true, errorMessage = null)
        }
        viewModelScope.launch {
            val stationsResult = getUserStations()
            stationsResult.fold(
                onSuccess = { loaded ->
                    val latest = fetchLatestParallel(loaded)
                    val allParams = collectAllParameters(latest)
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            stations = loaded,
                            latestByStation = latest,
                            allParameters = allParams
                        )
                    }
                },
                onFailure = { e ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = e.message ?: "Не удалось загрузить станции"
                        )
                    }
                }
            )
        }
    }

    private suspend fun fetchLatestParallel(stations: List<Station>): Map<String, StationLatest> =
        coroutineScope {
            stations.map { st ->
                async { st.stationNumber to getStationLatest(st.stationNumber).getOrNull() }
            }.awaitAll()
                .mapNotNull { (sn, latest) -> latest?.let { sn to it } }
                .toMap()
        }

    private fun collectAllParameters(latest: Map<String, StationLatest>): List<ParameterMeta> {
        val byCode = mutableMapOf<Int, ParameterMeta>()
        latest.values.forEach { stationLatest ->
            stationLatest.parameters.forEach { p ->
                byCode.putIfAbsent(p.code, p.meta)
            }
        }
        return byCode.values.toList()
    }

    private suspend fun <T> List<kotlinx.coroutines.Deferred<T>>.awaitAll(): List<T> =
        map { it.await() }
}

/** Состояние, которое реально показывается в UI. Скрытые элементы уже отфильтрованы. */
data class VisibleMainUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val stations: List<Station> = emptyList(),
    val latestByStation: Map<String, StationLatest> = emptyMap(),
    val allParameters: List<ParameterMeta> = emptyList(),
    val selectedParameter: ParameterMeta? = null,
    val selectedStationNumber: String? = null,
    val settings: AppSettings = AppSettings(),
    val errorMessage: String? = null
)
