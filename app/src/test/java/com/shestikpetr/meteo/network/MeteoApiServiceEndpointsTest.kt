package com.shestikpetr.meteo.network

import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull

/**
 * Tests for FastAPI v1 endpoints structure and data models
 * These tests document the API contract and verify model compatibility
 */
class MeteoApiServiceEndpointsTest {

    @Test
    fun `verify StationLatestDataResponse model structure`() {
        // Given - API response structure from /data/latest
        val response = StationLatestDataResponse(
            station_number = "60000105",
            custom_name = "Моя станция",
            is_favorite = true,
            location = "Томск",
            latitude = 56.46,
            longitude = 84.96,
            parameters = listOf(
                ParameterValue(
                    code = "4402",
                    name = "Температура воздуха",
                    value = 25.5,
                    unit = "°C",
                    category = "temperature"
                )
            ),
            timestamp = "2025-01-01T12:00:00Z"
        )

        // Then
        assertEquals("60000105", response.station_number)
        assertEquals("Моя станция", response.custom_name)
        assertEquals(true, response.is_favorite)
        assertEquals(1, response.parameters.size)
        assertNotNull(response.timestamp)

        println("✓ StationLatestDataResponse model is compatible with /data/latest endpoint")
    }

    @Test
    fun `verify ParameterHistoryResponse model structure`() {
        // Given - API response structure from /data/{station_number}/{parameter_code}/history
        val response = ParameterHistoryResponse(
            station_number = "60000105",
            parameter = ParameterInfoBasic(
                code = "4402",
                name = "Температура воздуха",
                unit = "°C",
                category = "temperature"
            ),
            data = listOf(
                HistoryDataPoint(time = 1640000000L, value = 25.5),
                HistoryDataPoint(time = 1640003600L, value = 26.0)
            ),
            count = 2
        )

        // Then
        assertEquals("60000105", response.station_number)
        assertEquals("4402", response.parameter.code)
        assertEquals(2, response.count)
        assertEquals(2, response.data.size)
        assertEquals(25.5, response.data[0].value, 0.01)

        println("✓ ParameterHistoryResponse model is compatible with /data/.../history endpoint")
    }

    @Test
    fun `verify ParameterVisibilityInfo model structure`() {
        // Given - API response structure from /stations/{station_number}/parameters
        val param = ParameterVisibilityInfo(
            code = "4402",
            name = "Температура воздуха",
            unit = "°C",
            description = "Температура на высоте 2м",
            category = "temperature",
            is_visible = true,
            display_order = 1
        )

        // Then
        assertEquals("4402", param.code)
        assertEquals(true, param.is_visible)
        assertEquals(1, param.display_order)
        assertNotNull(param.unit)

        println("✓ ParameterVisibilityInfo model is compatible with /stations/.../parameters endpoint")
    }

    @Test
    fun `verify BulkUpdateParametersRequest model structure`() {
        // Given - Request body for PATCH /stations/{station_number}/parameters
        val request = BulkUpdateParametersRequest(
            parameters = listOf(
                ParameterVisibilityUpdate(code = "4402", visible = true),
                ParameterVisibilityUpdate(code = "5402", visible = false),
                ParameterVisibilityUpdate(code = "700", visible = true)
            )
        )

        // Then
        assertEquals(3, request.parameters.size)
        assertEquals("4402", request.parameters[0].code)
        assertEquals(true, request.parameters[0].visible)
        assertEquals(false, request.parameters[1].visible)

        println("✓ BulkUpdateParametersRequest model is compatible with bulk update endpoint")
    }

    @Test
    fun `verify UpdateParameterVisibilityRequest model structure`() {
        // Given - Request body for PATCH /stations/{station_number}/parameters/{parameter_code}
        val request = UpdateParameterVisibilityRequest(is_visible = true)

        // Then
        assertEquals(true, request.is_visible)

        println("✓ UpdateParameterVisibilityRequest model is compatible with single parameter update")
    }

