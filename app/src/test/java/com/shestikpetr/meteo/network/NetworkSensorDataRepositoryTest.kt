package com.shestikpetr.meteo.network

import com.shestikpetr.meteo.cache.SensorDataCache
import com.shestikpetr.meteo.common.constants.MeteoConstants
import com.shestikpetr.meteo.repository.impl.NetworkSensorDataRepository
import com.shestikpetr.meteo.utils.RetryPolicy
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

/**
 * Unit tests for NetworkSensorDataRepository with new FastAPI endpoints
 */
class NetworkSensorDataRepositoryTest {

    private lateinit var repository: NetworkSensorDataRepository
    private lateinit var mockApiService: MeteoApiService
    private lateinit var mockAuthManager: AuthManager
    private lateinit var mockRetryPolicy: RetryPolicy
    private lateinit var mockCache: SensorDataCache

    @Before
    fun setup() {
        mockApiService = mockk()
        mockAuthManager = mockk()
        mockRetryPolicy = mockk()
        mockCache = mockk()

        repository = NetworkSensorDataRepository(
            meteoApiService = mockApiService,
            authManager = mockAuthManager,
            retryPolicy = mockRetryPolicy,
            sensorDataCache = mockCache
        )
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `getParameterHistory should return historical data points`() = runTest {
        // Given
        val stationNumber = "60000105"
        val parameterCode = "4402"
        val startTime = 1640000000L
        val endTime = 1640086400L

        val mockHistoryResponse = ParameterHistoryResponse(
            station_number = stationNumber,
            parameter = ParameterInfoBasic(
                code = parameterCode,
                name = "Температура воздуха",
                unit = "°C",
                category = "temperature"
            ),
            data = listOf(
                HistoryDataPoint(time = 1640000000L, value = 25.5),
                HistoryDataPoint(time = 1640003600L, value = 26.0),
                HistoryDataPoint(time = 1640007200L, value = 24.8)
            ),
            count = 3
        )

        coEvery { mockAuthManager.getAuthorizationHeader() } returns "Bearer test_token"
        coEvery {
            mockApiService.getParameterHistory(
                stationNumber = stationNumber,
                parameterCode = parameterCode,
                startTime = startTime,
                endTime = endTime,
                limit = null,
                authToken = "Bearer test_token"
            )
        } returns Response.success(ApiResponse(success = true, data = mockHistoryResponse))

        coEvery {
            mockRetryPolicy.executeWithDefaultRetry<List<SensorDataPoint>>(any())
        } coAnswers {
            val block = firstArg<suspend (Int) -> List<SensorDataPoint>>()
            RetryPolicy.RetryResult.Success(block(1))
        }

        // When
        val result = repository.getSensorData(stationNumber, parameterCode, startTime, endTime)

        // Then
        assertNotNull(result)
        assertEquals(3, result.size)
        assertEquals(25.5, result[0].value, 0.01)
        assertEquals(1640000000L, result[0].time)

        coVerify { mockApiService.getParameterHistory(stationNumber, parameterCode, startTime, endTime, null, "Bearer test_token") }
    }

    @Test
    fun `getLatestStationData should return latest parameter values`() = runTest {
        // Given
        val stationNumber = "60000105"
        val parameter = "4402"

        val mockStationData = StationLatestDataResponse(
            station_number = stationNumber,
            custom_name = "Test Station",
            is_favorite = false,
            location = "Tomsk",
            latitude = 56.46,
            longitude = 84.96,
            parameters = listOf(
                ParameterValue(
                    code = "4402",
                    name = "Температура воздуха",
                    value = 25.5,
                    unit = "°C",
                    category = "temperature"
                ),
                ParameterValue(
                    code = "5402",
                    name = "Влажность",
                    value = 65.0,
                    unit = "%",
                    category = "humidity"
                )
            ),
            timestamp = "2024-01-01T12:00:00Z"
        )

        coEvery { mockCache.getValue(any(), any()) } returns null
        coEvery { mockCache.isValidValue(any()) } returns false
        coEvery { mockCache.putValue(any(), any(), any()) } just Runs
        coEvery { mockAuthManager.getAuthorizationHeader() } returns "Bearer test_token"
        coEvery {
            mockApiService.getLatestStationData(stationNumber, "Bearer test_token")
        } returns Response.success(ApiResponse(success = true, data = mockStationData))

        coEvery {
            mockRetryPolicy.executeWithFallback(
                fallbackValue = MeteoConstants.Data.UNAVAILABLE_VALUE,
                config = any(),
                operation = any()
            )
        } coAnswers {
            val operation = thirdArg<suspend (Int) -> Double>()
            operation(1)
        }

        // When
        val result = repository.getLatestSensorData(stationNumber, parameter)

        // Then
        assertEquals(25.5, result, 0.01)
        coVerify { mockCache.putValue(stationNumber, parameter, 25.5) }
    }

    @Test
    fun `getAllStationsLatestData should return data for all stations`() = runTest {
        // Given
        val mockAllStationsData = listOf(
            StationLatestDataResponse(
                station_number = "60000105",
                custom_name = "Station 1",
                is_favorite = true,
                location = "Tomsk",
                latitude = 56.46,
                longitude = 84.96,
                parameters = listOf(
                    ParameterValue("4402", "Температура", 25.5, "°C", "temperature")
                ),
                timestamp = "2024-01-01T12:00:00Z"
            ),
            StationLatestDataResponse(
                station_number = "60000104",
                custom_name = "Station 2",
                is_favorite = false,
                location = "Tomsk",
                latitude = 56.46,
                longitude = 84.96,
                parameters = listOf(
                    ParameterValue("4402", "Температура", 22.0, "°C", "temperature")
                ),
                timestamp = "2024-01-01T12:00:00Z"
            )
        )

        coEvery { mockAuthManager.getAuthorizationHeader() } returns "Bearer test_token"
        coEvery {
            mockApiService.getLatestDataAllStations("Bearer test_token")
        } returns Response.success(ApiResponse(success = true, data = mockAllStationsData))

        // When
        val result = repository.getAllStationsLatestData()

        // Then
        assertNotNull(result)
        assertEquals(2, result.size)
        assertTrue(result.containsKey("60000105"))
        assertTrue(result.containsKey("60000104"))
        assertEquals(25.5, result["60000105"]?.get("4402"))
        assertEquals(22.0, result["60000104"]?.get("4402"))
    }

    @Test
    fun `getLatestMultiParameterData should return map of parameter values`() = runTest {
        // Given
        val stationNumber = "60000105"
        val parameters = listOf("4402", "5402")

        val mockStationData = StationLatestDataResponse(
            station_number = stationNumber,
            custom_name = "Test Station",
            is_favorite = false,
            location = "Tomsk",
            latitude = 56.46,
            longitude = 84.96,
            parameters = listOf(
                ParameterValue("4402", "Температура", 25.5, "°C", "temperature"),
                ParameterValue("5402", "Влажность", 65.0, "%", "humidity")
            ),
            timestamp = "2024-01-01T12:00:00Z"
        )

        coEvery { mockAuthManager.getAuthorizationHeader() } returns "Bearer test_token"
        coEvery {
            mockApiService.getLatestStationData(stationNumber, "Bearer test_token")
        } returns Response.success(ApiResponse(success = true, data = mockStationData))
        coEvery { mockCache.putValue(any(), any(), any()) } just Runs

        // When
        val result = repository.getLatestMultiParameterData(stationNumber, parameters)

        // Then
        assertNotNull(result)
        assertEquals(2, result.size)
        assertEquals(25.5, result["4402"])
        assertEquals(65.0, result["5402"])

        coVerify { mockCache.putValue(stationNumber, "4402", 25.5) }
        coVerify { mockCache.putValue(stationNumber, "5402", 65.0) }
    }

    @Test
    fun `getLatestSensorData should use cached value when available`() = runTest {
        // Given
        val stationNumber = "60000105"
        val parameter = "4402"
        val cachedValue = 26.3

        coEvery { mockCache.getValue(stationNumber, parameter) } returns cachedValue
        coEvery { mockCache.isValidValue(cachedValue) } returns true

        // When
        val result = repository.getLatestSensorData(stationNumber, parameter)

        // Then
        assertEquals(cachedValue, result, 0.01)
        coVerify(exactly = 0) { mockApiService.getLatestStationData(any(), any()) }
    }
}
