package com.shestikpetr.meteo.repository.impl

import android.util.Log
import com.shestikpetr.meteo.network.ApiResponse
import com.shestikpetr.meteo.network.AuthManager
import com.shestikpetr.meteo.network.MeteoApiService
import com.shestikpetr.meteo.network.SensorDataPoint
import com.shestikpetr.meteo.repository.interfaces.SensorDataRepository
import com.shestikpetr.meteo.utils.RetryPolicy
import kotlinx.coroutines.delay
import retrofit2.HttpException
import retrofit2.Response
import java.net.ProtocolException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Network implementation of SensorDataRepository interface for API v1.
 * Handles sensor data operations with proper error handling and retry logic.
 */
@Singleton
class NetworkSensorDataRepository @Inject constructor(
    private val meteoApiService: MeteoApiService,
    private val authManager: AuthManager,
    private val retryPolicy: RetryPolicy
) : SensorDataRepository {

    companion object {
        private const val TAG = "NetworkSensorDataRepo"
    }

    override suspend fun getSensorData(
        stationNumber: String,
        parameter: String,
        startTime: Long?,
        endTime: Long?
    ): List<SensorDataPoint> {
        return when (val result = retryPolicy.executeWithDefaultRetry { attempt ->
                val authHeader = authManager.getAuthorizationHeader()
                    ?: throw SecurityException("No valid authentication token")

                val response = meteoApiService.getSensorData(
                    stationNumber = stationNumber,
                    parameter = parameter,
                    startTime = startTime,
                    endTime = endTime,
                    limit = null,
                    authToken = authHeader
                )

                handleApiResponse(response, "getSensorData")
        }) {
            is com.shestikpetr.meteo.utils.RetryPolicy.RetryResult.Success -> result.data
            is com.shestikpetr.meteo.utils.RetryPolicy.RetryResult.Failure -> throw result.exception
        }
    }

    override suspend fun getLatestSensorData(stationNumber: String, parameter: String): Double {
        return retryPolicy.executeWithFallback(
            fallbackValue = -999.0
        ) { attempt ->
            val authHeader = authManager.getAuthorizationHeader()
                ?: throw SecurityException("No valid authentication token")

            val response = meteoApiService.getLatestSensorData(
                stationNumber = stationNumber,
                parameter = parameter,
                authToken = authHeader
            )

            val sensorData = handleApiResponse(response, "getLatestSensorData")
            sensorData.value
        }
    }

    override suspend fun getMultiParameterData(
        stationNumber: String,
        parameters: List<String>,
        startTime: Long?,
        endTime: Long?
    ): Map<String, List<SensorDataPoint>> {
        val result = mutableMapOf<String, List<SensorDataPoint>>()

        for (parameter in parameters) {
            try {
                val data = getSensorData(stationNumber, parameter, startTime, endTime)
                result[parameter] = data

                // Small delay to avoid overwhelming the server
                delay(100)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to get data for parameter $parameter: ${e.message}")
                result[parameter] = emptyList()
            }
        }

        return result
    }

    override suspend fun getLatestMultiParameterData(
        stationNumber: String,
        parameters: List<String>
    ): Map<String, Double> {
        // Use the new bulk endpoint if available
        return try {
            val authHeader = authManager.getAuthorizationHeader()
                ?: throw SecurityException("No valid authentication token")

            val response = meteoApiService.getLatestStationData(
                stationNumber = stationNumber,
                authToken = authHeader
            )

            val allData = handleApiResponse(response, "getLatestStationData")

            // Filter to requested parameters and convert to map
            allData.filter { it.parameter in parameters }
                .associate { it.parameter to it.value }

        } catch (e: Exception) {
            Log.w(TAG, "Bulk latest data failed, falling back to individual requests: ${e.message}")

            // Fallback: individual requests
            val result = mutableMapOf<String, Double>()
            for (parameter in parameters) {
                try {
                    val value = getLatestSensorData(stationNumber, parameter)
                    result[parameter] = value
                    delay(50) // Small delay between requests
                } catch (ex: Exception) {
                    Log.w(TAG, "Failed to get latest data for parameter $parameter: ${ex.message}")
                    result[parameter] = -999.0
                }
            }
            result
        }
    }

    override suspend fun isDataAvailable(stationNumber: String, parameter: String): Boolean {
        return try {
            getLatestSensorData(stationNumber, parameter)
            true
        } catch (e: Exception) {
            Log.d(TAG, "Data not available for $stationNumber/$parameter: ${e.message}")
            false
        }
    }

    override suspend fun getDataTimeRange(
        stationNumber: String,
        parameter: String
    ): Pair<Long?, Long?> {
        return try {
            // Get recent data to determine time range
            val recentData = getSensorData(
                stationNumber = stationNumber,
                parameter = parameter,
                startTime = null,
                endTime = null
            )

            if (recentData.isNotEmpty()) {
                val times = recentData.map { it.time }
                Pair(times.minOrNull(), times.maxOrNull())
            } else {
                Pair(null, null)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get time range for $stationNumber/$parameter: ${e.message}")
            Pair(null, null)
        }
    }

    override suspend fun getAllStationsLatestData(): Map<String, Map<String, Double>> {
        return try {
            val authHeader = authManager.getAuthorizationHeader()
                ?: throw SecurityException("No valid authentication token")

            val response = meteoApiService.getAllStationsLatestData(authToken = authHeader)
            val allStationsData = handleApiResponse(response, "getAllStationsLatestData")

            // Convert from Map<String, List<SensorDataPoint>> to Map<String, Map<String, Double>>
            allStationsData.mapValues { (_, sensorDataList) ->
                sensorDataList.associate { it.parameter to it.value }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to get all stations latest data: ${e.message}")
            emptyMap()
        }
    }

    /**
     * Handle API response and extract data from the success/data wrapper
     */
    private fun <T> handleApiResponse(response: Response<ApiResponse<T>>, operation: String): T {
        if (!response.isSuccessful) {
            throw HttpException(response)
        }

        val apiResponse = response.body()
        if (apiResponse?.success != true) {
            throw RuntimeException("API call failed: $operation")
        }

        return apiResponse.data ?: throw RuntimeException("API response data is null: $operation")
    }
}