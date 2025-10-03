package com.shestikpetr.meteo.repository.interfaces

import com.shestikpetr.meteo.network.SensorDataPoint

/**
 * Repository interface for sensor data operations.
 * This interface follows the Interface Segregation Principle by focusing
 * solely on sensor data-related operations, separated from other repository concerns.
 * It provides abstraction for sensor data retrieval and management.
 */
interface SensorDataRepository {

    /**
     * Retrieves sensor data for a specific complex and parameter within a time range.
     *
     * @param stationNumber The 8-digit station number
     * @param parameter The parameter to retrieve data for (e.g., "temperature", "humidity")
     * @param startTime Optional start time for the data range (Unix timestamp in milliseconds)
     * @param endTime Optional end time for the data range (Unix timestamp in milliseconds)
     * @return List of sensor data points within the specified time range
     * @throws Exception if the request fails or authentication is invalid
     */
    suspend fun getSensorData(
        stationNumber: String,
        parameter: String,
        startTime: Long? = null,
        endTime: Long? = null
    ): List<SensorDataPoint>

    /**
     * Retrieves the latest sensor data value for a specific complex and parameter.
     * This method includes retry logic and error handling for robust data retrieval.
     *
     * @param stationNumber The 8-digit station number
     * @param parameter The parameter to retrieve the latest value for
     * @return The latest sensor value, or a fallback value (-99.0) if data is unavailable
     * @throws Exception if the request fails due to authentication or network issues
     */
    suspend fun getLatestSensorData(
        stationNumber: String,
        parameter: String
    ): Double

    /**
     * Checks if the given value represents unavailable data (fallback value).
     *
     * @param value The sensor data value to check
     * @return true if the value represents unavailable data, false otherwise
     */
    fun isDataUnavailable(value: Double): Boolean

    /**
     * Retrieves sensor data for multiple parameters at once.
     * This is useful for dashboard views that need to display multiple parameters simultaneously.
     *
     * @param stationNumber The 8-digit station number
     * @param parameters List of parameters to retrieve data for
     * @param startTime Optional start time for the data range (Unix timestamp in milliseconds)
     * @param endTime Optional end time for the data range (Unix timestamp in milliseconds)
     * @return Map of parameter name to list of sensor data points
     * @throws Exception if the request fails or authentication is invalid
     */
    suspend fun getMultiParameterData(
        stationNumber: String,
        parameters: List<String>,
        startTime: Long? = null,
        endTime: Long? = null
    ): Map<String, List<SensorDataPoint>>

    /**
     * Retrieves the latest values for multiple parameters at once.
     * This is useful for displaying current conditions across multiple sensors.
     *
     * @param stationNumber The 8-digit station number
     * @param parameters List of parameters to retrieve latest values for
     * @return Map of parameter name to latest value
     * @throws Exception if the request fails or authentication is invalid
     */
    suspend fun getLatestMultiParameterData(
        stationNumber: String,
        parameters: List<String>
    ): Map<String, Double>

    /**
     * Checks if sensor data is available for a specific complex and parameter.
     *
     * @param stationNumber The 8-digit station number
     * @param parameter The parameter to check availability for
     * @return true if data is available, false otherwise
     */
    suspend fun isDataAvailable(
        stationNumber: String,
        parameter: String
    ): Boolean

    /**
     * Gets the time range (earliest and latest timestamps) for available sensor data.
     *
     * @param stationNumber The 8-digit station number
     * @param parameter The parameter to get the time range for
     * @return Pair of (earliest timestamp, latest timestamp) or null if no data available
     * @throws Exception if the request fails or authentication is invalid
     */
    suspend fun getDataTimeRange(
        stationNumber: String,
        parameter: String
    ): Pair<Long?, Long?>

    /**
     * Gets the latest data from all user stations at once.
     * This is useful for map display where all stations need to be updated.
     *
     * @return Map of station number to map of parameter to latest value
     * @throws Exception if the request fails or authentication is invalid
     */
    suspend fun getAllStationsLatestData(): Map<String, Map<String, Double>>

    /**
     * Gets the complete station data with coordinates and latest sensor values.
     * This is the primary method for map display using /api/v1/data/latest endpoint.
     *
     * @return Pair of (list of stations with locations, map of station number to parameter data)
     * @throws Exception if the request fails or authentication is invalid
     */
    suspend fun getAllStationsWithLocationAndData(): Pair<List<com.shestikpetr.meteo.data.StationWithLocation>, Map<String, Map<String, Double>>>
}