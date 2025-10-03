package com.shestikpetr.meteo.network

import retrofit2.Response
import retrofit2.http.*

/**
 * Generic API response wrapper for all API v1 endpoints
 */
data class ApiResponse<T>(
    val success: Boolean,
    val data: T?
)

/**
 * API error response format
 */
data class ApiError(
    val error: String
)

/**
 * MeteoApp API v1 Service Interface
 * Base URL is configured in local.properties via BuildConfig
 */
interface MeteoApiService {

    // ===================== AUTH ENDPOINTS =====================

    /**
     * User login
     */
    @POST("auth/login")
    suspend fun login(@Body credentials: UserCredentials): Response<ApiResponse<AuthTokens>>

    /**
     * User registration
     */
    @POST("auth/register")
    suspend fun register(@Body registrationData: UserRegistrationData): Response<ApiResponse<AuthTokens>>

    /**
     * Refresh access token using refresh token
     */
    @POST("auth/refresh")
    suspend fun refreshToken(@Header("Authorization") refreshToken: String): Response<ApiResponse<RefreshTokenData>>

    /**
     * Get current user information
     */
    @GET("auth/me")
    suspend fun getCurrentUser(@Header("Authorization") authToken: String): Response<ApiResponse<UserInfo>>


    // ===================== STATION ENDPOINTS =====================

    /**
     * Get all user stations
     */
    @GET("stations")
    suspend fun getUserStations(@Header("Authorization") authToken: String): Response<ApiResponse<List<StationInfo>>>

    /**
     * Add station to user
     */
    @POST("stations")
    suspend fun addStation(
        @Header("Authorization") authToken: String,
        @Body stationData: AddStationRequest
    ): Response<ApiResponse<AddStationResponse>>

    /**
     * Update station settings
     */
    @PATCH("stations/{station_number}")
    suspend fun updateStation(
        @Path("station_number") stationNumber: String,
        @Header("Authorization") authToken: String,
        @Query("custom_name") customName: String?,
        @Query("is_favorite") isFavorite: Boolean?
    ): Response<ApiResponse<SuccessResponse>>

    /**
     * Remove station from user
     */
    @DELETE("stations/{station_number}")
    suspend fun removeStation(
        @Path("station_number") stationNumber: String,
        @Header("Authorization") authToken: String
    ): Response<ApiResponse<Unit>>

    /**
     * Get station parameters with visibility info
     */
    @GET("stations/{station_number}/parameters")
    suspend fun getStationParameters(
        @Path("station_number") stationNumber: String,
        @Header("Authorization") authToken: String
    ): Response<ApiResponse<List<ParameterVisibilityInfo>>>

    /**
     * Update visibility of a single parameter
     */
    @PATCH("stations/{station_number}/parameters/{parameter_code}")
    suspend fun updateParameterVisibility(
        @Path("station_number") stationNumber: String,
        @Path("parameter_code") parameterCode: String,
        @Header("Authorization") authToken: String,
        @Body request: UpdateParameterVisibilityRequest
    ): Response<ApiResponse<UpdateParameterVisibilityResponse>>

    /**
     * Update visibility of multiple parameters
     */
    @PATCH("stations/{station_number}/parameters")
    suspend fun updateMultipleParametersVisibility(
        @Path("station_number") stationNumber: String,
        @Header("Authorization") authToken: String,
        @Body request: BulkUpdateParametersRequest
    ): Response<ApiResponse<BulkUpdateParametersResponse>>

    // ===================== SENSOR DATA ENDPOINTS =====================

    /**
     * Get latest data from all user stations (MAIN ENDPOINT FOR MOBILE APP)
     * Returns all stations with location and latest values of ONLY visible parameters
     */
    @GET("data/latest")
    suspend fun getLatestDataAllStations(
        @Header("Authorization") authToken: String
    ): Response<ApiResponse<List<StationLatestDataResponse>>>

