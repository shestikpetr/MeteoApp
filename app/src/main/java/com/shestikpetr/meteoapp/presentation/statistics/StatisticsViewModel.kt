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

/** Идентификатор серии: «станция × параметр». */
data class SeriesKey(val stationNumber: String, val parameterCode: Int)

data class StatisticsUiState(
    val isLoading: Boolean = true,
    val isLoadingHistory: Boolean = false,
    val stations: List<Station> = emptyList(),
    /** Несколько выбранных станций. Если >1 — допускается ровно один параметр. */
    val selectedStations: List<Station> = emptyList(),
    /** Параметры — пересечение по выбранным станциям (одинаковые во всех). */
    val parameters: List<ParameterMeta> = emptyList(),
    val selectedParameters: List<ParameterMeta> = emptyList(),
    val period: TimePeriod = TimePeriod.DAY,
    /** Произвольный диапазон в миллисекундах. */
    val customStartMs: Long? = null,
    val customEndMs: Long? = null,
    /** История по каждой серии. */
    val seriesData: Map<SeriesKey, List<TimeSeriesPoint>> = emptyMap(),
    val previousPeriodData: Map<SeriesKey, List<TimeSeriesPoint>> = emptyMap(),
    /** Единицы измерения по коду параметра. */
    val parameterUnits: Map<Int, String?> = emptyMap(),
    val comparePeriodEnabled: Boolean = false,
    val thresholdMin: Double? = null,
    val thresholdMax: Double? = null,
    val showTrendLine: Boolean = true,
    /** «Время последних данных» по каждой видимой станции (ключ — stationNumber). */
    val lastDataTimes: Map<String, Long?> = emptyMap(),
    val settings: AppSettings = AppSettings()
) {
    val isMultiStation: Boolean get() = selectedStations.size > 1
    val maxParametersAllowed: Int get() = if (isMultiStation) 1 else MAX_PARAMETERS
}

