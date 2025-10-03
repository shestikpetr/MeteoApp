package com.shestikpetr.meteo.ui.chart

import com.shestikpetr.meteo.common.logging.MeteoLogger
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shestikpetr.meteo.network.SensorDataPoint
import com.shestikpetr.meteo.repository.interfaces.SensorDataRepository
import com.shestikpetr.meteo.model.ParameterConfig
import com.shestikpetr.meteo.service.ParameterConfigService
import com.shestikpetr.meteo.localization.interfaces.LocalizationService
import com.shestikpetr.meteo.localization.interfaces.StringKey
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for the chart screen displaying historical sensor data.
 */
data class ChartUiState(
    val selectedParameter: ParameterConfig? = null,
    val availableParameters: List<ParameterConfig> = emptyList(),
    val selectedDateRange: Pair<Long?, Long?> = null to null,
    val isLoadingSensorData: Boolean = false,
    val sensorData: List<SensorDataPoint> = emptyList(),
    val errorMessage: String? = null,
    val selectedStationId: String? = null
)

/**
 * ViewModel responsible for managing chart-related UI state and operations.
 *
 * This ViewModel follows the Single Responsibility Principle by focusing exclusively on:
 * - Chart UI state management (selected parameter, selected date range, sensor data)
 * - Historical sensor data fetching for charts
 * - Date range selection
 * - Chart parameter selection
 * - Chart data filtering and processing
 */
