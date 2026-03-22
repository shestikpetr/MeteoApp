package com.shestikpetr.meteoapp.data.model

import com.google.gson.annotations.SerializedName

// Response for GET /api/v1/stations
data class UserStationListResponse(
    val success: Boolean = true,
    val data: List<UserStationResponse>
)

data class UserStationResponse(
    val id: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("station_id") val stationId: String,
    @SerializedName("custom_name") val customName: String?,
    @SerializedName("is_favorite") val isFavorite: Boolean = false,
    @SerializedName("created_at") val createdAt: String?,
    val station: StationResponse?
)

data class StationResponse(
    val id: String,
    @SerializedName("station_number") val stationNumber: String,
    val name: String,
    val location: String?,
    val latitude: Double?,
    val longitude: Double?,
    val altitude: Double?,
    @SerializedName("is_active") val isActive: Boolean = true,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("updated_at") val updatedAt: String?
)

// Response for GET /api/v1/stations/{station_number}/parameters
data class StationParametersResponse(
    val success: Boolean = true,
    @SerializedName("station_number") val stationNumber: String,
    val parameters: List<ParameterMetadata>
)

data class ParameterMetadata(
    val code: String,
    val name: String,
    val unit: String?,
    val category: String?
)

// Response for GET /api/v1/data/{station_number}/{parameter_code}/history
data class ParameterHistoryResponse(
    val success: Boolean = true,
    @SerializedName("station_number") val stationNumber: String,
    val parameter: ParameterMetadata,
    val data: List<TimeSeriesDataPoint>,
    val count: Int
)

data class TimeSeriesDataPoint(
    val time: Long,
    val value: Double
)

// Station management requests
data class AddStationRequest(
    @SerializedName("station_number") val stationNumber: String
)

data class RenameStationRequest(
    @SerializedName("custom_name") val customName: String
)

data class AddStationResponse(
    val success: Boolean = true,
    val data: UserStationResponse
)

// UI models
data class StationWithData(
    val stationNumber: String,
    val name: String,
    val customName: String?,
    val latitude: Double,
    val longitude: Double,
    val parameterValue: String?, // null means "None" (parameter not available)
    val unit: String?
)

data class StationParameterValue(
    val code: String,
    val name: String,
    val value: String,
    val unit: String?
)

data class StationAllData(
    val stationNumber: String,
    val name: String,
    val customName: String?,
    val latitude: Double,
    val longitude: Double,
    val parameters: List<StationParameterValue>
)
