package com.shestikpetr.meteo.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shestikpetr.meteo.data.Datasource.placemarks
import com.shestikpetr.meteo.network.MeteoRepository
import com.shestikpetr.meteo.network.SensorDataPoint
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
    val isLoadingLatestData: Boolean = false
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


    init {
        getLatestSensorData()
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
        _chartUiState.update {currentUiState ->
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
        _mapUiState.update {currentUiState ->
            currentUiState.copy(
                selectedParameter = parameter
            )
        }
        getLatestSensorData()
    }

    private fun getLatestSensorData() {
        viewModelScope.launch {
            _mapUiState.update {currentUiState ->
                currentUiState.copy(
                    isLoadingLatestData = true
                )
            }
            val dataMap = mutableMapOf<String, Double>()
            placemarks.forEach {
                val latestData = try {
                    when (_mapUiState.value.selectedParameter) {
                        Parameters.TEMPERATURE -> meteoRepository.getLatestSensorData(
                            complexId = it.first,
                            parameter = "4402"
                        )

                        Parameters.HUMIDITY -> meteoRepository.getLatestSensorData(
                            complexId = it.first,
                            parameter = "5402"
                        )

                        Parameters.PRESSURE -> meteoRepository.getLatestSensorData(
                            complexId = it.first,
                            parameter = "700"
                        )
                    }
                } catch (e: Exception) {
                    Log.e("MeteoViewModel", "Error fetching data for ${it.first}: ${e.message}")
                    0.0
                }
                dataMap[it.first] = latestData
            }

            _mapUiState.update {currentUiState ->
                currentUiState.copy(
                    isLoadingLatestData = false,
                    latestSensorData = dataMap
                )
            }
            Log.e("MeteoViewModel", "Данные получены ${_mapUiState.value.latestSensorData} ")
        }
    }

}