package com.shestikpetr.meteo.ui.map

import com.shestikpetr.meteo.common.logging.MeteoLogger
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shestikpetr.meteo.data.StationWithLocation
import com.shestikpetr.meteo.repository.interfaces.SensorDataRepository
import com.shestikpetr.meteo.repository.interfaces.StationRepository
import com.shestikpetr.meteo.model.ParameterConfig
import com.shestikpetr.meteo.service.ParameterConfigService
import com.shestikpetr.meteo.utils.StationDataTransformer
import com.yandex.maps.mobile.BuildConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for the map screen displaying weather stations and their current data.
 */
data class MapUiState(
    val selectedParameter: ParameterConfig? = null,
    val availableParameters: List<ParameterConfig> = emptyList(),
    val latestSensorData: Map<String, Double> = emptyMap(),
    val isLoadingLatestData: Boolean = false,
    val userStations: List<StationWithLocation> = emptyList(),
    val cameraPosZoom: Float = 15.0f,
    val isLoadingStations: Boolean = false,
    val errorMessage: String? = null
)

/**
 * ViewModel responsible for managing map-related UI state and operations.
 *
 * This ViewModel follows the Single Responsibility Principle by focusing exclusively on:
 * - Map UI state management (selected parameter, user stations, latest sensor data, camera zoom)
 * - Station loading and refresh operations
 * - Parameter selection for map display
 * - Camera zoom management
 * - Latest sensor data fetching for map markers
 */
