package com.shestikpetr.meteo.network

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.POST
import retrofit2.http.Body
import retrofit2.http.Header

data class ValueResponse(val value: Double)

// Интерфейс для запросов
interface MeteoApiService {
    // Авторизация пользователя
    @POST("auth/login")
    suspend fun login(@Body credentials: UserCredentials): Response<LoginResponse>

    // Получение данных сенсора за период
    @GET("sensors/{complexId}/{parameter}")
    suspend fun getSensorData(
        @Path("complexId") complexId: String,       // Айди комплекса
        @Path("parameter") parameter: String,      // Параметр
        @Query("startTime") startTime: Long?,       // Время начала интервала
        @Query("endTime") endTime: Long?,          // Время окончания интервала
        @Header("Authorization") authToken: String  // Токен авторизации
    ): List<SensorDataPoint>

    // Запрос для получения последней записи выбранного параметра
    @GET("sensors/{complexId}/{parameter}/latest")
    suspend fun getLatestSensorData(
        @Path("complexId") complexId: String,       // Айди комплекса
        @Path("parameter") parameter: String,       // Параметр
        @Header("Authorization") authToken: String  // Токен авторизации
    ): ValueResponse

    // Получение списка доступных параметров для станции
    @GET("sensors/{complexId}/parameters")
    suspend fun getStationParameters(
        @Path("complexId") complexId: String,       // Айди комплекса
        @Header("Authorization") authToken: String  // Токен авторизации
    ): List<ParameterInfo>

    // Получение списка доступных станций пользователя
    @GET("stations")
    suspend fun getUserStations(
        @Header("Authorization") authToken: String  // Токен авторизации
    ): List<StationInfo>

    // Получение метаданных о параметрах
    @GET("parameters")
    suspend fun getParametersMetadata(
        @Header("Authorization") authToken: String  // Токен авторизации
    ): Map<String, ParameterMetadata>

    // Получение информации о конкретном параметре
    @GET("parameters/{parameterId}")
    suspend fun getParameterMetadata(
        @Path("parameterId") parameterId: String,   // Идентификатор параметра
        @Header("Authorization") authToken: String  // Токен авторизации
    ): ParameterMetadata
}

// Данные для авторизации
data class UserCredentials(
    val username: String,
    val password: String
)

// Ответ на авторизацию
data class LoginResponse(
    val success: Boolean,
    val token: String?,
    val error: String?
)

// Точка данных с сенсора
data class SensorDataPoint(
    val time: Long,   // Время
    val value: Double // Значение
)

// Информация о параметре станции
data class ParameterInfo(
    val id: String,
    val name: String
)

// Информация о метеостанции
data class StationInfo(
    val stationNumber: String?,
    val name: String?,
    val location: String?
)

// Метаданные параметра
data class ParameterMetadata(
    val name: String,
    val unit: String,
    val description: String
)