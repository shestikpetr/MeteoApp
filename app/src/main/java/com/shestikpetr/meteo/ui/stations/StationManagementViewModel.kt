package com.shestikpetr.meteo.ui.stations

import com.shestikpetr.meteo.common.logging.MeteoLogger
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shestikpetr.meteo.network.AddStationRequest
import com.shestikpetr.meteo.network.AuthManager
import com.shestikpetr.meteo.network.MeteoApiService
import com.shestikpetr.meteo.network.StationInfo
import com.shestikpetr.meteo.network.UpdateStationRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for station management screen
 */
data class StationManagementUiState(
    val stations: List<StationInfo> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

/**
 * ViewModel for managing weather stations using API v1
 */
@HiltViewModel
class StationManagementViewModel @Inject constructor(
    private val meteoApiService: MeteoApiService,
    private val authManager: AuthManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(StationManagementUiState())
    val uiState: StateFlow<StationManagementUiState> = _uiState.asStateFlow()

    companion object {
        private val logger = MeteoLogger.forClass(StationManagementViewModel::class)
    }

    /**
     * Load user stations from API
     */
    fun loadUserStations() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val authHeader = authManager.getAuthorizationHeader()
                if (authHeader == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Ошибка аутентификации"
                        )
                    }
                    return@launch
                }

                val response = meteoApiService.getUserStations(authHeader)

                if (response.isSuccessful && response.body()?.success == true) {
                    val stations = response.body()?.data ?: emptyList()
                    logger.d("Loaded ${stations.size} stations")

                    _uiState.update {
                        it.copy(
                            stations = stations,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    logger.e("Failed to load stations: ${response.code()}, $errorBody")

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Ошибка загрузки станций: ${response.code()}"
                        )
                    }
                }
            } catch (e: Exception) {
                logger.e("Error loading stations", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Неизвестная ошибка"
                    )
                }
            }
        }
    }

    /**
     * Add a new station to user's list
     */
    fun addStation(stationNumber: String, customName: String?) {
        if (stationNumber.length != 8 || !stationNumber.all { it.isDigit() }) {
            _uiState.update {
                it.copy(errorMessage = "Номер станции должен содержать ровно 8 цифр")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val authHeader = authManager.getAuthorizationHeader()
                if (authHeader == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Ошибка аутентификации"
                        )
                    }
                    return@launch
                }

                val request = AddStationRequest(
                    station_number = stationNumber,
                    custom_name = customName
                )

                val response = meteoApiService.addStation(authHeader, request)

                if (response.isSuccessful && response.body()?.success == true) {
                    logger.d("Successfully added station $stationNumber")

                    _uiState.update { it.copy(isLoading = false) }

                    // Reload stations to get the updated list
                    loadUserStations()
                } else {
                    val errorBody = response.errorBody()?.string()
                    logger.e("Failed to add station: ${response.code()}, $errorBody")

                    val errorMessage = when (response.code()) {
                        400 -> "Неверный формат номера станции"
                        404 -> "Станция с номером $stationNumber не найдена"
                        409 -> "Станция уже добавлена к вашему аккаунту"
                        else -> "Ошибка добавления станции: ${response.code()}"
                    }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = errorMessage
                        )
                    }
                }
            } catch (e: Exception) {
                logger.e("Error adding station", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Ошибка подключения"
                    )
                }
            }
        }
    }

    /**
     * Remove station from user's list
     */
    fun removeStation(stationNumber: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val authHeader = authManager.getAuthorizationHeader()
                if (authHeader == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Ошибка аутентификации"
                        )
                    }
                    return@launch
                }

                val response = meteoApiService.removeStation(stationNumber, authHeader)

                if (response.isSuccessful && response.body()?.success == true) {
                    logger.d("Successfully removed station $stationNumber")

                    // Remove station from current list immediately
                    _uiState.update { currentState ->
                        currentState.copy(
                            stations = currentState.stations.filter { it.station_number != stationNumber },
                            isLoading = false
                        )
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    logger.e("Failed to remove station: ${response.code()}, $errorBody")

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Ошибка удаления станции: ${response.code()}"
                        )
                    }
                }
            } catch (e: Exception) {
                logger.e("Error removing station", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Ошибка подключения"
                    )
                }
            }
        }
    }

    /**
     * Update station custom name
     */
    fun updateStationName(stationNumber: String, newName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val authHeader = authManager.getAuthorizationHeader()
                if (authHeader == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Ошибка аутентификации"
                        )
                    }
                    return@launch
                }

                // Use query parameters instead of request body
                val customName = newName.ifBlank { null }

                val response = meteoApiService.updateStation(
                    stationNumber = stationNumber,
                    authToken = authHeader,
                    customName = customName,
                    isFavorite = null // Don't change favorite status
                )

                if (response.isSuccessful && response.body()?.success == true) {
                    logger.d("Successfully updated station $stationNumber name to '$newName'")

                    // Update station in current list immediately
                    _uiState.update { currentState ->
                        currentState.copy(
                            stations = currentState.stations.map { station ->
                                if (station.station_number == stationNumber) {
                                    station.copy(
                                        custom_name = newName.ifBlank { null },
                                        display_name = newName.ifBlank { station.name }
                                    )
                                } else {
                                    station
                                }
                            },
                            isLoading = false
                        )
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    logger.e("Failed to update station: ${response.code()}, $errorBody")

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Ошибка обновления станции: ${response.code()}"
                        )
                    }
                }
            } catch (e: Exception) {
                logger.e("Error updating station", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Ошибка подключения"
                    )
                }
            }
        }
    }

    /**
     * Toggle station favorite status
     */
    fun toggleFavorite(stationNumber: String) {
        val currentStation = _uiState.value.stations.find { it.station_number == stationNumber }
        if (currentStation == null) {
            logger.w("Station $stationNumber not found in current list")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val authHeader = authManager.getAuthorizationHeader()
                if (authHeader == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Ошибка аутентификации"
                        )
                    }
                    return@launch
                }

                // Use query parameters instead of request body
                val newFavoriteStatus = !currentStation.is_favorite

                val response = meteoApiService.updateStation(
                    stationNumber = stationNumber,
                    authToken = authHeader,
                    customName = null, // Don't change name
                    isFavorite = newFavoriteStatus
                )

                if (response.isSuccessful && response.body()?.success == true) {
                    logger.d("Successfully toggled favorite for station $stationNumber")

                    // Update station in current list immediately
                    _uiState.update { currentState ->
                        currentState.copy(
                            stations = currentState.stations.map { station ->
                                if (station.station_number == stationNumber) {
                                    station.copy(is_favorite = !station.is_favorite)
                                } else {
                                    station
                                }
                            },
                            isLoading = false
                        )
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    logger.e("Failed to toggle favorite: ${response.code()}, $errorBody")

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Ошибка обновления избранного: ${response.code()}"
                        )
                    }
                }
            } catch (e: Exception) {
                logger.e("Error toggling favorite", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Ошибка подключения"
                    )
                }
            }
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}