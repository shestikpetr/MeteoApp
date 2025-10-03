package com.shestikpetr.meteo.repository.interfaces

import com.shestikpetr.meteo.model.ParameterConfig
import com.shestikpetr.meteo.model.ParameterConfigSet

/**
 * Repository interface for parameter display and configuration operations.
 * This interface follows the Interface Segregation Principle by focusing
 * solely on parameter display configuration, separated from metadata operations.
 * It provides abstraction for parameter configuration retrieval and management.
 */
interface ParameterDisplayRepository {

    /**
     * Retrieves parameter configuration set for a specific station.
     * This includes display-specific information like localized names, units, and ordering.
     *
     * @param stationNumber The station identifier
     * @param locale The locale for localized parameter names (default: "ru")
     * @return ParameterConfigSet with station-specific parameters
     * @throws Exception if the request fails or authentication is invalid
     */
    suspend fun getStationParameterConfig(stationNumber: String, locale: String = "ru"): ParameterConfigSet

    /**
     * Retrieves global parameter configuration for all available parameters.
     * This can be used as a reference for all possible parameter types.
     *
     * @param locale The locale for localized parameter names (default: "ru")
     * @return ParameterConfigSet with global parameters
     * @throws Exception if the request fails or authentication is invalid
     */
    suspend fun getGlobalParameterConfig(locale: String = "ru"): ParameterConfigSet

    /**
     * Retrieves configuration for a specific parameter.
     *
     * @param parameterCode The parameter code
     * @param locale The locale for localized parameter names (default: "ru")
     * @return ParameterConfig or null if not found
     * @throws Exception if the request fails or authentication is invalid
     */
    suspend fun getParameterConfig(parameterCode: String, locale: String = "ru"): ParameterConfig?

    /**
     * Retrieves configuration for multiple parameters at once.
     *
     * @param parameterCodes List of parameter codes
     * @param locale The locale for localized parameter names (default: "ru")
     * @return Map of parameter code to configuration
     * @throws Exception if the request fails or authentication is invalid
     */
    suspend fun getMultipleParameterConfigs(parameterCodes: List<String>, locale: String = "ru"): Map<String, ParameterConfig>

    /**
     * Checks if a parameter is available for a specific station.
     *
     * @param stationNumber The station identifier
     * @param parameterCode The parameter code to check
     * @return true if the parameter is available for the station, false otherwise
     */
    suspend fun isParameterAvailableForStation(stationNumber: String, parameterCode: String): Boolean

    /**
     * Retrieves the default parameter for a station.
     * This is useful for initial parameter selection in UI.
     *
     * @param stationNumber The station identifier
     * @return ParameterConfig for default parameter or null if no defaults
     */
    suspend fun getDefaultParameterForStation(stationNumber: String): ParameterConfig?

    /**
     * Retrieves parameters grouped by category for UI organization.
     *
     * @param stationNumber The station identifier (optional - if null, returns global categories)
     * @param locale The locale for localized parameter names (default: "ru")
     * @return Map of category to list of parameters
     * @throws Exception if the request fails or authentication is invalid
     */
    suspend fun getParametersByCategory(stationNumber: String? = null, locale: String = "ru"): Map<String, List<ParameterConfig>>

    /**
     * Retrieves the display text for a parameter (name + unit).
     * This is a convenience method for UI display.
     *
     * @param parameterCode The parameter code
     * @param locale The locale for localized parameter names (default: "ru")
     * @return The display text for the parameter or null if not found
     */
    suspend fun getParameterDisplayText(parameterCode: String, locale: String = "ru"): String?

    /**
     * Retrieves just the unit for a parameter.
     * This is a convenience method for UI display.
     *
     * @param parameterCode The parameter code
     * @return The unit of measurement for the parameter or null if not found
     */
    suspend fun getParameterUnit(parameterCode: String): String?

    /**
     * Searches for parameters by name matching.
     *
     * @param query Search query to match against parameter names
     * @param stationNumber Optional station number to limit search scope
     * @param locale The locale for localized parameter names (default: "ru")
     * @return List of matching parameter configurations
     * @throws Exception if the request fails or authentication is invalid
     */
    suspend fun searchParameters(query: String, stationNumber: String? = null, locale: String = "ru"): List<ParameterConfig>

    /**
     * Refreshes the parameter configuration cache.
     * This method can be used to force a refresh of cached parameter configurations.
     *
     * @param stationNumber Optional station number to refresh specific station cache
     */
    suspend fun refreshParameterConfigs(stationNumber: String? = null)

    /**
     * Clears the parameter configuration cache.
     * This method clears all cached parameter configurations.
     */
    suspend fun clearParameterConfigCache()
}