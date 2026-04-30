package com.shestikpetr.meteoapp.data.remote.dto

// --- Requests ---

data class UserStationRequest(
    val stationNumber: String,
    val customName: String? = null
)

data class UserStationUpdateRequest(
    val customName: String?
)

// --- Server payloads ---

data class UserStationResponseDto(
    val stationNumber: String,
    val name: String,
    val location: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val altitude: Double? = null
)

data class ParameterMetadataDto(
    val code: Int,
    val name: String,
    val unit: String? = null,
    val description: String? = null
)

data class StationParametersResponseDto(
    val parameters: List<ParameterMetadataDto>
)

data class TimeSeriesPointDto(
    val time: Long,
    val value: Double
)

data class ParameterHistoryResponseDto(
    val parameter: ParameterMetadataDto,
    val data: List<TimeSeriesPointDto>
)

data class ParameterWithValueDto(
    val code: Int,
    val name: String,
    val value: Double? = null,
    val unit: String? = null,
    val description: String? = null
)

data class StationDataResponseDto(
    val stationNumber: String,
    val name: String,
    val location: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val time: Long? = null,
    val parameters: List<ParameterWithValueDto>
)

// --- Envelopes ---

data class ApiResponseListUserStationResponse(
    val success: Boolean = false,
    val data: List<UserStationResponseDto>? = null,
    val error: String? = null
)

data class ApiResponseUserStationResponse(
    val success: Boolean = false,
    val data: UserStationResponseDto? = null,
    val error: String? = null
)

data class ApiResponseStationParametersResponse(
    val success: Boolean = false,
    val data: StationParametersResponseDto? = null,
    val error: String? = null
)

data class ApiResponseParameterHistoryResponse(
    val success: Boolean = false,
    val data: ParameterHistoryResponseDto? = null,
    val error: String? = null
)

data class ApiResponseStationDataResponse(
    val success: Boolean = false,
    val data: StationDataResponseDto? = null,
    val error: String? = null
)