    /**
     * Get latest data from one station
     * Returns latest values of ONLY visible parameters
     */
    @GET("data/{station_number}/latest")
    suspend fun getLatestStationData(
        @Path("station_number") stationNumber: String,
        @Header("Authorization") authToken: String
    ): Response<ApiResponse<StationLatestDataResponse>>

    /**
     * Get historical data for a parameter
     * User selects a parameter on the station to view history
     * Parameter must be visible to the user
     * NOTE: This endpoint does NOT use ApiResponse wrapper - returns ParameterHistoryResponse directly
     */
    @GET("data/{station_number}/{parameter_code}/history")
    suspend fun getParameterHistory(
        @Path("station_number") stationNumber: String,
        @Path("parameter_code") parameterCode: String,
        @Query("start_time") startTime: Long?,
        @Query("end_time") endTime: Long?,
        @Query("limit") limit: Int?,
        @Header("Authorization") authToken: String
    ): Response<ParameterHistoryResponse>


}

// ===================== AUTH DATA MODELS =====================

/**
 * User login credentials
 */
data class UserCredentials(
    val username: String,
    val password: String
)

/**
 * User registration data
 */
data class UserRegistrationData(
    val username: String,
    val email: String,
    val password: String
)

/**
 * Authentication tokens response
 */
data class AuthTokens(
    val user_id: String,  // Changed from Int to String to handle JWT "sub" field correctly
    val access_token: String,
    val refresh_token: String
)

/**
 * Refresh token data (only contains access token)
 */
data class RefreshTokenData(
    val access_token: String
)

/**
 * Current user information
 */
data class UserInfo(
    val id: Int,
    val username: String,
    val email: String,
    val role: String,
    val is_active: Boolean
)

// ===================== STATION DATA MODELS =====================

/**
 * Station information from API v1
 */
data class StationInfo(
    val id: Int,
    val station_number: String,         // 8-digit station number
    val name: String,                   // System name
    val custom_name: String?,           // User custom name
    val display_name: String,           // Display name (custom_name or name)
    val location: String,
    val latitude: String,               // Coordinates as strings from API
    val longitude: String,
    val is_favorite: Boolean,           // User favorite flag (1/0 from API)
    val is_active: Boolean,             // Station active status (1/0 from API)
    val parameters: List<StationParameter>,  // Array of parameter objects
    val user_station_id: Int,           // User station relationship ID
    val created_at: String?,            // Creation timestamp
    val updated_at: String?             // Update timestamp
) {
    /**
     * Convert latitude string to Double safely
     */
    fun getLatitudeDouble(): Double = latitude.toDoubleOrNull() ?: 0.0

    /**
     * Convert longitude string to Double safely
     */
    fun getLongitudeDouble(): Double = longitude.toDoubleOrNull() ?: 0.0

    /**
     * Get available parameter codes as strings
     */
    fun getParameterCodes(): List<String> = parameters.map { it.parameter_code }

    /**
     * Get only active parameters
     */
    fun getActiveParameters(): List<StationParameter> = parameters.filter { it.isActive() }

    /**
     * Get active parameter codes as strings
     */
    fun getActiveParameterCodes(): List<String> = getActiveParameters().map { it.parameter_code }

    /**
     * Convert station parameters to ParameterInfo format for compatibility
     */
    fun getParameterInfoList(): List<ParameterInfo> = getActiveParameters().map { stationParam ->
        ParameterInfo(
            code = stationParam.parameter_code,
            name = stationParam.name,
            unit = stationParam.unit,
            description = stationParam.description,
            category = stationParam.category
        )
    }
}

/**
 * Station parameter information from API v1 (/stations response)
 */
data class StationParameter(
    val id: Int,
    val station_id: Int,
    val parameter_code: String,         // e.g., "4402"
    val name: String,                   // e.g., "Температура воздуха"
    val description: String,            // e.g., "Температура воздуха на высоте 2 м"
    val unit: String,                   // e.g., "°C"
    val category: String,               // e.g., "temperature"
    val is_active: Int                  // 1/0 from API
) {
    /**
     * Check if parameter is active (API returns 1/0, convert to boolean)
     */
    fun isActive(): Boolean = is_active == 1
}

