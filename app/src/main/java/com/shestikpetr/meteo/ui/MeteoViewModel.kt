package com.shestikpetr.meteo.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.yml.charts.common.extensions.isNotNull
import com.shestikpetr.meteo.data.StationWithLocation
import com.shestikpetr.meteo.network.MeteoRepository
import com.shestikpetr.meteo.network.SensorDataPoint
import com.yandex.maps.mobile.BuildConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class Parameters(private val unit: String) {
    TEMPERATURE("°C"),
    HUMIDITY("%"),
    PRESSURE("гПа");

    fun getUnit(): String {
        return unit
    }
}

data class MapUiState(
    val selectedParameter: Parameters = Parameters.TEMPERATURE,
    val latestSensorData: Map<String, Double> = emptyMap(),
    val isLoadingLatestData: Boolean = false,
    val userStations: List<StationWithLocation> = emptyList(),
    val cameraPosZoom: Float = 15.0f
)

data class ChartUiState(
    val selectedParameter: Parameters = Parameters.TEMPERATURE,
    val selectedDateRange: Pair<Long?, Long?> = null to null,
    val isLoadingSensorData: Boolean = false,
    val sensorData: List<SensorDataPoint> = emptyList()
)

@HiltViewModel
class MeteoViewModel @Inject constructor(
    private val meteoRepository: MeteoRepository
) : ViewModel() {

    private val _mapUiState: MutableStateFlow<MapUiState> = MutableStateFlow(MapUiState())
    val mapUiState: StateFlow<MapUiState> = _mapUiState.asStateFlow()

    private val _chartUiState: MutableStateFlow<ChartUiState> = MutableStateFlow(ChartUiState())
    val chartUiState: StateFlow<ChartUiState> = _chartUiState.asStateFlow()

    // Кеш для хранения последних успешных значений по станциям и параметрам
    private val sensorDataCache = mutableMapOf<String, Double>()

    // Генерация ключа для кеша
    private fun getCacheKey(stationId: String, parameter: Parameters): String {
        return "${stationId}_${parameter.name}"
    }

    // Загружаем список станций пользователя после авторизации
    fun loadUserStations() {
        viewModelScope.launch {
            try {
                _mapUiState.update { currentState ->
                    currentState.copy(isLoadingLatestData = true)
                }

                val stations = meteoRepository.getUserStationsWithLocation()

                if (stations.isEmpty()) {
                    Log.w("MeteoViewModel", "Получен пустой список станций")
                }

                // Сначала обновляем UI со станциями
                _mapUiState.update { currentState ->
                    currentState.copy(
                        userStations = stations,
                        // НЕ сбрасываем isLoadingLatestData здесь, так как еще загружаем данные
                    )
                }

                // Затем загружаем данные для станций
                if (stations.isNotEmpty()) {
                    getLatestSensorData() // Эта функция сама управляет isLoadingLatestData
                } else {
                    // Если станций нет, сбрасываем loading
                    _mapUiState.update { currentState ->
                        currentState.copy(isLoadingLatestData = false)
                    }
                }
            } catch (e: Exception) {
                Log.e("MeteoViewModel", "Ошибка загрузки станций: ${e.message}", e)

                _mapUiState.update { currentState ->
                    currentState.copy(
                        isLoadingLatestData = false,
                        userStations = if (currentState.userStations.isEmpty() && BuildConfig.DEBUG)
                            createEmergencyStations() else currentState.userStations
                    )
                }
            }
        }
    }

    // Функция для создания аварийных тестовых станций, когда всё остальное не работает
    private fun createEmergencyStations(): List<StationWithLocation> {
        return listOf(
            StationWithLocation(
                stationNumber = "60000105",
                name = "Томск (тестовая станция)",
                latitude = 56.460850,
                longitude = 84.962327
            )
        )
    }

    // Обновляем уровень масштабирования на карте
    fun updateCameraZoom(zoom: Float) {
        _mapUiState.update { currentState ->
            currentState.copy(cameraPosZoom = zoom)
        }
    }

    fun getSensorData(complexId: String) {
        val selectedParameter = _chartUiState.value.selectedParameter
        val dateRange = _chartUiState.value.selectedDateRange

        viewModelScope.launch {
            _chartUiState.update { currentUiState ->
                currentUiState.copy(isLoadingSensorData = true)
            }

            val sensorData = try {
                val parameterCode = when (selectedParameter) {
                    Parameters.TEMPERATURE -> "4402"
                    Parameters.HUMIDITY -> "5402"
                    Parameters.PRESSURE -> "700"
                }
                meteoRepository.getSensorData(
                    complexId = complexId,
                    parameter = parameterCode,
                    startTime = dateRange.first?.div(1000),
                    endTime = dateRange.second?.div(1000)
                ).ifEmpty {
                    Log.e("MeteoViewModel", "Данные отсутствуют для выбранного периода")
                    emptyList()
                }
            } catch (e: Exception) {
                Log.e("MeteoViewModel", "Error fetching sensor data: ${e.message}")
                emptyList()
            }

            _chartUiState.update { currentUiState ->
                currentUiState.copy(
                    isLoadingSensorData = false,
                    sensorData = sensorData
                )
            }
        }
    }

    fun changeChartParameter(parameter: Parameters) {
        _chartUiState.update { currentUiState ->
            currentUiState.copy(
                selectedParameter = parameter
            )
        }
    }

    fun changeDateRange(dateRange: Pair<Long?, Long?>) {
        _chartUiState.update { currentUiState ->
            currentUiState.copy(
                selectedDateRange = dateRange
            )
        }
    }

    fun changeMapParameter(parameter: Parameters) {
        Log.d("MeteoViewModel", "Смена параметра на: $parameter")

        _mapUiState.update { currentUiState ->
            currentUiState.copy(
                selectedParameter = parameter
            )
        }

        // Загружаем данные только если есть станции
        if (_mapUiState.value.userStations.isNotEmpty()) {
            getLatestSensorData()
        } else {
            Log.w("MeteoViewModel", "Нет станций для загрузки данных при смене параметра")
            // Пробуем загрузить станции, если их нет
            loadUserStations()
        }
    }

    private fun getLatestSensorData() {
        viewModelScope.launch {
            _mapUiState.update { currentUiState ->
                currentUiState.copy(isLoadingLatestData = true)
            }

            val currentStations = _mapUiState.value.userStations
            val selectedParameter = _mapUiState.value.selectedParameter

            if (currentStations.isEmpty()) {
                Log.w("MeteoViewModel", "Нет станций для загрузки данных")
                _mapUiState.update { currentUiState ->
                    currentUiState.copy(isLoadingLatestData = false)
                }
                return@launch
            }

            Log.d("MeteoViewModel", "Загружаем данные для ${currentStations.size} станций")

            // Сначала берем кешированные значения и сразу показываем их
            val dataMap = mutableMapOf<String, Double>()
            currentStations.forEach { station ->
                val cacheKey = getCacheKey(station.stationNumber, selectedParameter)
                val cachedValue = sensorDataCache[cacheKey]
                if (cachedValue != null) {
                    dataMap[station.stationNumber] = cachedValue
                    Log.d(
                        "MeteoViewModel",
                        "Использую кешированное значение для ${station.stationNumber}: $cachedValue"
                    )
                }
            }

            // Сразу обновляем UI с кешированными данными (если есть)
            if (dataMap.isNotEmpty()) {
                _mapUiState.update { currentUiState ->
                    currentUiState.copy(
                        latestSensorData = dataMap.toMap(),
                        isLoadingLatestData = true // Оставляем загрузку, так как еще обновляем данные
                    )
                }
                Log.d("MeteoViewModel", "Показаны кешированные данные: $dataMap")
            }

            // Затем обновляем данные с сервера для каждой станции
            currentStations.forEach { station ->
                val parameterCode = when (selectedParameter) {
                    Parameters.TEMPERATURE -> "4402"
                    Parameters.HUMIDITY -> "5402"
                    Parameters.PRESSURE -> "700"
                }

                Log.d(
                    "MeteoViewModel",
                    "Запрос данных для ${station.stationNumber}, параметр: $parameterCode"
                )

                try {
                    val latestData = meteoRepository.getLatestSensorData(
                        complexId = station.stationNumber,
                        parameter = parameterCode
                    )

                    if (latestData > -100) { // Только если получили валидные данные
                        dataMap[station.stationNumber] = latestData
                        // Сохраняем в кеш
                        val cacheKey = getCacheKey(station.stationNumber, selectedParameter)
                        sensorDataCache[cacheKey] = latestData
                        Log.d(
                            "MeteoViewModel",
                            "Получены и сохранены данные для ${station.stationNumber}: $latestData"
                        )

                        // ВАЖНО: Обновляем UI сразу после получения каждого значения
                        _mapUiState.update { currentUiState ->
                            currentUiState.copy(
                                latestSensorData = dataMap.toMap()
                            )
                        }
                    } else {
                        // Если данные не получены (вернулся -100), используем кешированное значение
                        val cacheKey = getCacheKey(station.stationNumber, selectedParameter)
                        val cachedValue = sensorDataCache[cacheKey]
                        if (cachedValue != null && cachedValue > -100) {
                            dataMap[station.stationNumber] = cachedValue
                            Log.d(
                                "MeteoViewModel",
                                "Используем кешированное значение для ${station.stationNumber}: $cachedValue"
                            )
                        } else {
                            // Если кеша нет, устанавливаем 0.0
                            dataMap[station.stationNumber] = 0.0
                            Log.d(
                                "MeteoViewModel",
                                "Нет данных и кеша для ${station.stationNumber}, устанавливаем 0.0"
                            )
                        }
                    }

                } catch (e: Exception) {
                    Log.e(
                        "MeteoViewModel",
                        "Ошибка получения данных для ${station.stationNumber}: ${e.message}"
                    )
                    // При ошибке используем кешированное значение
                    val cacheKey = getCacheKey(station.stationNumber, selectedParameter)
                    val cachedValue = sensorDataCache[cacheKey]
                    if (cachedValue != null && cachedValue > -100) {
                        dataMap[station.stationNumber] = cachedValue
                        Log.d(
                            "MeteoViewModel",
                            "При ошибке используем кеш для ${station.stationNumber}: $cachedValue"
                        )
                    } else {
                        dataMap[station.stationNumber] = 0.0
                        Log.d(
                            "MeteoViewModel",
                            "При ошибке нет кеша для ${station.stationNumber}, устанавливаем 0.0"
                        )
                    }
                }
            }

            // Финальное обновление UI
            Log.d("MeteoViewModel", "Финальное обновление UI с данными: ${dataMap.size} значений")
            Log.d("MeteoViewModel", "Финальные данные: $dataMap")

            _mapUiState.update { currentUiState ->
                currentUiState.copy(
                    isLoadingLatestData = false,
                    latestSensorData = dataMap
                )
            }
        }
    }

    // Метод для очистки кеша (например, при смене пользователя)
    fun clearCache() {
        Log.d("MeteoViewModel", "Очистка кеша данных")
        sensorDataCache.clear()
    }

    fun clearData() {
        Log.d("MeteoViewModel", "Очистка данных при выходе")

        // Очищаем кеш при выходе
        clearCache()

        _mapUiState.update {
            MapUiState() // Возвращаем к начальному состоянию
        }

        _chartUiState.update {
            ChartUiState() // Возвращаем к начальному состоянию
        }
    }

    // Метод для принудительного обновления данных (например, по pull-to-refresh)
    fun forceRefreshData() {
        Log.d("MeteoViewModel", "Принудительное обновление данных")
        if (_mapUiState.value.userStations.isNotEmpty()) {
            getLatestSensorData()
        } else {
            loadUserStations()
        }
    }

    // Метод для получения информации о кеше (для отладки)
    fun getCacheInfo(): Map<String, Any> {
        return mapOf(
            "cacheSize" to sensorDataCache.size,
            "cachedValues" to sensorDataCache.toMap()
        )
    }

    // Типобезопасный метод для получения кешированных значений
    fun getCachedValues(): Map<String, Double> {
        return sensorDataCache.toMap()
    }

    // Метод для проверки, есть ли кешированные данные для станции и параметра
    fun hasCachedData(stationId: String, parameter: Parameters): Boolean {
        val cacheKey = getCacheKey(stationId, parameter)
        return sensorDataCache.containsKey(cacheKey)
    }

    // Метод для получения списка станций с кешированными данными для текущего параметра
    fun getCachedStationsForParameter(parameter: Parameters): Set<String> {
        return sensorDataCache.keys
            .filter { it.endsWith("_${parameter.name}") }
            .map { it.substringBefore("_${parameter.name}") }
            .toSet()
    }
}