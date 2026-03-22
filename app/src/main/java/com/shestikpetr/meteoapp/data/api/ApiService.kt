package com.shestikpetr.meteoapp.data.api

import com.shestikpetr.meteoapp.data.model.AuthResponse
import com.shestikpetr.meteoapp.data.model.ParameterHistoryResponse
import com.shestikpetr.meteoapp.data.model.RefreshTokenResponse
import com.shestikpetr.meteoapp.data.model.StationParametersResponse
import com.shestikpetr.meteoapp.data.model.UserLoginRequest
import com.shestikpetr.meteoapp.data.model.UserMeResponse
import com.shestikpetr.meteoapp.data.model.UserRegisterRequest
import com.shestikpetr.meteoapp.data.model.AddStationRequest
import com.shestikpetr.meteoapp.data.model.AddStationResponse
import com.shestikpetr.meteoapp.data.model.RenameStationRequest
import com.shestikpetr.meteoapp.data.model.UserStationListResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    // Auth
    @POST("api/v1/auth/login")
    suspend fun login(@Body request: UserLoginRequest): Response<AuthResponse>

    @POST("api/v1/auth/register")
    suspend fun register(@Body request: UserRegisterRequest): Response<AuthResponse>

    @POST("api/v1/auth/logout")
    suspend fun logout(): Response<Unit>

    @GET("api/v1/auth/me")
    suspend fun getCurrentUser(
        @Header("Authorization") token: String
    ): Response<UserMeResponse>

    @POST("api/v1/auth/refresh")
    suspend fun refreshToken(
        @Header("Authorization") token: String
    ): Response<RefreshTokenResponse>

    // Stations
    @GET("api/v1/stations")
    suspend fun getUserStations(
        @Header("Authorization") token: String
    ): Response<UserStationListResponse>

    @POST("api/v1/stations")
    suspend fun addStation(
        @Header("Authorization") token: String,
        @Body request: AddStationRequest
    ): Response<AddStationResponse>

    @DELETE("api/v1/stations/{station_id}")
    suspend fun deleteStation(
        @Header("Authorization") token: String,
        @Path("station_id") stationId: String
    ): Response<Unit>

    @PATCH("api/v1/stations/{station_id}")
    suspend fun renameStation(
        @Header("Authorization") token: String,
        @Path("station_id") stationId: String,
        @Body request: RenameStationRequest
    ): Response<UserStationListResponse>

    @GET("api/v1/stations/{station_number}/parameters")
    suspend fun getStationParameters(
        @Header("Authorization") token: String,
        @Path("station_number") stationNumber: String
    ): Response<StationParametersResponse>

    // Data
    @GET("api/v1/data/{station_number}/{parameter_code}/history")
    suspend fun getParameterHistory(
        @Header("Authorization") token: String,
        @Path("station_number") stationNumber: String,
        @Path("parameter_code") parameterCode: String,
        @Query("start_time") startTime: Long? = null,
        @Query("end_time") endTime: Long? = null
    ): Response<ParameterHistoryResponse>
}