/**
 * Request to add station to user
 */
data class AddStationRequest(
    val station_number: String,    // 8-digit station number
    val custom_name: String?       // Optional custom name
)

/**
 * Response when adding station
 */
data class AddStationResponse(
    val user_station_id: Int,
    val station_number: String,
    val name: String,
    val parameters: List<String>
)

/**
 * Request to update station settings (unused, now using query params)
 */
@Deprecated("Use query parameters instead")
data class UpdateStationRequest(
    val custom_name: String?,      // Update custom name
    val is_favorite: Boolean?      // Update favorite status
)

/**
 * Success response for simple operations
 */
data class SuccessResponse(
    val success: Boolean
)

// ===================== SENSOR DATA MODELS (FastAPI v1) =====================

/**
 * Latest data response for a station (used by /data/latest and /data/{station_number}/latest)
 */
data class StationLatestDataResponse(
    val station_number: String,
    val custom_name: String?,
    val is_favorite: Boolean,
    val location: String?,
    val latitude: Double?,
    val longitude: Double?,
    val parameters: List<ParameterValue>,
    val timestamp: String?          // ISO 8601 timestamp
)

/**
 * Parameter value in latest data response
 */
data class ParameterValue(
    val code: String,
    val name: String,
    val value: Double?,
    val unit: String?,
    val category: String?
)

/**
 * Historical data response for a parameter (used by /data/{station_number}/{parameter_code}/history)
 * NOTE: This response is NOT wrapped in ApiResponse - it contains success field directly
 */
data class ParameterHistoryResponse(
    val success: Boolean,
    val station_number: String,
    val parameter: ParameterInfoBasic,
    val data: List<HistoryDataPoint>,
    val count: Int
)

/**
 * Basic parameter information in history response
 */
data class ParameterInfoBasic(
    val code: String,
    val name: String,
    val unit: String?,
    val category: String?
)

/**
 * History data point
 */
data class HistoryDataPoint(
    val time: Long,                 // Unix timestamp
    val value: Double
)

/**
 * Sensor data point (legacy, for backward compatibility with repository interface)
 */
typealias SensorDataPoint = HistoryDataPoint

/**
 * Parameter information with detailed metadata
 */
data class ParameterInfo(
    val code: String,               // Parameter code (T, H, P)
    val name: String,               // Display name
    val unit: String,               // Measurement unit
    val description: String,        // Full description
    val category: String            // Parameter category
)

/**
 * Legacy parameter metadata (for compatibility)
 */
data class ParameterMetadata(
    val name: String,
    val unit: String,
    val description: String
)

// ===================== PARAMETER VISIBILITY MODELS =====================

/**
 * Parameter with visibility information (used by GET /stations/{station_number}/parameters)
 */
data class ParameterVisibilityInfo(
    val code: String,
    val name: String,
    val unit: String?,
    val description: String?,
    val category: String?,
    val is_visible: Boolean,
    val display_order: Int
)

/**
 * Request to update visibility of a single parameter
 */
data class UpdateParameterVisibilityRequest(
    val is_visible: Boolean
)

/**
 * Response when updating single parameter visibility
 */
data class UpdateParameterVisibilityResponse(
    val success: Boolean,
    val parameter_code: String,
    val is_visible: Boolean
)

/**
 * Request to update visibility of multiple parameters
 */
data class BulkUpdateParametersRequest(
    val parameters: List<ParameterVisibilityUpdate>
)

/**
 * Single parameter visibility update in bulk request
 */
data class ParameterVisibilityUpdate(
    val code: String,
    val visible: Boolean
)

/**
 * Response when updating multiple parameters visibility
 */
data class BulkUpdateParametersResponse(
    val success: Boolean,
    val updated: Int,
    val total: Int
)