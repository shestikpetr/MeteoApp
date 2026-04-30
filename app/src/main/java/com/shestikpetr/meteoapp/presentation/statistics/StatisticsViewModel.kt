package com.shestikpetr.meteoapp.presentation.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shestikpetr.meteoapp.domain.model.AppSettings
import com.shestikpetr.meteoapp.domain.model.ParameterMeta
import com.shestikpetr.meteoapp.domain.model.Station
import com.shestikpetr.meteoapp.domain.model.TimeSeriesPoint
import com.shestikpetr.meteoapp.domain.usecase.settings.ObserveSettingsUseCase
import com.shestikpetr.meteoapp.domain.usecase.stations.GetParameterHistoryUseCase
import com.shestikpetr.meteoapp.domain.usecase.stations.GetStationLatestUseCase
import com.shestikpetr.meteoapp.domain.usecase.stations.GetStationParametersUseCase
import com.shestikpetr.meteoapp.domain.usecase.stations.GetUserStationsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class TimePeriod(val label: String, val hours: Int) {
    DAY("24 часа", 24),
    WEEK("Неделя", 168),
    MONTH("Месяц", 720),
    CUSTOM("Свой", 0)
}

data class StatisticsUiState(
    val isLoading: Boolean = true,
    val isLoadingHistory: Boolean = false,
    val stations: List<Station> = emptyList(),
    val selectedStation: Station? = null,
    val parameters: List<ParameterMeta> = emptyList(),
    val selectedParameters: List<ParameterMeta> = emptyList(),
    val period: TimePeriod = TimePeriod.DAY,
    val customStartTime: Long? = null,
    val customEndTime: Long? = null,
    val historyData: List<TimeSeriesPoint> = emptyList(),
    val parameterUnit: String? = null,
    val additionalParamsData: Map<Int, List<TimeSeriesPoint>> = emptyMap(),
    val previousPeriodData: List<TimeSeriesPoint> = emptyList(),
    val comparePeriodEnabled: Boolean = false,
    val thresholdMin: Double? = null,
    val thresholdMax: Double? = null,
    val showTrendLine: Boolean = true,
    val lastDataTime: Long? = null,
    val tooltipsEnabled: Boolean = true,
    val settings: AppSettings = AppSettings()
)

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val getUserStations: GetUserStationsUseCase,
    private val getStationParameters: GetStationParametersUseCase,
    private val getStationLatest: GetStationLatestUseCase,
    private val getParameterHistory: GetParameterHistoryUseCase,
    observeSettings: ObserveSettingsUseCase
) : ViewModel() {

    private val raw = MutableStateFlow(StatisticsUiState())

    val state: StateFlow<StatisticsUiState> = combine(raw, observeSettings()) { s, settings ->
        s.copy(settings = settings, tooltipsEnabled = settings.tooltipsEnabled)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, StatisticsUiState())

    init {
        // При появлении настроек — фильтруем станции и параметры от скрытых.
        viewModelScope.launch {
            observeSettings().collect { settings ->
                refreshAfterSettingsChange(settings)
            }
        }
    }

    private suspend fun refreshAfterSettingsChange(settings: AppSettings) {
        if (raw.value.stations.isEmpty() && raw.value.isLoading) {
            loadStations(settings)
        } else {
            // Подрезаем выбранную станцию/параметры если они стали скрытыми.
            raw.update { current ->
                val visibleStations = current.stations.filter { it.stationNumber !in settings.hiddenStations }
                val visibleParams = current.parameters.filter { it.code !in settings.hiddenParameters }
                val newSelected =
                    current.selectedStation?.takeIf { it.stationNumber !in settings.hiddenStations }
                        ?: visibleStations.firstOrNull()
                val newSelectedParams =
                    current.selectedParameters.filter { it.code !in settings.hiddenParameters }
                        .ifEmpty { listOfNotNull(visibleParams.firstOrNull()) }
                current.copy(
                    selectedStation = newSelected,
                    parameters = visibleParams,
                    selectedParameters = newSelectedParams
                )
            }
        }
    }

    private suspend fun loadStations(settings: AppSettings) {
        raw.update { it.copy(isLoading = true) }
        val list = getUserStations().getOrElse { emptyList() }
        val visible = list.filter { it.stationNumber !in settings.hiddenStations }
        raw.update { it.copy(stations = visible, isLoading = false) }
        visible.firstOrNull()?.let { selectStation(it) }
    }

    fun selectStation(station: Station) {
        raw.update { it.copy(selectedStation = station, parameters = emptyList(), selectedParameters = emptyList()) }
        viewModelScope.launch {
            val params = getStationParameters(station.stationNumber).getOrElse { emptyList() }
            val visible = params.filter { it.code !in raw.value.settings.hiddenParameters }
            val first = listOfNotNull(visible.firstOrNull())
            raw.update {
                it.copy(parameters = visible, selectedParameters = first)
            }
            reloadHistory()
            reloadLastDataTime()
        }
    }

    fun toggleParameter(meta: ParameterMeta) {
        raw.update { current ->
            val isSelected = current.selectedParameters.any { it.code == meta.code }
            val next = when {
                isSelected && current.selectedParameters.size > 1 ->
                    current.selectedParameters.filter { it.code != meta.code }
                isSelected -> current.selectedParameters
                current.selectedParameters.size < MAX_SELECTED_PARAMS ->
                    current.selectedParameters + meta
                else -> current.selectedParameters
            }
            current.copy(selectedParameters = next)
        }
        reloadHistory()
    }

    fun setPeriod(period: TimePeriod) {
        raw.update { it.copy(period = period) }
        reloadHistory()
    }

    fun setCustomStartTime(epochMs: Long) {
        raw.update { it.copy(customStartTime = epochMs, period = TimePeriod.CUSTOM) }
        reloadHistory()
    }

    fun setCustomEndTime(epochMs: Long) {
        raw.update { it.copy(customEndTime = epochMs, period = TimePeriod.CUSTOM) }
        reloadHistory()
    }

    fun setComparePeriod(enabled: Boolean) {
        raw.update { it.copy(comparePeriodEnabled = enabled) }
        reloadHistory()
    }

    fun setThresholds(min: Double?, max: Double?) {
        raw.update { it.copy(thresholdMin = min, thresholdMax = max) }
    }

    fun setShowTrendLine(value: Boolean) {
        raw.update { it.copy(showTrendLine = value) }
    }

    private fun reloadLastDataTime() {
        val station = raw.value.selectedStation ?: return
        viewModelScope.launch {
            val latest = getStationLatest(station.stationNumber).getOrNull()
            raw.update { it.copy(lastDataTime = latest?.time) }
        }
    }

    private fun reloadHistory() {
        val current = raw.value
        val station = current.selectedStation ?: return
        if (current.selectedParameters.isEmpty()) {
            raw.update { it.copy(historyData = emptyList(), additionalParamsData = emptyMap(), parameterUnit = null) }
            return
        }
        val (startTime, endTime) = computeRange(current) ?: return

        raw.update { it.copy(isLoadingHistory = true) }
        viewModelScope.launch {
            val first = current.selectedParameters.first()
            val firstResult = getParameterHistory(station.stationNumber, first.code, startTime, endTime).getOrNull()

            val additional = mutableMapOf<Int, List<TimeSeriesPoint>>()
            current.selectedParameters.drop(1).forEach { p ->
                getParameterHistory(station.stationNumber, p.code, startTime, endTime).getOrNull()?.let {
                    additional[p.code] = it.points
                }
            }

            val previous = if (current.comparePeriodEnabled) {
                val periodLength = endTime - startTime
                val prevEnd = startTime
                val prevStart = prevEnd - periodLength
                getParameterHistory(station.stationNumber, first.code, prevStart, prevEnd)
                    .getOrNull()?.points.orEmpty()
            } else emptyList()

            raw.update {
                it.copy(
                    isLoadingHistory = false,
                    historyData = firstResult?.points.orEmpty(),
                    parameterUnit = firstResult?.parameter?.unit,
                    additionalParamsData = additional,
                    previousPeriodData = previous
                )
            }
        }
    }

    private fun computeRange(current: StatisticsUiState): Pair<Long, Long>? {
        return if (current.period == TimePeriod.CUSTOM) {
            val start = current.customStartTime ?: return null
            val end = current.customEndTime ?: return null
            (start / 1000) to (end / 1000)
        } else {
            val end = System.currentTimeMillis() / 1000
            val start = end - current.period.hours * 3600L
            start to end
        }
    }

    private companion object {
        const val MAX_SELECTED_PARAMS = 5
    }
}
