package com.shestikpetr.meteo.repository.impl

import com.shestikpetr.meteo.cache.SensorDataCache
import com.shestikpetr.meteo.common.constants.MeteoConstants
import com.shestikpetr.meteo.common.logging.MeteoLogger
import com.shestikpetr.meteo.network.ApiResponse
import com.shestikpetr.meteo.network.AuthManager
import com.shestikpetr.meteo.network.MeteoApiService
import com.shestikpetr.meteo.network.SensorDataPoint
import com.shestikpetr.meteo.repository.interfaces.SensorDataRepository
import com.shestikpetr.meteo.utils.RetryPolicy
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import retrofit2.HttpException
import retrofit2.Response
import java.net.ProtocolException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Network implementation of SensorDataRepository interface for API v1.
 * Handles sensor data operations with unified error handling, retry logic, and centralized constants.
 */
@Singleton
class NetworkSensorDataRepository @Inject constructor(
    private val meteoApiService: MeteoApiService,
    private val authManager: AuthManager,
    private val retryPolicy: RetryPolicy,
    private val sensorDataCache: SensorDataCache
) : SensorDataRepository {

    private val logger = MeteoLogger.forClass(NetworkSensorDataRepository::class)

    override suspend fun getSensorData(
        stationNumber: String,
        parameter: String,
        startTime: Long?,
        endTime: Long?
    ): List<SensorDataPoint> {
        return when (val result = retryPolicy.executeWithDefaultRetry { attempt ->
                val authHeader = authManager.getAuthorizationHeader()
                    ?: throw SecurityException("No valid authentication token")

                val response = meteoApiService.getParameterHistory(
                    stationNumber = stationNumber,
                    parameterCode = parameter,
                    startTime = startTime,
                    endTime = endTime,
                    limit = null,
                    authToken = authHeader
                )

                // NOTE: This endpoint returns ParameterHistoryResponse directly, not wrapped in ApiResponse
                if (!response.isSuccessful) {
                    throw HttpException(response)
                }

                val historyResponse = response.body()
                    ?: throw RuntimeException("Empty response body from getParameterHistory")

                if (!historyResponse.success) {
                    throw RuntimeException("API call failed: getParameterHistory returned success=false")
                }

                // Convert HistoryDataPoint to SensorDataPoint for backward compatibility
                historyResponse.data.map { SensorDataPoint(time = it.time, value = it.value) }
        }) {
            is com.shestikpetr.meteo.utils.RetryPolicy.RetryResult.Success -> result.data
            is com.shestikpetr.meteo.utils.RetryPolicy.RetryResult.Failure -> throw result.exception
        }
    }

    override suspend fun getLatestSensorData(stationNumber: String, parameter: String): Double {
        // Check cache first
        val cachedValue = sensorDataCache.getValue(stationNumber, parameter)
        if (cachedValue != null && sensorDataCache.isValidValue(cachedValue)) {
            return cachedValue
        }

        return retryPolicy.executeWithFallback(
            fallbackValue = MeteoConstants.Data.UNAVAILABLE_VALUE
        ) { attempt ->
            val authHeader = authManager.getAuthorizationHeader()
                ?: throw SecurityException("No valid authentication token")

            // Log request details for debugging
            logger.d("Making getLatestSensorData request: stationNumber='$stationNumber', parameter='$parameter'")

            val response = meteoApiService.getLatestStationData(
                stationNumber = stationNumber,
                authToken = authHeader
            )

            val stationData = handleApiResponse(response, "getLatestStationData")

            // Find the requested parameter in the response
            val parameterData = stationData.parameters.find { it.code == parameter }
            val value = parameterData?.value ?: MeteoConstants.Data.UNAVAILABLE_VALUE

            // Cache the result if valid
            if (value != MeteoConstants.Data.UNAVAILABLE_VALUE) {
                sensorDataCache.putValue(stationNumber, parameter, value)
            }
            value
        }
    }

    override suspend fun getMultiParameterData(
        stationNumber: String,
        parameters: List<String>,
        startTime: Long?,
        endTime: Long?
    ): Map<String, List<SensorDataPoint>> = coroutineScope {
        val deferredResults = parameters.map { parameter ->
            async {
                try {
                    parameter to getSensorData(stationNumber, parameter, startTime, endTime)
                } catch (e: Exception) {
                    logger.w("Failed to get data for parameter $parameter: ${e.message}")
                    parameter to emptyList<SensorDataPoint>()
                }
            }
        }

        deferredResults.awaitAll().toMap()
    }

    override suspend fun getLatestMultiParameterData(
        stationNumber: String,
        parameters: List<String>
    ): Map<String, Double> {
        return try {
            val authHeader = authManager.getAuthorizationHeader()
                ?: throw SecurityException("No valid authentication token")

            val response = meteoApiService.getLatestStationData(
                stationNumber = stationNumber,
                authToken = authHeader
            )

            val stationData = handleApiResponse(response, "getLatestStationData")

            // Convert List<ParameterValue> to Map<String, Double>
            val parameterMap = stationData.parameters.associate { param ->
                param.code to (param.value ?: MeteoConstants.Data.UNAVAILABLE_VALUE)
            }

            // Cache all received values
            parameterMap.forEach { (paramCode, value) ->
                if (paramCode in parameters && value != MeteoConstants.Data.UNAVAILABLE_VALUE) {
                    sensorDataCache.putValue(stationNumber, paramCode, value)
                }
            }

            // Filter to requested parameters only
            parameterMap.filterKeys { it in parameters }

        } catch (e: Exception) {
            logger.w("Bulk latest data failed, falling back to individual requests: ${e.message}")

            // Fallback: parallel individual requests
            coroutineScope {
                val deferredResults = parameters.map { parameter ->
                    async {
                        try {
                            parameter to getLatestSensorData(stationNumber, parameter)
                        } catch (ex: Exception) {
                            logger.w("Failed to get latest data for parameter $parameter: ${ex.message}")
                            parameter to MeteoConstants.Data.UNAVAILABLE_VALUE
                        }
                    }
                }
                deferredResults.awaitAll().toMap()
            }
        }
    }

    override suspend fun isDataAvailable(stationNumber: String, parameter: String): Boolean {
        return try {
            getLatestSensorData(stationNumber, parameter)
            true
        } catch (e: Exception) {
            logger.d("Data not available for $stationNumber/$parameter: ${e.message}")
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
            logger.w("Failed to get time range for $stationNumber/$parameter: ${e.message}")
            Pair(null, null)
        }
    }

    override fun isDataUnavailable(value: Double): Boolean {
        return value == MeteoConstants.Data.UNAVAILABLE_VALUE
    }

    override suspend fun getAllStationsLatestData(): Map<String, Map<String, Double>> {
        return try {
            val authHeader = authManager.getAuthorizationHeader()
                ?: throw SecurityException("No valid authentication token")

            val response = meteoApiService.getLatestDataAllStations(authToken = authHeader)
            val allStationsData = handleApiResponse(response, "getLatestDataAllStations")

            // Convert from List<StationLatestDataResponse> to Map<String, Map<String, Double>>
            allStationsData.associate { stationData ->
                val parameterMap = stationData.parameters.associate { param ->
                    param.code to (param.value ?: MeteoConstants.Data.UNAVAILABLE_VALUE)
                }
                stationData.station_number to parameterMap
            }

        } catch (e: Exception) {
            logger.e("Failed to get all stations latest data: ${e.message}")
            emptyMap()
        }
    }

    override suspend fun getAllStationsWithLocationAndData(): Pair<List<com.shestikpetr.meteo.data.StationWithLocation>, Map<String, Map<String, Double>>> {
        return try {
            val authHeader = authManager.getAuthorizationHeader()
                ?: throw SecurityException("No valid authentication token")

            val response = meteoApiService.getLatestDataAllStations(authToken = authHeader)
            val allStationsData = handleApiResponse(response, "getLatestDataAllStations")

            logger.d("Получены данные для ${allStationsData.size} станций из /api/v1/data/latest")

            // Convert to StationWithLocation list
            val stations = allStationsData.mapNotNull { stationData ->
                // Skip stations without coordinates
                if (stationData.latitude == null || stationData.longitude == null) {
                    logger.w("Пропущена станция ${stationData.station_number} без координат")
                    return@mapNotNull null
                }

                logger.d("Станция ${stationData.station_number}: lat=${stationData.latitude}, lon=${stationData.longitude}, custom_name=${stationData.custom_name}")

                com.shestikpetr.meteo.data.StationWithLocation(
                    stationNumber = stationData.station_number,
                    name = stationData.station_number, // Fallback to number
                    latitude = stationData.latitude,
                    longitude = stationData.longitude,
                    customName = stationData.custom_name,
                    location = stationData.location,
                    isFavorite = stationData.is_favorite
                )
            }

            // Convert to parameter data map
            val dataMap = allStationsData.associate { stationData ->
                val parameterMap = stationData.parameters.associate { param ->
                    param.code to (param.value ?: MeteoConstants.Data.UNAVAILABLE_VALUE)
                }
                stationData.station_number to parameterMap
            }

            logger.d("Создано ${stations.size} станций с координатами")

            Pair(stations, dataMap)

        } catch (e: Exception) {
            logger.e("Failed to get all stations with location and data: ${e.message}")
            Pair(emptyList(), emptyMap())
        }
    }

    /**
     * Handle API response and extract data from the success/data wrapper
     */
    private fun <T> handleApiResponse(response: Response<ApiResponse<T>>, operation: String): T {
        if (!response.isSuccessful) {
            // Log detailed error information for HTTP 422 (UNPROCESSABLE ENTITY)
            if (response.code() == 422) {
                val errorBody = response.errorBody()?.string()
                logger.e("HTTP 422 UNPROCESSABLE ENTITY in $operation: $errorBody")
            }
            throw HttpException(response)
        }

        val apiResponse = response.body()
        if (apiResponse?.success != true) {
            throw RuntimeException("API call failed: $operation")
        }

        return apiResponse.data ?: throw RuntimeException("API response data is null: $operation")
    }
}