@HiltViewModel
class ChartViewModel @Inject constructor(
    private val sensorDataRepository: SensorDataRepository,
    private val parameterConfigService: ParameterConfigService,
    private val localizationService: LocalizationService
) : ViewModel() {

    private val logger = MeteoLogger.forClass(ChartViewModel::class)

    private val _uiState: MutableStateFlow<ChartUiState> = MutableStateFlow(ChartUiState())
    val uiState: StateFlow<ChartUiState> = _uiState.asStateFlow()


    /**
     * Selects a parameter for display on the chart.
     * Updates the UI state and triggers data refresh if station is selected.
     */
    fun selectParameter(parameter: ParameterConfig) {
        _uiState.update { currentState ->
            currentState.copy(selectedParameter = parameter)
        }

        // Reload sensor data for the new parameter if station is selected
        val currentStationId = _uiState.value.selectedStationId
        if (currentStationId != null) {
            getSensorData(currentStationId)
        }
    }

    /**
     * Loads available parameters for the specified station.
     */
    fun loadAvailableParameters(stationNumber: String) {
        viewModelScope.launch {
            try {
                val stationConfig = parameterConfigService.getStationParameterConfig(stationNumber)

                _uiState.update { currentState ->
                    val newSelectedParameter = currentState.selectedParameter
                        ?: stationConfig.getDefault()
                        ?: stationConfig.parameters.firstOrNull()

                    currentState.copy(
                        availableParameters = stationConfig.getSorted(),
                        selectedParameter = newSelectedParameter
                    )
                }

                logger.d("Loaded ${stationConfig.parameters.size} available parameters for station $stationNumber")

            } catch (e: Exception) {
                logger.e("Failed to load available parameters for station $stationNumber: ${e.message}")
                // Use fallback parameters
                val fallbackConfig = parameterConfigService.globalParameterConfig.value
                _uiState.update { currentState ->
                    currentState.copy(
                        availableParameters = fallbackConfig.parameters,
                        selectedParameter = currentState.selectedParameter ?: fallbackConfig.getDefault()
                    )
                }
            }
        }
    }

    /**
     * Fetches sensor data for the specified station within the selected date range and parameter.
     *
     * @param stationNumber The 8-digit station number
     */
    fun getSensorData(stationNumber: String) {
        val selectedParameter = _uiState.value.selectedParameter
        val dateRange = _uiState.value.selectedDateRange

        viewModelScope.launch {
            _uiState.update { currentUiState ->
                currentUiState.copy(
                    isLoadingSensorData = true,
                    errorMessage = null,
                    selectedStationId = stationNumber
                )
            }

            val sensorData = try {
                val parameterCode = selectedParameter?.code ?: run {
                    logger.w("No parameter selected, cannot fetch data")
                    _uiState.update { currentState ->
                        currentState.copy(
                            isLoadingSensorData = false,
                            errorMessage = localizationService.getString(StringKey.ErrorNoParameterSelected)
                        )
                    }
                    return@launch
                }

                logger.d("Fetching data for station: $stationNumber, parameter: $parameterCode")
                logger.d("Date range: ${dateRange.first} to ${dateRange.second}")

                sensorDataRepository.getSensorData(
                    stationNumber = stationNumber,
                    parameter = parameterCode,
                    startTime = dateRange.first?.div(1000),
                    endTime = dateRange.second?.div(1000)
                ).ifEmpty {
                    logger.e("Данные отсутствуют для выбранного периода")
                    emptyList()
                }
            } catch (e: Exception) {
                logger.e("Error fetching sensor data: ${e.message}", e)

                _uiState.update { currentUiState ->
                    currentUiState.copy(
                        isLoadingSensorData = false,
                        errorMessage = "Ошибка загрузки данных: ${e.message}"
                    )
                }
                return@launch
            }

            _uiState.update { currentUiState ->
                currentUiState.copy(
                    isLoadingSensorData = false,
                    sensorData = sensorData
                )
            }

            logger.d("Loaded ${sensorData.size} data points for chart")
        }
    }


    /**
     * Changes the selected date range for chart data.
     * Automatically reloads sensor data if a station is selected.
     *
     * @param dateRange The new date range (start timestamp, end timestamp) in milliseconds
     */
    fun changeDateRange(dateRange: Pair<Long?, Long?>) {
        logger.d("Changing date range to: ${dateRange.first} - ${dateRange.second}")

        _uiState.update { currentUiState ->
            currentUiState.copy(
                selectedDateRange = dateRange,
                errorMessage = null
            )
        }

        // Reload data if station is selected
        _uiState.value.selectedStationId?.let { stationId ->
            getSensorData(stationId)
        }
    }

    /**
     * Refreshes the current chart data by reloading from the server.
     * Useful for pull-to-refresh functionality.
     */
    fun refreshChartData() {
        _uiState.value.selectedStationId?.let { stationId ->
            logger.d("Refreshing chart data for station: $stationId")
            getSensorData(stationId)
        } ?: run {
            logger.w("Cannot refresh data - no station selected")
        }
    }

    /**
     * Clears all chart data and resets the UI state.
     * Used when user logs out or switches accounts.
     */
    fun clearData() {
        logger.d("Clearing chart data")

        _uiState.update {
            ChartUiState() // Возвращаем к начальному состоянию
        }
    }

    /**
     * Clears any error messages from the UI state.
     */
    fun clearError() {
        _uiState.update { currentState ->
            currentState.copy(errorMessage = null)
        }
    }

    /**
     * Sets a custom date range using start and end dates.
     *
     * @param startDate Start date in milliseconds since epoch
     * @param endDate End date in milliseconds since epoch
     */
    fun setCustomDateRange(startDate: Long, endDate: Long) {
        changeDateRange(Pair(startDate, endDate))
    }

    /**
     * Sets a predefined date range (e.g., last 24 hours, last week).
     *
     * @param hours Number of hours from now going backwards
     */
    fun setRecentDateRange(hours: Int) {
        val endTime = System.currentTimeMillis()
        val startTime = endTime - (hours * 60 * 60 * 1000L)
        changeDateRange(Pair(startTime, endTime))
    }

    /**
     * Clears the current date range selection.
     */
    fun clearDateRange() {
        changeDateRange(Pair(null, null))
    }

    /**
     * Gets the current parameter code for API requests.
     */
    fun getCurrentParameterCode(): String? {
        return _uiState.value.selectedParameter?.code
    }

    /**
     * Checks if chart data is available.
     */
    fun hasChartData(): Boolean {
        return _uiState.value.sensorData.isNotEmpty()
    }

    /**
     * Gets the time range of the current chart data.
     */
    fun getDataTimeRange(): Pair<Long?, Long?> {
        val data = _uiState.value.sensorData
        if (data.isEmpty()) return Pair(null, null)

        val timestamps = data.map { it.time }
        return Pair(timestamps.minOrNull() as Long?, timestamps.maxOrNull() as Long?)
    }

    /**
     * Filters chart data based on a value range.
     *
     * @param minValue Minimum value to include (null for no minimum)
     * @param maxValue Maximum value to include (null for no maximum)
     */
    fun filterDataByValue(minValue: Double? = null, maxValue: Double? = null) {
        val currentData = _uiState.value.sensorData
        val filteredData = currentData.filter { dataPoint ->
            val valueInRange = (minValue == null || dataPoint.value >= minValue) &&
                              (maxValue == null || dataPoint.value <= maxValue)
            valueInRange
        }

        _uiState.update { currentState ->
            currentState.copy(sensorData = filteredData)
        }

        logger.d("Filtered data: ${currentData.size} -> ${filteredData.size} points")
    }

    /**
     * Resets any applied filters to show all available data.
     */
    fun resetFilters() {
        _uiState.value.selectedStationId?.let { stationId ->
            getSensorData(stationId)
        }
    }
}