@HiltViewModel
class MapViewModel @Inject constructor(
    private val sensorDataRepository: SensorDataRepository,
    private val stationRepository: StationRepository,
    private val stationDataTransformer: StationDataTransformer,
    private val parameterConfigService: ParameterConfigService
) : ViewModel() {

    private val logger = MeteoLogger.forClass(MapViewModel::class)

    private val _uiState: MutableStateFlow<MapUiState> = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()


    /**
     * Selects a parameter for display on the map.
     * Updates the UI state and triggers data refresh.
     */
    fun selectParameter(parameter: ParameterConfig) {
        _uiState.update { currentState ->
            currentState.copy(selectedParameter = parameter)
        }

        // Reload sensor data for the new parameter
        if (_uiState.value.userStations.isNotEmpty()) {
            getLatestSensorData()
        }
    }

    /**
     * Loads available parameters for the current stations.
     * Uses the first station to determine available parameters.
     */
    fun loadAvailableParameters() {
        viewModelScope.launch {
            try {
                val currentStations = _uiState.value.userStations
                if (currentStations.isEmpty()) {
                    logger.w("No stations available to load parameters")
                    // Use fallback parameters
                    val fallbackConfig = parameterConfigService.globalParameterConfig.value
                    _uiState.update { currentState ->
                        currentState.copy(
                            availableParameters = fallbackConfig.parameters,
                            selectedParameter = fallbackConfig.getDefault()
                        )
                    }
                    return@launch
                }

                // Use first station to get available parameters
                val firstStation = currentStations.first()
                val stationConfig = parameterConfigService.getStationParameterConfig(firstStation.stationNumber)

                _uiState.update { currentState ->
                    val newSelectedParameter = currentState.selectedParameter
                        ?: stationConfig.getDefault()
                        ?: stationConfig.parameters.firstOrNull()

                    currentState.copy(
                        availableParameters = stationConfig.getSorted(),
                        selectedParameter = newSelectedParameter
                    )
                }

                logger.d("Loaded ${stationConfig.parameters.size} available parameters")

                // Refresh data with the selected parameter
                if (_uiState.value.selectedParameter != null) {
                    getLatestSensorData()
                }

            } catch (e: Exception) {
                logger.e("Failed to load available parameters: ${e.message}")
                // Use fallback parameters
                val fallbackConfig = parameterConfigService.globalParameterConfig.value
                _uiState.update { currentState ->
                    currentState.copy(
                        availableParameters = fallbackConfig.parameters,
                        selectedParameter = fallbackConfig.getDefault()
                    )
                }
            }
        }
    }

    /**
     * Loads the list of user stations with their geographic coordinates and latest data.
     * Uses /api/v1/data/latest endpoint for accurate coordinates and data in a single request.
     */
    fun loadUserStations() {
        viewModelScope.launch {
            try {
                _uiState.update { currentState ->
                    currentState.copy(
                        isLoadingStations = true,
                        isLoadingLatestData = true,
                        errorMessage = null
                    )
                }

                // Fetch stations with coordinates and data from /api/v1/data/latest
                val (stations, allStationsData) = sensorDataRepository.getAllStationsWithLocationAndData()

                if (stations.isEmpty()) {
                    logger.w("Получен пустой список станций")
                } else {
                    logger.d("Загружено ${stations.size} станций с координатами из /api/v1/data/latest")
                }

                // Update UI with stations and load parameters
                _uiState.update { currentState ->
                    currentState.copy(
                        userStations = stations,
                        isLoadingStations = false
                    )
                }

                // Load available parameters and then update data
                if (stations.isNotEmpty()) {
                    loadAvailableParameters() // Will trigger getLatestSensorData()
                } else {
                    // If no stations, load fallback parameters
                    loadAvailableParameters()
                    _uiState.update { currentState ->
                        currentState.copy(isLoadingLatestData = false)
                    }
                }
            } catch (e: Exception) {
                logger.e("Ошибка загрузки станций: ${e.message}", e)

                _uiState.update { currentState ->
                    currentState.copy(
                        isLoadingStations = false,
                        isLoadingLatestData = false,
                        userStations = if (currentState.userStations.isEmpty() && BuildConfig.DEBUG)
                            stationDataTransformer.createEmergencyStations() else currentState.userStations,
                        errorMessage = "Ошибка загрузки станций: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Updates the camera zoom level on the map.
     *
     * @param zoom The new zoom level
     */
    fun updateCameraZoom(zoom: Float) {
        _uiState.update { currentState ->
            currentState.copy(cameraPosZoom = zoom)
        }
    }


    /**
     * Fetches the latest sensor data for all user stations for the currently selected parameter.
     * Uses bulk endpoint /api/v1/data/latest for efficient data retrieval.
     */
    private fun getLatestSensorData() {
        viewModelScope.launch {
            _uiState.update { currentUiState ->
                currentUiState.copy(
                    isLoadingLatestData = true,
                    errorMessage = null
                )
            }

            val currentStations = _uiState.value.userStations
            val selectedParameter = _uiState.value.selectedParameter

            if (currentStations.isEmpty()) {
                logger.w("Нет станций для загрузки данных")
                _uiState.update { currentUiState ->
                    currentUiState.copy(isLoadingLatestData = false)
                }
                return@launch
            }

            logger.d("Загружаем данные для ${currentStations.size} станций через /api/v1/data/latest")

            try {
                // Fetch all stations data at once using the bulk endpoint
                val allStationsData = sensorDataRepository.getAllStationsLatestData()
                logger.d("Получены данные для ${allStationsData.size} станций")

                val parameterCode = selectedParameter?.code
                val dataMap = mutableMapOf<String, Double>()

                if (parameterCode != null) {
                    // Extract the selected parameter value for each station
                    currentStations.forEach { station ->
                        val stationData = allStationsData[station.stationNumber]
                        val value = stationData?.get(parameterCode) ?: -99.0

                        dataMap[station.stationNumber] = value
                        logger.d("Данные для ${station.stationNumber}: $value")
                    }
                }

                // Update UI with all data at once
                logger.d("Обновление UI с ${dataMap.size} значениями")
                _uiState.update { currentUiState ->
                    currentUiState.copy(
                        isLoadingLatestData = false,
                        latestSensorData = dataMap
                    )
                }

            } catch (e: Exception) {
                logger.e("Ошибка загрузки данных станций: ${e.message}")

                _uiState.update { currentUiState ->
                    currentUiState.copy(
                        isLoadingLatestData = false,
                        errorMessage = "Ошибка загрузки данных: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Forces a refresh of all data (stations and sensor data).
     * Useful for pull-to-refresh functionality.
     */
    fun forceRefreshData() {
        logger.d("Принудительное обновление данных")
        if (_uiState.value.userStations.isNotEmpty()) {
            getLatestSensorData()
        } else {
            loadUserStations()
        }
    }

    /**
     * Clears all data and resets the UI state.
     * Used when user logs out or switches accounts.
     */
    fun clearData() {
        logger.d("Очистка данных при выходе")

        _uiState.update {
            MapUiState() // Возвращаем к начальному состоянию
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
}