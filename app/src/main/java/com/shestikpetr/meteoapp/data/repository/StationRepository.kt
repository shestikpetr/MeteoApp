package com.shestikpetr.meteoapp.data.repository

import com.shestikpetr.meteoapp.data.api.RetrofitClient
import com.shestikpetr.meteoapp.data.model.ParameterHistoryResponse
import com.shestikpetr.meteoapp.data.model.ParameterMetadata
import com.shestikpetr.meteoapp.data.model.StationDataResponse
import com.shestikpetr.meteoapp.data.model.UserStationRequest
import com.shestikpetr.meteoapp.data.model.UserStationResponse
import com.shestikpetr.meteoapp.data.model.UserStationUpdateRequest
import com.shestikpetr.meteoapp.util.TokenStore
import retrofit2.Response

class StationRepository(private val tokenStore: TokenStore) {

    private val api = RetrofitClient.apiService

    private suspend fun getAuthHeader(): String {
        val token = tokenStore.getAccessToken() ?: ""
        return "Bearer $token"
    }

    suspend fun getUserStations(): Result<List<UserStationResponse>> = runCatching {
        val response = api.getUserStations(getAuthHeader())
        unwrap(response) { it.data }.orEmpty()
    }

    suspend fun getStationParameters(stationNumber: String): Result<List<ParameterMetadata>> = runCatching {
        val response = api.getStationParameters(getAuthHeader(), stationNumber)
        unwrap(response) { it.data }?.parameters ?: emptyList()
    }

    suspend fun getStationData(stationNumber: String): Result<StationDataResponse> = runCatching {
        val response = api.getStationData(getAuthHeader(), stationNumber)
        unwrap(response) { it.data } ?: error("No data")
    }

    suspend fun getLatestDataTime(stationNumber: String): Result<Long> = runCatching {
        getStationData(stationNumber).getOrThrow().time ?: error("No data")
    }

    suspend fun addStation(stationNumber: String): Result<UserStationResponse> = runCatching {
        val response = api.addStation(getAuthHeader(), UserStationRequest(stationNumber))
        unwrap(response) { it.data } ?: error("Не удалось добавить станцию")
    }

    suspend fun deleteStation(stationNumber: String): Result<Unit> = runCatching {
        val response = api.deleteStation(getAuthHeader(), stationNumber)
        val body = response.body()
        if (!response.isSuccessful || body == null || !body.success) {
            error(body?.error ?: response.errorBody()?.string() ?: "Не удалось удалить станцию")
        }
    }

    suspend fun renameStation(stationNumber: String, newName: String): Result<Unit> = runCatching {
        val response = api.renameStation(
            getAuthHeader(),
            stationNumber,
            UserStationUpdateRequest(newName)
        )
        val body = response.body()
        if (!response.isSuccessful || body == null || !body.success) {
            error(body?.error ?: response.errorBody()?.string() ?: "Не удалось переименовать станцию")
        }
    }

    suspend fun getParameterHistory(
        stationNumber: String,
        parameterCode: Int,
        startTime: Long? = null,
        endTime: Long? = null
    ): Result<ParameterHistoryResponse> = runCatching {
        val response = api.getParameterHistory(
            token = getAuthHeader(),
            stationNumber = stationNumber,
            parameterCode = parameterCode,
            startTime = startTime,
            endTime = endTime
        )
        unwrap(response) { it.data } ?: error("Failed to load history")
    }

    private fun <B, T> unwrap(response: Response<B>, extractor: (B) -> T?): T? {
        val body = response.body()
        if (!response.isSuccessful || body == null) {
            error(response.errorBody()?.string() ?: "Request failed")
        }
        val errorField = (body as? HasError)?.error
        if (errorField != null) error(errorField)
        return extractor(body)
    }

    private interface HasError { val error: String? }
}
