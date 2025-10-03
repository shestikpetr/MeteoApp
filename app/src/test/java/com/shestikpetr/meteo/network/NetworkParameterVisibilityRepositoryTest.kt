package com.shestikpetr.meteo.network

import com.shestikpetr.meteo.repository.impl.NetworkParameterVisibilityRepository
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

/**
 * Unit tests for NetworkParameterVisibilityRepository with FastAPI parameter visibility endpoints
 */
class NetworkParameterVisibilityRepositoryTest {

    private lateinit var repository: NetworkParameterVisibilityRepository
    private lateinit var mockApiService: MeteoApiService
    private lateinit var mockAuthManager: AuthManager

    @Before
    fun setup() {
        mockApiService = mockk()
        mockAuthManager = mockk()

        repository = NetworkParameterVisibilityRepository(
            meteoApiService = mockApiService,
            authManager = mockAuthManager
        )
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `getStationParametersWithVisibility should return parameters with visibility info`() = runTest {
        // Given
        val stationNumber = "60000105"
        val mockParameters = listOf(
            ParameterVisibilityInfo(
                code = "4402",
                name = "Температура воздуха",
                unit = "°C",
                description = "Температура на высоте 2м",
                category = "temperature",
                is_visible = true,
                display_order = 1
            ),
            ParameterVisibilityInfo(
                code = "5402",
                name = "Влажность",
                unit = "%",
                description = "Относительная влажность",
                category = "humidity",
                is_visible = false,
                display_order = 2
            )
        )

        coEvery { mockAuthManager.getAuthorizationHeader() } returns "Bearer test_token"
        coEvery {
            mockApiService.getStationParameters(stationNumber, "Bearer test_token")
        } returns Response.success(ApiResponse(success = true, data = mockParameters))

        // When
        val result = repository.getStationParametersWithVisibility(stationNumber)

        // Then
        assertEquals(2, result.size)
        assertTrue(result[0].is_visible)
        assertFalse(result[1].is_visible)
        assertEquals("4402", result[0].code)
        assertEquals("5402", result[1].code)
    }

    @Test
    fun `updateParameterVisibility should update single parameter visibility`() = runTest {
        // Given
        val stationNumber = "60000105"
        val parameterCode = "4402"
        val isVisible = true

        val mockResponse = UpdateParameterVisibilityResponse(
            success = true,
            parameter_code = parameterCode,
            is_visible = isVisible
        )

        coEvery { mockAuthManager.getAuthorizationHeader() } returns "Bearer test_token"
        coEvery {
            mockApiService.updateParameterVisibility(
                stationNumber = stationNumber,
                parameterCode = parameterCode,
                authToken = "Bearer test_token",
                request = UpdateParameterVisibilityRequest(is_visible = isVisible)
            )
        } returns Response.success(ApiResponse(success = true, data = mockResponse))

        // When
        val result = repository.updateParameterVisibility(stationNumber, parameterCode, isVisible)

        // Then
        assertTrue(result)
        coVerify {
            mockApiService.updateParameterVisibility(
                stationNumber,
                parameterCode,
                "Bearer test_token",
                UpdateParameterVisibilityRequest(is_visible = isVisible)
            )
        }
    }

    @Test
    fun `updateMultipleParametersVisibility should update multiple parameters`() = runTest {
        // Given
        val stationNumber = "60000105"
        val parameterUpdates = mapOf(
            "4402" to true,
            "5402" to false,
            "700" to true
        )

        val mockResponse = BulkUpdateParametersResponse(
            success = true,
            updated = 3,
            total = 3
        )

        coEvery { mockAuthManager.getAuthorizationHeader() } returns "Bearer test_token"
        coEvery {
            mockApiService.updateMultipleParametersVisibility(
                stationNumber = stationNumber,
                authToken = "Bearer test_token",
                request = any()
            )
        } returns Response.success(ApiResponse(success = true, data = mockResponse))

        // When
        val result = repository.updateMultipleParametersVisibility(stationNumber, parameterUpdates)

        // Then
        assertEquals(3, result.first) // updated count
        assertEquals(3, result.second) // total count

        coVerify {
            mockApiService.updateMultipleParametersVisibility(
                stationNumber = stationNumber,
                authToken = "Bearer test_token",
                request = match { request ->
                    request.parameters.size == 3 &&
                    request.parameters.any { it.code == "4402" && it.visible }
                }
            )
        }
    }

    @Test
    fun `getVisibleParameters should return only visible parameter codes`() = runTest {
        // Given
        val stationNumber = "60000105"
        val mockParameters = listOf(
            ParameterVisibilityInfo("4402", "Температура", "°C", null, "temperature", true, 1),
            ParameterVisibilityInfo("5402", "Влажность", "%", null, "humidity", false, 2),
            ParameterVisibilityInfo("700", "Давление", "гПа", null, "pressure", true, 3)
        )

        coEvery { mockAuthManager.getAuthorizationHeader() } returns "Bearer test_token"
        coEvery {
            mockApiService.getStationParameters(stationNumber, "Bearer test_token")
        } returns Response.success(ApiResponse(success = true, data = mockParameters))

        // When
        val result = repository.getVisibleParameters(stationNumber)

        // Then
        assertEquals(2, result.size)
        assertTrue(result.contains("4402"))
        assertTrue(result.contains("700"))
        assertFalse(result.contains("5402"))
    }

    @Test
    fun `isParameterVisible should check visibility of specific parameter`() = runTest {
        // Given
        val stationNumber = "60000105"
        val parameterCode = "4402"
        val mockParameters = listOf(
            ParameterVisibilityInfo("4402", "Температура", "°C", null, "temperature", true, 1),
            ParameterVisibilityInfo("5402", "Влажность", "%", null, "humidity", false, 2)
        )

        coEvery { mockAuthManager.getAuthorizationHeader() } returns "Bearer test_token"
        coEvery {
            mockApiService.getStationParameters(stationNumber, "Bearer test_token")
        } returns Response.success(ApiResponse(success = true, data = mockParameters))

        // When
        val result = repository.isParameterVisible(stationNumber, parameterCode)

        // Then
        assertTrue(result)
    }

    @Test
    fun `showAllParameters should set all parameters visible`() = runTest {
        // Given
        val stationNumber = "60000105"
        val mockParameters = listOf(
            ParameterVisibilityInfo("4402", "Температура", "°C", null, "temperature", false, 1),
            ParameterVisibilityInfo("5402", "Влажность", "%", null, "humidity", false, 2)
        )

        val mockUpdateResponse = BulkUpdateParametersResponse(
            success = true,
            updated = 2,
            total = 2
        )

        coEvery { mockAuthManager.getAuthorizationHeader() } returns "Bearer test_token"
        coEvery {
            mockApiService.getStationParameters(stationNumber, "Bearer test_token")
        } returns Response.success(ApiResponse(success = true, data = mockParameters))
        coEvery {
            mockApiService.updateMultipleParametersVisibility(
                stationNumber = stationNumber,
                authToken = "Bearer test_token",
                request = any()
            )
        } returns Response.success(ApiResponse(success = true, data = mockUpdateResponse))

        // When
        val result = repository.showAllParameters(stationNumber)

        // Then
        assertEquals(2, result.first)
        assertEquals(2, result.second)

        coVerify {
            mockApiService.updateMultipleParametersVisibility(
                stationNumber = stationNumber,
                authToken = "Bearer test_token",
                request = match { request ->
                    request.parameters.all { it.visible }
                }
            )
        }
    }

    @Test
    fun `hideAllParameters should set all parameters hidden`() = runTest {
        // Given
        val stationNumber = "60000105"
        val mockParameters = listOf(
            ParameterVisibilityInfo("4402", "Температура", "°C", null, "temperature", true, 1),
            ParameterVisibilityInfo("5402", "Влажность", "%", null, "humidity", true, 2)
        )

        val mockUpdateResponse = BulkUpdateParametersResponse(
            success = true,
            updated = 2,
            total = 2
        )

        coEvery { mockAuthManager.getAuthorizationHeader() } returns "Bearer test_token"
        coEvery {
            mockApiService.getStationParameters(stationNumber, "Bearer test_token")
        } returns Response.success(ApiResponse(success = true, data = mockParameters))
        coEvery {
            mockApiService.updateMultipleParametersVisibility(
                stationNumber = stationNumber,
                authToken = "Bearer test_token",
                request = any()
            )
        } returns Response.success(ApiResponse(success = true, data = mockUpdateResponse))

        // When
        val result = repository.hideAllParameters(stationNumber)

        // Then
        assertEquals(2, result.first)
        assertEquals(2, result.second)

        coVerify {
            mockApiService.updateMultipleParametersVisibility(
                stationNumber = stationNumber,
                authToken = "Bearer test_token",
                request = match { request ->
                    request.parameters.all { !it.visible }
                }
            )
        }
    }

    @Test
    fun `updateParameterVisibility should return false on API failure`() = runTest {
        // Given
        val stationNumber = "60000105"
        val parameterCode = "4402"

        coEvery { mockAuthManager.getAuthorizationHeader() } returns "Bearer test_token"
        coEvery {
            mockApiService.updateParameterVisibility(any(), any(), any(), any())
        } returns Response.error(500, mockk(relaxed = true))

        // When
        val result = repository.updateParameterVisibility(stationNumber, parameterCode, true)

        // Then
        assertFalse(result)
    }
}
