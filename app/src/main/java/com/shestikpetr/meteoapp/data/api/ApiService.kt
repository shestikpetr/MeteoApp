package com.shestikpetr.meteoapp.data.api

import com.shestikpetr.meteoapp.data.model.ApiResponseAuthLoginData
import com.shestikpetr.meteoapp.data.model.ApiResponseListUserStationResponse
import com.shestikpetr.meteoapp.data.model.ApiResponseMapStringString
import com.shestikpetr.meteoapp.data.model.ApiResponseParameterHistoryResponse
import com.shestikpetr.meteoapp.data.model.ApiResponseRefreshTokenData
import com.shestikpetr.meteoapp.data.model.ApiResponseStationDataResponse
import com.shestikpetr.meteoapp.data.model.ApiResponseStationParametersResponse
import com.shestikpetr.meteoapp.data.model.ApiResponseUnit
import com.shestikpetr.meteoapp.data.model.ApiResponseUserResponse
import com.shestikpetr.meteoapp.data.model.ApiResponseUserStationResponse
import com.shestikpetr.meteoapp.data.model.ChangePasswordRequest
import com.shestikpetr.meteoapp.data.model.RefreshTokenRequest
import com.shestikpetr.meteoapp.data.model.UpdateMeRequest
import com.shestikpetr.meteoapp.data.model.UserLoginRequest
import com.shestikpetr.meteoapp.data.model.UserRegisterRequest
import com.shestikpetr.meteoapp.data.model.UserStationRequest
import com.shestikpetr.meteoapp.data.model.UserStationUpdateRequest
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
    suspend fun login(@Body request: UserLoginRequest): Response<ApiResponseAuthLoginData>

    @POST("api/v1/auth/register")
    suspend fun register(@Body request: UserRegisterRequest): Response<ApiResponseAuthLoginData>

    @POST("api/v1/auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<ApiResponseRefreshTokenData>

    @POST("api/v1/auth/logout")
    suspend fun logout(
        @Header("Authorization") token: String
    ): Response<ApiResponseUnit>

    @POST("api/v1/auth/change-password")
    suspend fun changePassword(
        @Header("Authorization") token: String,
        @Body request: ChangePasswordRequest
    ): Response<ApiResponseUnit>

    @GET("api/v1/auth/me")
    suspend fun me(
        @Header("Authorization") token: String
    ): Response<ApiResponseUserResponse>

    @PATCH("api/v1/auth/me")
    suspend fun updateMe(
        @Header("Authorization") token: String,
        @Body request: UpdateMeRequest
    ): Response<ApiResponseUserResponse>

    // Stations
    @GET("api/v1/stations")
    suspend fun getUserStations(
        @Header("Authorization") token: String
    ): Response<ApiResponseListUserStationResponse>

    @POST("api/v1/stations")
    suspend fun addStation(
        @Header("Authorization") token: String,
        @Body request: UserStationRequest
    ): Response<ApiResponseUserStationResponse>

    @DELETE("api/v1/stations/{stationNumber}")
    suspend fun deleteStation(
        @Header("Authorization") token: String,
        @Path("stationNumber") stationNumber: String
    ): Response<ApiResponseUnit>

    @PATCH("api/v1/stations/{stationNumber}")
    suspend fun renameStation(
        @Header("Authorization") token: String,
        @Path("stationNumber") stationNumber: String,
        @Body request: UserStationUpdateRequest
    ): Response<ApiResponseUserStationResponse>

    @GET("api/v1/stations/{stationNumber}/parameters")
    suspend fun getStationParameters(
        @Header("Authorization") token: String,
        @Path("stationNumber") stationNumber: String
    ): Response<ApiResponseStationParametersResponse>

    // Data
    @GET("api/v1/stations/{stationNumber}/data")
    suspend fun getStationData(
        @Header("Authorization") token: String,
        @Path("stationNumber") stationNumber: String
    ): Response<ApiResponseStationDataResponse>

    @GET("api/v1/stations/{stationNumber}/parameters/{parameterCode}/history")
    suspend fun getParameterHistory(
        @Header("Authorization") token: String,
        @Path("stationNumber") stationNumber: String,
        @Path("parameterCode") parameterCode: Int,
        @Query("startTime") startTime: Long? = null,
        @Query("endTime") endTime: Long? = null
    ): Response<ApiResponseParameterHistoryResponse>

    // Health
    @GET("health")
    suspend fun health(): Response<ApiResponseMapStringString>
}
