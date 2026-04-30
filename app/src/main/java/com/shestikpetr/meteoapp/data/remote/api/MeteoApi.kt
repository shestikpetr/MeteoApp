package com.shestikpetr.meteoapp.data.remote.api

import com.shestikpetr.meteoapp.data.remote.dto.ApiResponseAuthLoginData
import com.shestikpetr.meteoapp.data.remote.dto.ApiResponseListUserStationResponse
import com.shestikpetr.meteoapp.data.remote.dto.ApiResponseMapStringString
import com.shestikpetr.meteoapp.data.remote.dto.ApiResponseParameterHistoryResponse
import com.shestikpetr.meteoapp.data.remote.dto.ApiResponseRefreshTokenData
import com.shestikpetr.meteoapp.data.remote.dto.ApiResponseStationDataResponse
import com.shestikpetr.meteoapp.data.remote.dto.ApiResponseStationParametersResponse
import com.shestikpetr.meteoapp.data.remote.dto.ApiResponseUnit
import com.shestikpetr.meteoapp.data.remote.dto.ApiResponseUserResponse
import com.shestikpetr.meteoapp.data.remote.dto.ApiResponseUserStationResponse
import com.shestikpetr.meteoapp.data.remote.dto.ChangePasswordRequest
import com.shestikpetr.meteoapp.data.remote.dto.RefreshTokenRequest
import com.shestikpetr.meteoapp.data.remote.dto.UpdateMeRequest
import com.shestikpetr.meteoapp.data.remote.dto.UserLoginRequest
import com.shestikpetr.meteoapp.data.remote.dto.UserRegisterRequest
import com.shestikpetr.meteoapp.data.remote.dto.UserStationRequest
import com.shestikpetr.meteoapp.data.remote.dto.UserStationUpdateRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Низкоуровневый Retrofit-интерфейс. Авторизация обрабатывается через AuthInterceptor:
 * заголовок `Authorization: Bearer …` добавляется автоматически, когда токен есть в TokenStorage.
 * Не использовать напрямую из presentation-слоя — только через [com.shestikpetr.meteoapp.domain.repository] интерфейсы.
 */
interface MeteoApi {

    // === Auth ===

    @POST("api/v1/auth/login")
    suspend fun login(@Body request: UserLoginRequest): Response<ApiResponseAuthLoginData>

    @POST("api/v1/auth/register")
    suspend fun register(@Body request: UserRegisterRequest): Response<ApiResponseAuthLoginData>

    @POST("api/v1/auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<ApiResponseRefreshTokenData>

    @POST("api/v1/auth/logout")
    suspend fun logout(): Response<ApiResponseUnit>

    @POST("api/v1/auth/change-password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<ApiResponseUnit>

    @GET("api/v1/auth/me")
    suspend fun me(): Response<ApiResponseUserResponse>

    @PATCH("api/v1/auth/me")
    suspend fun updateMe(@Body request: UpdateMeRequest): Response<ApiResponseUserResponse>

    // === Stations ===

    @GET("api/v1/stations")
    suspend fun getUserStations(): Response<ApiResponseListUserStationResponse>

    @POST("api/v1/stations")
    suspend fun attachStation(@Body request: UserStationRequest): Response<ApiResponseUserStationResponse>

    @DELETE("api/v1/stations/{stationNumber}")
    suspend fun detachStation(@Path("stationNumber") stationNumber: String): Response<ApiResponseUnit>

    @PATCH("api/v1/stations/{stationNumber}")
    suspend fun renameStation(
        @Path("stationNumber") stationNumber: String,
        @Body request: UserStationUpdateRequest
    ): Response<ApiResponseUserStationResponse>

    @GET("api/v1/stations/{stationNumber}/parameters")
    suspend fun getStationParameters(
        @Path("stationNumber") stationNumber: String
    ): Response<ApiResponseStationParametersResponse>

    // === Data ===

    @GET("api/v1/stations/{stationNumber}/data")
    suspend fun getStationData(
        @Path("stationNumber") stationNumber: String
    ): Response<ApiResponseStationDataResponse>

    @GET("api/v1/stations/{stationNumber}/parameters/{parameterCode}/history")
    suspend fun getParameterHistory(
        @Path("stationNumber") stationNumber: String,
        @Path("parameterCode") parameterCode: Int,
        @Query("startTime") startTime: Long? = null,
        @Query("endTime") endTime: Long? = null
    ): Response<ApiResponseParameterHistoryResponse>

    // === Health ===

    @GET("health")
    suspend fun health(): Response<ApiResponseMapStringString>
}
