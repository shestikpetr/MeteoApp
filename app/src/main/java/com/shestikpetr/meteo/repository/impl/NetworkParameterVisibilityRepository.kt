package com.shestikpetr.meteo.repository.impl

import com.shestikpetr.meteo.common.logging.MeteoLogger
import com.shestikpetr.meteo.network.*
import com.shestikpetr.meteo.repository.interfaces.ParameterVisibilityRepository
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Network implementation of ParameterVisibilityRepository for FastAPI v1.
 * Handles parameter visibility operations with unified error handling and logging.
 */
@Singleton
class NetworkParameterVisibilityRepository @Inject constructor(
    private val meteoApiService: MeteoApiService,
    private val authManager: AuthManager
) : ParameterVisibilityRepository {

    private val logger = MeteoLogger.forClass(NetworkParameterVisibilityRepository::class)

    override suspend fun getStationParametersWithVisibility(stationNumber: String): List<ParameterVisibilityInfo> {
        val authHeader = authManager.getAuthorizationHeader()
            ?: throw SecurityException("No valid authentication token")

        val response = meteoApiService.getStationParameters(
            stationNumber = stationNumber,
            authToken = authHeader
        )

        return handleApiResponse(response, "getStationParameters")
    }

    override suspend fun updateParameterVisibility(
        stationNumber: String,
        parameterCode: String,
        isVisible: Boolean
    ): Boolean {
        return try {
            val authHeader = authManager.getAuthorizationHeader()
                ?: throw SecurityException("No valid authentication token")

            val request = UpdateParameterVisibilityRequest(is_visible = isVisible)
            val response = meteoApiService.updateParameterVisibility(
                stationNumber = stationNumber,
                parameterCode = parameterCode,
                authToken = authHeader,
                request = request
            )

            val result = handleApiResponse(response, "updateParameterVisibility")
            logger.d("Updated parameter $parameterCode visibility to $isVisible: ${result.success}")
            result.success

        } catch (e: Exception) {
            logger.e("Failed to update parameter visibility: ${e.message}")
            false
        }
    }

    override suspend fun updateMultipleParametersVisibility(
        stationNumber: String,
        parameterUpdates: Map<String, Boolean>
    ): Pair<Int, Int> {
        return try {
            val authHeader = authManager.getAuthorizationHeader()
                ?: throw SecurityException("No valid authentication token")

            val updates = parameterUpdates.map { (code, visible) ->
                ParameterVisibilityUpdate(code = code, visible = visible)
            }

            val request = BulkUpdateParametersRequest(parameters = updates)
            val response = meteoApiService.updateMultipleParametersVisibility(
                stationNumber = stationNumber,
                authToken = authHeader,
                request = request
            )

            val result = handleApiResponse(response, "updateMultipleParametersVisibility")
            logger.d("Bulk update: ${result.updated} of ${result.total} parameters updated")
            Pair(result.updated, result.total)

        } catch (e: Exception) {
            logger.e("Failed to update multiple parameters visibility: ${e.message}")
            Pair(0, parameterUpdates.size)
        }
    }

    override suspend fun getVisibleParameters(stationNumber: String): List<String> {
        return try {
            val allParameters = getStationParametersWithVisibility(stationNumber)
            allParameters.filter { it.is_visible }.map { it.code }
        } catch (e: Exception) {
            logger.e("Failed to get visible parameters: ${e.message}")
            emptyList()
        }
    }

    override suspend fun isParameterVisible(stationNumber: String, parameterCode: String): Boolean {
        return try {
            val allParameters = getStationParametersWithVisibility(stationNumber)
            allParameters.find { it.code == parameterCode }?.is_visible ?: false
        } catch (e: Exception) {
            logger.e("Failed to check parameter visibility: ${e.message}")
            false
        }
    }

    override suspend fun showAllParameters(stationNumber: String): Pair<Int, Int> {
        return try {
            val allParameters = getStationParametersWithVisibility(stationNumber)
            val updates = allParameters.associate { it.code to true }
            updateMultipleParametersVisibility(stationNumber, updates)
        } catch (e: Exception) {
            logger.e("Failed to show all parameters: ${e.message}")
            Pair(0, 0)
        }
    }

    override suspend fun hideAllParameters(stationNumber: String): Pair<Int, Int> {
        return try {
            val allParameters = getStationParametersWithVisibility(stationNumber)
            val updates = allParameters.associate { it.code to false }
            updateMultipleParametersVisibility(stationNumber, updates)
        } catch (e: Exception) {
            logger.e("Failed to hide all parameters: ${e.message}")
            Pair(0, 0)
        }
    }

    /**
     * Handle API response and extract data from the success/data wrapper
     */
    private fun <T> handleApiResponse(response: Response<ApiResponse<T>>, operation: String): T {
        if (!response.isSuccessful) {
            throw retrofit2.HttpException(response)
        }

        val apiResponse = response.body()
        if (apiResponse?.success != true) {
            throw RuntimeException("API call failed: $operation")
        }

        return apiResponse.data ?: throw RuntimeException("API response data is null: $operation")
    }
}
