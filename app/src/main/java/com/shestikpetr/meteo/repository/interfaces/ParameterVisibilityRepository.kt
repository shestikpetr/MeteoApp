package com.shestikpetr.meteo.repository.interfaces

import com.shestikpetr.meteo.network.ParameterVisibilityInfo

/**
 * Repository interface for parameter visibility operations.
 * This interface follows the Interface Segregation Principle by focusing
 * solely on parameter visibility management, separated from other parameter concerns.
 * It provides abstraction for user-specific parameter visibility settings.
 */
interface ParameterVisibilityRepository {

    /**
     * Retrieves all parameters for a station with visibility information.
     *
     * @param stationNumber The 8-digit station number
     * @return List of parameters with visibility info (is_visible, display_order)
     * @throws Exception if the request fails or authentication is invalid
     */
    suspend fun getStationParametersWithVisibility(stationNumber: String): List<ParameterVisibilityInfo>

    /**
     * Updates visibility of a single parameter for the user.
     *
     * @param stationNumber The 8-digit station number
     * @param parameterCode The parameter code (e.g., "4402")
     * @param isVisible Whether the parameter should be visible to the user
     * @return true if update was successful, false otherwise
     * @throws Exception if the request fails or authentication is invalid
     */
    suspend fun updateParameterVisibility(
        stationNumber: String,
        parameterCode: String,
        isVisible: Boolean
    ): Boolean

    /**
     * Updates visibility of multiple parameters at once (bulk update).
     *
     * @param stationNumber The 8-digit station number
     * @param parameterUpdates Map of parameter code to visibility status
     * @return Pair of (updated count, total count)
     * @throws Exception if the request fails or authentication is invalid
     */
    suspend fun updateMultipleParametersVisibility(
        stationNumber: String,
        parameterUpdates: Map<String, Boolean>
    ): Pair<Int, Int>

    /**
     * Gets only visible parameters for a station.
     *
     * @param stationNumber The 8-digit station number
     * @return List of visible parameter codes
     * @throws Exception if the request fails or authentication is invalid
     */
    suspend fun getVisibleParameters(stationNumber: String): List<String>

    /**
     * Checks if a parameter is visible for the user.
     *
     * @param stationNumber The 8-digit station number
     * @param parameterCode The parameter code
     * @return true if visible, false otherwise
     * @throws Exception if the request fails or authentication is invalid
     */
    suspend fun isParameterVisible(stationNumber: String, parameterCode: String): Boolean

    /**
     * Shows all parameters for a station (sets all to visible).
     *
     * @param stationNumber The 8-digit station number
     * @return Pair of (updated count, total count)
     * @throws Exception if the request fails or authentication is invalid
     */
    suspend fun showAllParameters(stationNumber: String): Pair<Int, Int>

    /**
     * Hides all parameters for a station (sets all to hidden).
     *
     * @param stationNumber The 8-digit station number
     * @return Pair of (updated count, total count)
     * @throws Exception if the request fails or authentication is invalid
     */
    suspend fun hideAllParameters(stationNumber: String): Pair<Int, Int>
}
