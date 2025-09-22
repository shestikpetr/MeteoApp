package com.shestikpetr.meteo.ui.map

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shestikpetr.meteo.cache.SensorDataCache
import com.shestikpetr.meteo.data.StationWithLocation
import com.shestikpetr.meteo.repository.interfaces.SensorDataRepository
import com.shestikpetr.meteo.repository.interfaces.StationRepository
import com.shestikpetr.meteo.model.ParameterConfig
import com.shestikpetr.meteo.service.ParameterConfigService
import com.shestikpetr.meteo.ui.Parameters
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
    private val sensorDataCache: SensorDataCache,
    private val stationDataTransformer: StationDataTransformer,
    private val parameterConfigService: ParameterConfigService
) : ViewModel() {

    private val _uiState: MutableStateFlow<MapUiState> = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    /**
     * Helper function to convert ParameterConfig to legacy Parameters enum for cache compatibility.
     * TODO: Remove this when SensorDataCache is updated to work with ParameterConfig directly.
     */
    private fun parameterConfigToLegacyEnum(parameterConfig: ParameterConfig?): Parameters? {
        if (parameterConfig == null) return null

        return when {
            parameterConfig.name.lowercase().contains("температур") ||
            parameterConfig.code.lowercase() == "t" ||
            parameterConfig.code == "4402" -> Parameters.TEMPERATURE

            parameterConfig.name.lowercase().contains("влажность") ||
            parameterConfig.code.lowercase() == "h" ||
            parameterConfig.code == "5402" -> Parameters.HUMIDITY

            parameterConfig.name.lowercase().contains("давление") ||
            parameterConfig.code.lowercase() == "p" ||
            parameterConfig.code == "700" -> Parameters.PRESSURE

            else -> null
        }
    }

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
                    Log.w("MapViewModel", "No stations available to load parameters")
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

                Log.d("MapViewModel", "Loaded ${stationConfig.parameters.size} available parameters")

                // Refresh data with the selected parameter
                if (_uiState.value.selectedParameter != null) {
                    getLatestSensorData()
                }

            } catch (e: Exception) {
                Log.e("MapViewModel", "Failed to load available parameters: ${e.message}")
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
     * Loads the list of user stations with their geographic coordinates.
     * This includes fallback to demo stations in debug mode if the request fails.
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

                val stations = stationRepository.getUserStationsWithLocation()

                if (stations.isEmpty()) {
                    Log.w("MapViewModel", "Получен пустой список станций")
                }

                // Сначала обновляем UI со станциями
                _uiState.update { currentState ->
                    currentState.copy(
                        userStations = stations,
                        isLoadingStations = false
                        // НЕ сбрасываем isLoadingLatestData здесь, так как еще загружаем данные
                    )
                }

                // Загружаем доступные параметры и затем данные для станций
                if (stations.isNotEmpty()) {
                    loadAvailableParameters() // Автоматически загрузит данные после загрузки параметров
                } else {
                    // Если станций нет, сбрасываем loading и загружаем fallback параметры
                    loadAvailableParameters() // Загрузит fallback параметры
                    _uiState.update { currentState ->
                        currentState.copy(isLoadingLatestData = false)
                    }
                }
            } catch (e: Exception) {
                Log.e("MapViewModel", "Ошибка загрузки станций: ${e.message}", e)

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
     * Changes the selected parameter for map display.
     * This method provides backward compatibility with the legacy Parameters enum.
     *
     * @deprecated Use selectParameter(ParameterConfig) instead
     * @param parameter The new parameter to display
     */
    @Deprecated("Use selectParameter(ParameterConfig) instead")
    fun changeMapParameter(parameter: Parameters) {
        Log.d("MapViewModel", "Смена параметра на: $parameter (legacy)")

        // Convert legacy parameter to ParameterConfig
        val parameterConfig = _uiState.value.availableParameters.find { config ->
            parameterConfigToLegacyEnum(config) == parameter
        } ?: run {
            Log.w("MapViewModel", "Could not find ParameterConfig for legacy parameter: $parameter")
            return
        }

        selectParameter(parameterConfig)
    }

    /**
     * Fetches the latest sensor data for all user stations for the currently selected parameter.
     * Uses caching to provide immediate feedback and then updates with fresh data from the server.
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
                Log.w("MapViewModel", "Нет станций для загрузки данных")
                _uiState.update { currentUiState ->
                    currentUiState.copy(isLoadingLatestData = false)
                }
                return@launch
            }

            Log.d("MapViewModel", "Загружаем данные для ${currentStations.size} станций")

            // Get cached values and show them immediately
            val dataMap = mutableMapOf<String, Double>()
            currentStations.forEach { station ->
                val legacyParameter = parameterConfigToLegacyEnum(selectedParameter)
                val cachedValue = if (legacyParameter != null) {
                    sensorDataCache.getValue(station.stationNumber, legacyParameter)
                } else null
                if (cachedValue != null) {
                    dataMap[station.stationNumber] = cachedValue
                    Log.d(
                        "MapViewModel",
                        "Using cached value for ${station.stationNumber}: $cachedValue"
                    )
                }
            }

            // Сразу обновляем UI с кешированными данными (если есть)
            if (dataMap.isNotEmpty()) {
                _uiState.update { currentUiState ->
                    currentUiState.copy(
                        latestSensorData = dataMap.toMap(),
                        isLoadingLatestData = true // Оставляем загрузку, так как еще обновляем данные
                    )
                }
                Log.d("MapViewModel", "Показаны кешированные данные: $dataMap")
            }

            // Затем обновляем данные с сервера для каждой станции
            currentStations.forEach { station ->
                val parameterCode = selectedParameter?.code ?: run {
                    Log.w("MapViewModel", "No parameter selected, skipping station ${station.stationNumber}")
                    return@forEach
                }

                Log.d(
                    "MapViewModel",
                    "Запрос данных для ${station.stationNumber}, параметр: $parameterCode"
                )

                try {
                    val latestData = sensorDataRepository.getLatestSensorData(
                        stationNumber = station.stationNumber,
                        parameter = parameterCode
                    )

                    if (sensorDataCache.isValidValue(latestData)) { // Only if we got valid data
                        dataMap[station.stationNumber] = latestData
                        // Save to cache
                        val legacyParameter = parameterConfigToLegacyEnum(selectedParameter)
                        if (legacyParameter != null) {
                            sensorDataCache.putValue(station.stationNumber, legacyParameter, latestData)
                        }
                        Log.d(
                            "MapViewModel",
                            "Received and cached data for ${station.stationNumber}: $latestData"
                        )

                        // ВАЖНО: Обновляем UI сразу после получения каждого значения
                        _uiState.update { currentUiState ->
                            currentUiState.copy(
                                latestSensorData = dataMap.toMap()
                            )
                        }
                    } else {
                        // If data not received, use cached value
                        val legacyParameter = parameterConfigToLegacyEnum(selectedParameter)
                        val cachedValue = if (legacyParameter != null) {
                            sensorDataCache.getValue(station.stationNumber, legacyParameter)
                        } else null
                        if (cachedValue != null && sensorDataCache.isValidValue(cachedValue)) {
                            dataMap[station.stationNumber] = cachedValue
                            Log.d(
                                "MapViewModel",
                                "Using cached value for ${station.stationNumber}: $cachedValue"
                            )
                        } else {
                            // If no cache, set to 0.0
                            dataMap[station.stationNumber] = 0.0
                            Log.d(
                                "MapViewModel",
                                "No data or cache for ${station.stationNumber}, setting to 0.0"
                            )
                        }
                    }

                } catch (e: Exception) {
                    Log.e(
                        "MapViewModel",
                        "Ошибка получения данных для ${station.stationNumber}: ${e.message}"
                    )
                    // On error, use cached value
                    val legacyParameter = parameterConfigToLegacyEnum(selectedParameter)
                    val cachedValue = if (legacyParameter != null) {
                        sensorDataCache.getValue(station.stationNumber, legacyParameter)
                    } else null
                    if (cachedValue != null && sensorDataCache.isValidValue(cachedValue)) {
                        dataMap[station.stationNumber] = cachedValue
                        Log.d(
                            "MapViewModel",
                            "On error using cache for ${station.stationNumber}: $cachedValue"
                        )
                    } else {
                        dataMap[station.stationNumber] = 0.0
                        Log.d(
                            "MapViewModel",
                            "On error no cache for ${station.stationNumber}, setting to 0.0"
                        )
                    }
                }
            }

            // Финальное обновление UI
            Log.d("MapViewModel", "Финальное обновление UI с данными: ${dataMap.size} значений")
            Log.d("MapViewModel", "Финальные данные: $dataMap")

            _uiState.update { currentUiState ->
                currentUiState.copy(
                    isLoadingLatestData = false,
                    latestSensorData = dataMap
                )
            }
        }
    }

    /**
     * Forces a refresh of all data (stations and sensor data).
     * Useful for pull-to-refresh functionality.
     */
    fun forceRefreshData() {
        Log.d("MapViewModel", "Принудительное обновление данных")
        if (_uiState.value.userStations.isNotEmpty()) {
            getLatestSensorData()
        } else {
            loadUserStations()
        }
    }

    /**
     * Clears all cached data and resets the UI state.
     * Used when user logs out or switches accounts.
     */
    fun clearData() {
        Log.d("MapViewModel", "Очистка данных при выходе")

        // Clear cache on logout
        sensorDataCache.clearAll()

        _uiState.update {
            MapUiState() // Возвращаем к начальному состоянию
        }
    }

    /**
     * Method for clearing cache (e.g., when user changes).
     */
    fun clearCache() {
        Log.d("MapViewModel", "Clearing data cache")
        sensorDataCache.clearAll()
    }

    /**
     * Method for getting cache information (for debugging).
     */
    fun getCacheInfo(): Map<String, Any> {
        return sensorDataCache.getCacheInfo()
    }

    /**
     * Type-safe method for getting cached values.
     */
    fun getCachedValues(): Map<String, Double> {
        return sensorDataCache.getAllValues()
    }

    /**
     * Method for checking if there's cached data for a station and parameter.
     */
    fun hasCachedData(stationId: String, parameter: Parameters): Boolean {
        return sensorDataCache.hasValue(stationId, parameter)
    }

    /**
     * Method for getting stations with cached data for the current parameter.
     */
    fun getCachedStationsForParameter(parameter: Parameters): Set<String> {
        return sensorDataCache.getCachedStationsForParameter(parameter)
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