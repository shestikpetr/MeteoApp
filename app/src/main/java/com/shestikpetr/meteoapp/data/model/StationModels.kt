package com.shestikpetr.meteoapp.data.model

// --- Requests ---

data class UserStationRequest(
    val stationNumber: String,
    val customName: String? = null
)

data class UserStationUpdateRequest(
    val customName: String?
)

// --- Domain DTOs ---

data class UserStationResponse(
    val stationNumber: String,
    val name: String,
    val location: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val altitude: Double? = null
)

data class ParameterMetadata(
    val code: Int,
    val name: String,
    val unit: String? = null,
    val description: String? = null
)

data class StationParametersResponse(
    val parameters: List<ParameterMetadata>
)

data class TimeSeriesDataPoint(
    val time: Long,
    val value: Double
)

data class ParameterHistoryResponse(
    val parameter: ParameterMetadata,
    val data: List<TimeSeriesDataPoint>
)

data class ParameterWithValue(
    val code: Int,
    val name: String,
    val value: Double? = null,
    val unit: String? = null,
    val description: String? = null
)

data class StationDataResponse(
    val stationNumber: String,
    val name: String,
    val location: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val time: Long? = null,
    val parameters: List<ParameterWithValue>
)

// --- API wrappers ---

data class ApiResponseListUserStationResponse(
    val success: Boolean = false,
    val data: List<UserStationResponse>? = null,
    val error: String? = null
)

data class ApiResponseUserStationResponse(
    val success: Boolean = false,
    val data: UserStationResponse? = null,
    val error: String? = null
)

data class ApiResponseStationParametersResponse(
    val success: Boolean = false,
    val data: StationParametersResponse? = null,
    val error: String? = null
)

data class ApiResponseParameterHistoryResponse(
    val success: Boolean = false,
    val data: ParameterHistoryResponse? = null,
    val error: String? = null
)

data class ApiResponseStationDataResponse(
    val success: Boolean = false,
    val data: StationDataResponse? = null,
    val error: String? = null
)

data class ApiResponseUnit(
    val success: Boolean = false,
    val error: String? = null
)

data class ApiResponseMapStringString(
    val success: Boolean = false,
    val data: Map<String, String>? = null,
    val error: String? = null
)

// --- UI models ---

data class StationWithData(
    val stationNumber: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val parameterValue: Double?, // null means "None" (parameter not available)
    val unit: String?
)

data class StationParameterValue(
    val code: Int,
    val name: String,
    val value: Double,
    val unit: String?
)

data class StationAllData(
    val stationNumber: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val time: Long? = null,
    val parameters: List<StationParameterValue>
)
