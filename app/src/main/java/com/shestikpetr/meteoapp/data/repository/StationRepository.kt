package com.shestikpetr.meteoapp.data.repository

import com.shestikpetr.meteoapp.data.api.RetrofitClient
import com.shestikpetr.meteoapp.data.model.AddStationRequest
import com.shestikpetr.meteoapp.data.model.ParameterHistoryResponse
import com.shestikpetr.meteoapp.data.model.ParameterMetadata
import com.shestikpetr.meteoapp.data.model.RenameStationRequest
import com.shestikpetr.meteoapp.data.model.UserStationResponse
import com.shestikpetr.meteoapp.util.TokenManager

class StationRepository(private val tokenManager: TokenManager) {

    private val api = RetrofitClient.apiService

    private suspend fun getAuthHeader(): String {
        val token = tokenManager.getAccessToken() ?: ""
        return "Bearer $token"
    }

    suspend fun getUserStations(): Result<List<UserStationResponse>> {
        return try {
            val response = api.getUserStations(getAuthHeader())
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.data)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to load stations"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getStationParameters(stationNumber: String): Result<List<ParameterMetadata>> {
        return try {
            val response = api.getStationParameters(getAuthHeader(), stationNumber)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.parameters)
            } else {
                Result.failure(Exception("Failed to load parameters"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLatestParameterValue(
        stationNumber: String,
        parameterCode: String
    ): Result<Pair<Double, String?>> {
        return try {
            val response = api.getParameterHistory(
                token = getAuthHeader(),
                stationNumber = stationNumber,
                parameterCode = parameterCode
            )
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val lastDataPoint = body.data.lastOrNull()
                if (lastDataPoint != null) {
                    Result.success(Pair(lastDataPoint.value, body.parameter.unit))
                } else {
                    Result.failure(Exception("No data"))
                }
            } else {
                Result.failure(Exception("Failed to load data"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLatestDataTime(
        stationNumber: String,
        parameterCode: String
    ): Result<Long> {
        return try {
            val response = api.getParameterHistory(
                token = getAuthHeader(),
                stationNumber = stationNumber,
                parameterCode = parameterCode
            )
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val lastDataPoint = body.data.maxByOrNull { it.time }
                if (lastDataPoint != null) {
                    Result.success(lastDataPoint.time)
                } else {
                    Result.failure(Exception("No data"))
                }
            } else {
                Result.failure(Exception("Failed to load data"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addStation(stationNumber: String): Result<UserStationResponse> {
        return try {
            val response = api.addStation(getAuthHeader(), AddStationRequest(stationNumber))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.data)
            } else {
                val error = response.errorBody()?.string() ?: "Не удалось добавить станцию"
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteStation(stationId: String): Result<Unit> {
        return try {
            val response = api.deleteStation(getAuthHeader(), stationId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val error = response.errorBody()?.string() ?: "Не удалось удалить станцию"
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun renameStation(stationId: String, newName: String): Result<Unit> {
        return try {
            val response = api.renameStation(getAuthHeader(), stationId, RenameStationRequest(newName))
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val error = response.errorBody()?.string() ?: "Не удалось переименовать станцию"
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getParameterHistory(
        stationNumber: String,
        parameterCode: String,
        startTime: Long? = null,
        endTime: Long? = null
    ): Result<ParameterHistoryResponse> {
        return try {
            val response = api.getParameterHistory(
                token = getAuthHeader(),
                stationNumber = stationNumber,
                parameterCode = parameterCode,
                startTime = startTime,
                endTime = endTime
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to load history"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
