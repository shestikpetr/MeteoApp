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
 * Base URL: http://84.237.1.131:8085/api/v1/
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
    suspend fun refreshToken(@Header("Authorization") refreshToken: String): Response<RefreshTokenResponse>

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
    @PUT("stations/{station_number}")
    suspend fun updateStation(
        @Path("station_number") stationNumber: String,
        @Header("Authorization") authToken: String,
        @Body updateData: UpdateStationRequest
    ): Response<ApiResponse<Unit>>

    /**
     * Remove station from user
     */
    @DELETE("stations/{station_number}")
    suspend fun removeStation(
        @Path("station_number") stationNumber: String,
        @Header("Authorization") authToken: String
    ): Response<ApiResponse<Unit>>

    /**
     * Get station parameters
     */
    @GET("stations/{station_number}/parameters")
    suspend fun getStationParameters(
        @Path("station_number") stationNumber: String,
        @Header("Authorization") authToken: String
    ): Response<ApiResponse<List<ParameterInfo>>>

    // ===================== SENSOR DATA ENDPOINTS =====================

    /**
     * Get sensor data time series
     */
    @GET("sensors/{station_number}/{parameter}")
    suspend fun getSensorData(
        @Path("station_number") stationNumber: String,
        @Path("parameter") parameter: String,
        @Query("start_time") startTime: Long?,
        @Query("end_time") endTime: Long?,
        @Query("limit") limit: Int?,
        @Header("Authorization") authToken: String
    ): Response<ApiResponse<List<SensorDataPoint>>>

    /**
     * Get latest value for specific parameter
     */
    @GET("sensors/{station_number}/{parameter}/latest")
    suspend fun getLatestSensorData(
        @Path("station_number") stationNumber: String,
        @Path("parameter") parameter: String,
        @Header("Authorization") authToken: String
    ): Response<ApiResponse<SensorDataPoint>>


    /**
     * Get latest values for all parameters of a station
     */
    @GET("sensors/{station_number}/latest")
    suspend fun getLatestStationData(
        @Path("station_number") stationNumber: String,
        @Header("Authorization") authToken: String
    ): Response<ApiResponse<List<SensorDataPoint>>>

    /**
     * Get latest data from all user stations
     */
    @GET("sensors/latest")
    suspend fun getAllStationsLatestData(
        @Header("Authorization") authToken: String
    ): Response<ApiResponse<Map<String, List<SensorDataPoint>>>>
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
    val user_id: Int,
    val access_token: String,
    val refresh_token: String
)

/**
 * Refresh token response (only returns new access token)
 */
data class RefreshTokenResponse(
    val success: Boolean,
    val access_token: String?
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
    val station_number: String,     // 8-digit station number
    val name: String,               // System name
    val custom_name: String?,       // User custom name
    val display_name: String,       // Display name (custom_name or name)
    val location: String,
    val latitude: Double,           // Exact coordinates
    val longitude: Double,
    val altitude: Double?,
    val is_favorite: Boolean,       // User favorite flag
    val is_active: Boolean,         // Station active status
    val parameters: List<String>    // Available parameter codes
)

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
 * Request to update station settings
 */
data class UpdateStationRequest(
    val custom_name: String?,      // Update custom name
    val is_favorite: Boolean?      // Update favorite status
)

// ===================== SENSOR DATA MODELS =====================

/**
 * Sensor data point from API v1
 */
data class SensorDataPoint(
    val time: Long,                 // Unix timestamp
    val value: Double,              // Sensor value
    val parameter: String,          // Parameter code (T, H, P, etc.)
    val station: String             // Station number
)

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