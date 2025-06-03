package com.shestikpetr.meteo.network

import android.util.Log
import com.shestikpetr.meteo.data.StationWithLocation
import com.yandex.maps.mobile.BuildConfig
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Inject

// Репозиторий метеоданных
interface MeteoRepository {
    suspend fun getSensorData(
        complexId: String,
        parameter: String,
        startTime: Long? = null,
        endTime: Long? = null
    ): List<SensorDataPoint>

    suspend fun getLatestSensorData(
        complexId: String,
        parameter: String
    ): Double

    suspend fun getStationParameters(
        complexId: String
    ): List<ParameterInfo>

    suspend fun getUserStations(): List<StationInfo>

    suspend fun getParametersMetadata(): Map<String, ParameterMetadata>

    suspend fun getParameterMetadata(parameterId: String): ParameterMetadata

    suspend fun getUserStationsWithLocation(): List<StationWithLocation>
}

// Реализация репозитория метеоданных
class NetworkMeteoRepository @Inject constructor(
    private val meteoApiService: MeteoApiService,
    private val authManager: AuthManager
) : MeteoRepository {
    override suspend fun getSensorData(
        complexId: String,
        parameter: String,
        startTime: Long?,
        endTime: Long?
    ): List<SensorDataPoint> {
        val token = "Basic ${authManager.getAuthToken()}"
        return meteoApiService.getSensorData(complexId, parameter, startTime, endTime, token)
    }

    override suspend fun getLatestSensorData(complexId: String, parameter: String): Double {
        var lastException: Exception? = null

        // Пробуем 3 раза с задержкой
        repeat(3) { attempt ->
            try {
                val token = "Basic ${authManager.getAuthToken()}"
                Log.d(
                    "MeteoRepository",
                    "Попытка ${attempt + 1}/3 для $complexId, параметр: $parameter"
                )

                val response = meteoApiService.getLatestSensorData(complexId, parameter, token)
                Log.d("MeteoRepository", "Успешно получен ответ для $complexId: ${response.value}")

                return response.value
            } catch (e: retrofit2.HttpException) {
                // HTTP ошибка
                lastException = e
                Log.w("MeteoRepository", "HTTP ошибка ${e.code()} для $complexId: ${e.message()}")

                try {
                    val errorBody = e.response()?.errorBody()?.string()
                    Log.w("MeteoRepository", "Тело ошибки: $errorBody")
                } catch (bodyException: Exception) {
                    Log.w(
                        "MeteoRepository",
                        "Не удалось прочитать тело ошибки: ${bodyException.message}"
                    )
                }
            } catch (e: com.google.gson.JsonSyntaxException) {
                // Ошибка парсинга JSON
                lastException = e
                Log.w("MeteoRepository", "Ошибка парсинга JSON для $complexId: ${e.message}")

                try {
                    val rawResponse = getRawLatestData(complexId, parameter)
                    if (rawResponse != null) {
                        Log.d("MeteoRepository", "Получен raw ответ: '$rawResponse'")
                        val extractedValue = extractValueFromResponse(rawResponse)
                        if (extractedValue != null) {
                            Log.d("MeteoRepository", "Успешно извлечено значение: $extractedValue")
                            return extractedValue
                        }
                    }
                } catch (rawException: Exception) {
                    Log.e("MeteoRepository", "Ошибка получения raw данных: ${rawException.message}")
                }
            } catch (e: java.net.ProtocolException) {
                // Ошибка протокола
                lastException = e
                Log.w("MeteoRepository", "Ошибка протокола для $complexId: ${e.message}")
            } catch (e: Exception) {
                lastException = e
                Log.w(
                    "MeteoRepository",
                    "Попытка ${attempt + 1}/3 неудачна для $complexId: ${e.message}"
                )
            }

            // Если это последняя попытка, не ждем
            if (attempt < 2) {
                delay(1000) // Ждем 1 секунду перед следующей попыткой
            }
        }

        // Если все попытки неудачны, возвращаем 0.0 вместо исключения
        Log.e(
            "MeteoRepository",
            "Все попытки неудачны для $complexId, возвращаем -1000",
            lastException
        )
        return -1000.0
    }

    private fun getRawLatestData(complexId: String, parameter: String): String? {
        return try {
            val token = "Basic ${authManager.getAuthToken()}"
            val url = "http://84.237.1.131:8085/api/sensors/$complexId/$parameter/latest"

            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()

            val request = okhttp3.Request.Builder()
                .url(url)
                .addHeader("Authorization", token)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            Log.d("MeteoRepository", "Raw HTTP ответ: '$responseBody'")
            responseBody
        } catch (e: Exception) {
            Log.e("MeteoRepository", "Ошибка получения raw ответа: ${e.message}")
            null
        }
    }

    private fun extractValueFromResponse(response: String): Double? {
        return try {
            if (response.trim().startsWith("{")) {
                val gson = com.google.gson.Gson()
                val jsonObject = gson.fromJson(response, com.google.gson.JsonObject::class.java)
                jsonObject.get("value")?.asDouble
            } else {
                val numberPattern = "-?\\d+(?:\\.\\d+)?".toRegex()
                val match = numberPattern.find(response.trim())
                match?.value?.toDouble()
            }
        } catch (e: Exception) {
            Log.e("MeteoRepository", "Ошибка извлечения значения из '$response': ${e.message}")
            null
        }
    }

    override suspend fun getStationParameters(complexId: String): List<ParameterInfo> {
        val token = "Basic ${authManager.getAuthToken()}"
        return meteoApiService.getStationParameters(complexId, token)
    }

    override suspend fun getUserStations(): List<StationInfo> {
        val token = "Basic ${authManager.getAuthToken()}"
        return meteoApiService.getUserStations(token)
    }

    override suspend fun getParametersMetadata(): Map<String, ParameterMetadata> {
        val token = "Basic ${authManager.getAuthToken()}"
        return meteoApiService.getParametersMetadata(token)
    }

    override suspend fun getParameterMetadata(parameterId: String): ParameterMetadata {
        val token = "Basic ${authManager.getAuthToken()}"
        return meteoApiService.getParameterMetadata(parameterId, token)
    }

    override suspend fun getUserStationsWithLocation(): List<StationWithLocation> {
        try {
            val token = "Basic ${authManager.getAuthToken()}"
            Log.d("MeteoRepository", "Запрос станций с токеном")

            val stations = meteoApiService.getUserStations(token)
            Log.d("MeteoRepository", "Получено станций: ${stations.size}")

            // Фильтруем станции с null полями перед маппингом
            return stations
                .filter { station ->
                    val isValid =
                        !station.stationNumber.isNullOrEmpty() && !station.location.isNullOrEmpty()
                    if (!isValid) {
                        Log.w("MeteoRepository", "Пропущена станция с неполными данными: $station")
                    }
                    isValid
                }
                .map { station ->
                    val coordinates = parseLocation(station.location!!)
                    StationWithLocation(
                        stationNumber = station.stationNumber!!,
                        name = station.name ?: station.stationNumber,
                        latitude = coordinates.first,
                        longitude = coordinates.second
                    )
                }
        } catch (e: Exception) {
            Log.e("NetworkMeteoRepository", "Ошибка при загрузке станций: ${e.message}", e)

            // В режиме отладки возвращаем демо-станции
            if (BuildConfig.DEBUG) {
                Log.d("MeteoRepository", "Возвращаем тестовые станции для отладки")
                return createDemoStations()
            }

            throw e
        }
    }

    // Функция для создания тестовых данных
    private fun createDemoStations(): List<StationWithLocation> {
        return listOf(
            StationWithLocation(
                stationNumber = "60000105",
                name = "60000105",
                latitude = 56.460850,
                longitude = 84.962327
            ),
            StationWithLocation(
                stationNumber = "60000104",
                name = "60000104",
                latitude = 56.460039,
                longitude = 84.962282
            ),
            StationWithLocation(
                stationNumber = "50000022",
                name = "50000022",
                latitude = 56.460337,
                longitude = 84.961591
            )
        )
    }

    private fun parseLocation(location: String): Pair<Double, Double> {
        try {
            if (location.isBlank()) {
                Log.w("MeteoRepository", "Получена пустая строка с координатами")
                return Pair(56.460337, 84.961591) // Значения по умолчанию
            }

            val parts = location.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .map {
                    try {
                        it.toDouble()
                    } catch (e: NumberFormatException) {
                        Log.e("MeteoRepository", "Ошибка преобразования координаты: $it", e)
                        0.0 // Значение по умолчанию при ошибке преобразования
                    }
                }

            if (parts.size >= 2) {
                return Pair(parts[0], parts[1])
            } else {
                Log.w("MeteoRepository", "Неверный формат координат: $location")
            }
        } catch (e: Exception) {
            Log.e("MeteoRepository", "Ошибка при парсинге координат: $location", e)
        }

        // Значения по умолчанию для центра Томска
        return Pair(56.460337, 84.961591)
    }
}