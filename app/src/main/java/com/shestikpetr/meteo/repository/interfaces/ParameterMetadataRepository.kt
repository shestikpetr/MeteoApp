package com.shestikpetr.meteo.repository.interfaces

import com.shestikpetr.meteo.network.ParameterMetadata

/**
 * Repository interface for parameter metadata operations.
 * This interface follows the Interface Segregation Principle by focusing
 * solely on parameter metadata-related operations, separated from other repository concerns.
 * It provides abstraction for parameter metadata retrieval and management.
 */
interface ParameterMetadataRepository {

    /**
     * Retrieves metadata for all available parameters.
     * This includes information like parameter names, units, and descriptions.
     *
     * @return Map of parameter ID to parameter metadata
     * @throws Exception if the request fails or authentication is invalid
     */
    suspend fun getParametersMetadata(): Map<String, ParameterMetadata>

    /**
     * Retrieves metadata for a specific parameter.
     *
     * @param parameterId The identifier of the parameter
     * @return Parameter metadata including name, unit, and description
     * @throws Exception if the request fails, parameter is not found, or authentication is invalid
     */
    suspend fun getParameterMetadata(parameterId: String): ParameterMetadata

    /**
     * Retrieves metadata for multiple parameters at once.
     *
     * @param parameterIds List of parameter identifiers
     * @return Map of parameter ID to parameter metadata for the requested parameters
     * @throws Exception if the request fails or authentication is invalid
     */
    suspend fun getMultipleParametersMetadata(parameterIds: List<String>): Map<String, ParameterMetadata>

    /**
     * Checks if a parameter exists in the system.
     *
     * @param parameterId The identifier of the parameter to check
     * @return true if the parameter exists, false otherwise
     */
    suspend fun parameterExists(parameterId: String): Boolean

    /**
     * Retrieves all available parameter IDs.
     *
     * @return Set of all parameter identifiers available in the system
     * @throws Exception if the request fails or authentication is invalid
     */
    suspend fun getAvailableParameterIds(): Set<String>

    /**
     * Searches for parameters by name or description.
     *
     * @param query Search query to match against parameter names and descriptions
     * @return Map of parameter ID to metadata for parameters matching the search criteria
     * @throws Exception if the request fails or authentication is invalid
     */
    suspend fun searchParameters(query: String): Map<String, ParameterMetadata>

    /**
     * Retrieves parameters grouped by category or unit.
     * This is useful for UI organization and parameter selection.
     *
     * @return Map of unit to list of parameters using that unit
     * @throws Exception if the request fails or authentication is invalid
     */
    suspend fun getParametersByUnit(): Map<String, List<ParameterMetadata>>

    /**
     * Retrieves the display name for a parameter.
     * This is a convenience method that returns just the human-readable name.
     *
     * @param parameterId The identifier of the parameter
     * @return The display name of the parameter
     * @throws Exception if the request fails, parameter is not found, or authentication is invalid
     */
    suspend fun getParameterDisplayName(parameterId: String): String

    /**
     * Retrieves the unit for a parameter.
     * This is a convenience method that returns just the unit of measurement.
     *
     * @param parameterId The identifier of the parameter
     * @return The unit of measurement for the parameter
     * @throws Exception if the request fails, parameter is not found, or authentication is invalid
     */
    suspend fun getParameterUnit(parameterId: String): String

    /**
     * Retrieves the description for a parameter.
     * This is a convenience method that returns just the parameter description.
     *
     * @param parameterId The identifier of the parameter
     * @return The description of the parameter
     * @throws Exception if the request fails, parameter is not found, or authentication is invalid
     */
    suspend fun getParameterDescription(parameterId: String): String

    /**
     * Refreshes the parameter metadata cache.
     * This method can be used to force a refresh of cached parameter metadata.
     */
    suspend fun refreshParameterMetadata()
}