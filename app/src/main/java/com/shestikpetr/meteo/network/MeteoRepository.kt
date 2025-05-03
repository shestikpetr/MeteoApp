package com.shestikpetr.meteo.network

import javax.inject.Inject

// Репозиторий метеоданных
interface MeteoRepository {
    suspend fun getSensorData(
        complexId: String,
        parameter: String,
        startTime: Long? = null,
        endTime: Long? = null
    ): List<SensorDataPoint>

    suspend fun getLatestSensorData(
        complexId: String,
        parameter: String
    ): Double

    suspend fun getStationParameters(
        complexId: String
    ): List<ParameterInfo>

    suspend fun getUserStations(): List<StationInfo>

    suspend fun getParametersMetadata(): Map<String, ParameterMetadata>

    suspend fun getParameterMetadata(parameterId: String): ParameterMetadata
}

// Реализация репозитория метеоданных
class NetworkMeteoRepository @Inject constructor(
    private val meteoApiService: MeteoApiService,
    private val authManager: AuthManager
) : MeteoRepository {
    override suspend fun getSensorData(
        complexId: String,
        parameter: String,
        startTime: Long?,
        endTime: Long?
    ): List<SensorDataPoint> {
        val token = "Basic ${authManager.getAuthToken()}"
        return meteoApiService.getSensorData(complexId, parameter, startTime, endTime, token)
    }

    override suspend fun getLatestSensorData(complexId: String, parameter: String): Double {
        val token = "Basic ${authManager.getAuthToken()}"
        return meteoApiService.getLatestSensorData(complexId, parameter, token)
    }

    override suspend fun getStationParameters(complexId: String): List<ParameterInfo> {
        val token = "Basic ${authManager.getAuthToken()}"
        return meteoApiService.getStationParameters(complexId, token)
    }

    override suspend fun getUserStations(): List<StationInfo> {
        val token = "Basic ${authManager.getAuthToken()}"
        return meteoApiService.getUserStations(token)
    }

    override suspend fun getParametersMetadata(): Map<String, ParameterMetadata> {
        val token = "Basic ${authManager.getAuthToken()}"
        return meteoApiService.getParametersMetadata(token)
    }

    override suspend fun getParameterMetadata(parameterId: String): ParameterMetadata {
        val token = "Basic ${authManager.getAuthToken()}"
        return meteoApiService.getParameterMetadata(parameterId, token)
    }
}