private const val MAX_PARAMETERS = 5
private const val MAX_STATIONS = 3

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
        s.copy(settings = settings)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, StatisticsUiState())

    init {
        viewModelScope.launch {
            observeSettings().collect { settings ->
                refreshAfterSettingsChange(settings)
            }
        }
    }

    private suspend fun refreshAfterSettingsChange(settings: AppSettings) {
        if (raw.value.stations.isEmpty() && raw.value.isLoading) {
            loadStations(settings)
            return
        }
        // Сужаем выборы при появлении новых скрытий.
        val current = raw.value
        val visibleStations = current.stations.filter { it.stationNumber !in settings.hiddenStations }
        val newSelectedStations = current.selectedStations
            .filter { it.stationNumber !in settings.hiddenStations }
            .ifEmpty { listOfNotNull(visibleStations.firstOrNull()) }
        val visibleParams = current.parameters.filter { it.code !in settings.hiddenParameters }
        val newSelectedParams = current.selectedParameters
            .filter { it.code !in settings.hiddenParameters }
            .ifEmpty { listOfNotNull(visibleParams.firstOrNull()) }
        val changed =
            newSelectedStations.map { it.stationNumber } != current.selectedStations.map { it.stationNumber } ||
                    newSelectedParams.map { it.code } != current.selectedParameters.map { it.code } ||
                    visibleStations != current.stations
        raw.update {
            it.copy(
                stations = visibleStations,
                selectedStations = newSelectedStations,
                parameters = visibleParams,
                selectedParameters = newSelectedParams
            )
        }
        if (changed) {
            loadAvailableParameters(newSelectedStations)
            reloadHistory()
            reloadLastDataTime()
        }
    }

    private suspend fun loadStations(settings: AppSettings) {
        raw.update { it.copy(isLoading = true) }
        val list = getUserStations().getOrElse { emptyList() }
        val visible = list.filter { it.stationNumber !in settings.hiddenStations }
        raw.update { it.copy(stations = visible, isLoading = false) }
        // Подгружаем «последние данные» сразу для всего списка.
        reloadLastDataTime()
        // Стартуем с одной станцией.
        visible.firstOrNull()?.let { toggleStation(it) }
    }

    fun toggleStation(station: Station) {
        val current = raw.value
        val already = current.selectedStations.any { it.stationNumber == station.stationNumber }
        val next = when {
            already && current.selectedStations.size > 1 ->
                current.selectedStations.filter { it.stationNumber != station.stationNumber }
            already -> current.selectedStations
            current.selectedStations.size < MAX_STATIONS ->
                current.selectedStations + station
            else -> current.selectedStations
        }
        if (next.map { it.stationNumber } == current.selectedStations.map { it.stationNumber }) return
        raw.update { it.copy(selectedStations = next) }
        viewModelScope.launch {
            loadAvailableParameters(next)
            reloadHistory()
        }
    }

    fun toggleParameter(meta: ParameterMeta) {
        raw.update { current ->
            val isSelected = current.selectedParameters.any { it.code == meta.code }
            val limit = current.maxParametersAllowed
            val next = when {
                isSelected && current.selectedParameters.size > 1 ->
                    current.selectedParameters.filter { it.code != meta.code }
                isSelected -> current.selectedParameters
                limit == 1 -> listOf(meta)
                current.selectedParameters.size < limit ->
                    current.selectedParameters + meta
                else -> current.selectedParameters
            }
            current.copy(selectedParameters = next)
        }
        reloadHistory()
    }

    fun setPeriod(period: TimePeriod) {
        if (period == TimePeriod.CUSTOM) {
            // Активация «Свой» — без полной перезагрузки, просто запомнить.
            raw.update { it.copy(period = period) }
            if (raw.value.customStartMs != null && raw.value.customEndMs != null) reloadHistory()
            return
        }
        raw.update { it.copy(period = period) }
        reloadHistory()
    }

    fun setCustomRange(startMs: Long, endMs: Long) {
        if (startMs >= endMs) return
        raw.update {
            it.copy(
                customStartMs = startMs,
                customEndMs = endMs,
                period = TimePeriod.CUSTOM
            )
        }
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

    private suspend fun loadAvailableParameters(stations: List<Station>) {
        if (stations.isEmpty()) {
            raw.update { it.copy(parameters = emptyList(), selectedParameters = emptyList()) }
            return
        }
        val hidden = raw.value.settings.hiddenParameters
        val perStation = stations.map { station ->
            getStationParameters(station.stationNumber).getOrElse { emptyList() }
                .filter { it.code !in hidden }
        }
        // Пересечение по коду; имена/описания берём из первой станции.
        val intersection = if (perStation.isEmpty()) emptyList()
        else {
            val codes = perStation.map { it.map { p -> p.code }.toSet() }
                .reduce { acc, set -> acc intersect set }
            perStation[0].filter { it.code in codes }
        }
        raw.update { current ->
            val survived = current.selectedParameters
                .filter { p -> intersection.any { it.code == p.code } }
            val isMulti = stations.size > 1
            val limit = if (isMulti) 1 else MAX_PARAMETERS
            val newSelected = when {
                survived.isEmpty() -> listOfNotNull(intersection.firstOrNull())
                survived.size > limit -> survived.take(limit)
                else -> survived
            }
            current.copy(parameters = intersection, selectedParameters = newSelected)
        }
    }

    private fun reloadLastDataTime() {
        val stations = raw.value.stations
        if (stations.isEmpty()) {
            raw.update { it.copy(lastDataTimes = emptyMap()) }
            return
        }
        viewModelScope.launch {
            // Грузим параллельно и собираем карту `stationNumber -> time`.
            val result = mutableMapOf<String, Long?>()
            stations.forEach { st ->
                result[st.stationNumber] = getStationLatest(st.stationNumber).getOrNull()?.time
            }
            raw.update { it.copy(lastDataTimes = result.toMap()) }
        }
    }

    private fun reloadHistory() {
        val current = raw.value
        val stations = current.selectedStations
        val params = current.selectedParameters
        if (stations.isEmpty() || params.isEmpty()) {
            raw.update {
                it.copy(
                    seriesData = emptyMap(),
                    previousPeriodData = emptyMap(),
                    parameterUnits = emptyMap()
                )
            }
            return
        }
        val (start, end) = computeRange(current) ?: return
        raw.update { it.copy(isLoadingHistory = true) }
        viewModelScope.launch {
            val data = mutableMapOf<SeriesKey, List<TimeSeriesPoint>>()
            val prev = mutableMapOf<SeriesKey, List<TimeSeriesPoint>>()
            val units = mutableMapOf<Int, String?>()
            for (st in stations) {
                for (p in params) {
                    val key = SeriesKey(st.stationNumber, p.code)
                    val res = getParameterHistory(st.stationNumber, p.code, start, end).getOrNull()
                    if (res != null) {
                        data[key] = res.points
                        units.putIfAbsent(p.code, res.parameter.unit ?: p.unit)
                    } else {
                        units.putIfAbsent(p.code, p.unit)
                    }
                    if (current.comparePeriodEnabled) {
                        val len = end - start
                        val prevEnd = start
                        val prevStart = prevEnd - len
                        getParameterHistory(st.stationNumber, p.code, prevStart, prevEnd)
                            .getOrNull()?.let { prev[key] = it.points }
                    }
                }
            }
            raw.update {
                it.copy(
                    isLoadingHistory = false,
                    seriesData = data,
                    previousPeriodData = prev,
                    parameterUnits = units
                )
            }
        }
    }

    private fun computeRange(current: StatisticsUiState): Pair<Long, Long>? {
        return if (current.period == TimePeriod.CUSTOM) {
            val start = current.customStartMs ?: return null
            val end = current.customEndMs ?: return null
            (start / 1000) to (end / 1000)
        } else {
            val end = System.currentTimeMillis() / 1000
            val start = end - current.period.hours * 3600L
            start to end
        }
    }
}