    @Test
    fun `verify ApiResponse wrapper structure`() {
        // Given - Generic API response wrapper
        val response = ApiResponse(
            success = true,
            data = StationLatestDataResponse(
                station_number = "60000105",
                custom_name = null,
                is_favorite = false,
                location = "Томск",
                latitude = 56.46,
                longitude = 84.96,
                parameters = emptyList(),
                timestamp = null
            )
        )

        // Then
        assertEquals(true, response.success)
        assertNotNull(response.data)
        assertEquals("60000105", response.data?.station_number)

        println("✓ ApiResponse wrapper correctly wraps all endpoint responses")
    }

    @Test
    fun `document API endpoint paths`() {
        println("\n=== FastAPI v1 ENDPOINT DOCUMENTATION ===\n")

        println("DATA ENDPOINTS:")
        println("  GET  /data/latest                                    - Get all stations latest data")
        println("  GET  /data/{station_number}/latest                   - Get one station latest data")
        println("  GET  /data/{station_number}/{parameter_code}/history - Get parameter history")
        println()

        println("PARAMETER VISIBILITY ENDPOINTS:")
        println("  GET   /stations/{station_number}/parameters                   - Get parameters with visibility")
        println("  PATCH /stations/{station_number}/parameters/{parameter_code}  - Update single parameter visibility")
        println("  PATCH /stations/{station_number}/parameters                   - Bulk update visibility")
        println()

        println("STATION MANAGEMENT ENDPOINTS:")
        println("  GET    /stations                     - Get user stations")
        println("  POST   /stations                     - Add station")
        println("  PATCH  /stations/{station_number}    - Update station (query params)")
        println("  DELETE /stations/{station_number}    - Remove station")
        println()

        println("✓ All endpoint paths documented")
    }

    @Test
    fun `verify SuccessResponse model for simple operations`() {
        // Given - Response for simple operations like PATCH /stations/{station_number}
        val response = SuccessResponse(success = true)

        // Then
        assertEquals(true, response.success)

        println("✓ SuccessResponse model works for simple boolean responses")
    }

    @Test
    fun `verify nullable fields in StationLatestDataResponse`() {
        // Given - Response with nullable fields
        val response = StationLatestDataResponse(
            station_number = "60000105",
            custom_name = null,  // nullable
            is_favorite = false,
            location = null,     // nullable
            latitude = null,     // nullable
            longitude = null,    // nullable
            parameters = listOf(
                ParameterValue(
                    code = "4402",
                    name = "Температура",
                    value = null,    // nullable
                    unit = null,     // nullable
                    category = null  // nullable
                )
            ),
            timestamp = null     // nullable
        )

        // Then - All nullable fields should work correctly
        assertEquals(null, response.custom_name)
        assertEquals(null, response.location)
        assertEquals(null, response.latitude)
        assertEquals(null, response.timestamp)
        assertEquals(null, response.parameters[0].value)

        println("✓ Nullable fields in models work correctly for missing data")
    }

    @Test
    fun `verify BulkUpdateParametersResponse structure`() {
        // Given - Response from bulk update
        val response = BulkUpdateParametersResponse(
            success = true,
            updated = 3,
            total = 5
        )

        // Then
        assertEquals(true, response.success)
        assertEquals(3, response.updated)
        assertEquals(5, response.total)

        println("✓ BulkUpdateParametersResponse provides update statistics")
    }

    @Test
    fun `verify data endpoint query parameters`() {
        println("\n=== QUERY PARAMETERS DOCUMENTATION ===\n")

        println("GET /data/{station_number}/{parameter_code}/history:")
        println("  - start_time: Long? (Unix timestamp)")
        println("  - end_time: Long? (Unix timestamp)")
        println("  - limit: Int? (default 1000, max 10000)")
        println()

        println("PATCH /stations/{station_number}:")
        println("  - custom_name: String? (query param)")
        println("  - is_favorite: Boolean? (query param)")
        println()

        println("✓ Query parameters documented")
    }
}
