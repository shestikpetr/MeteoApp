package com.shestikpetr.meteo.repository.interfaces

import com.shestikpetr.meteo.data.StationWithLocation
import com.shestikpetr.meteo.network.StationInfo
import com.shestikpetr.meteo.network.ParameterInfo

/**
 * Repository interface for weather station operations.
 * This interface follows the Interface Segregation Principle by focusing
 * solely on station-related operations, separated from other repository concerns.
 * It provides abstraction for weather station data retrieval and management.
 */
interface StationRepository {

    /**
     * Retrieves all weather stations available to the authenticated user.
     *
     * @return List of station information including station numbers, names, and locations
     * @throws Exception if the request fails or authentication is invalid
     */
    suspend fun getUserStations(): List<StationInfo>

    /**
     * Retrieves all user stations with parsed geographic coordinates.
     * This method processes the location strings and converts them to latitude/longitude pairs.
     * Includes fallback to demo stations in debug mode if the request fails.
     *
     * @return List of stations with parsed geographic coordinates
     * @throws Exception if the request fails in production mode
     */
    suspend fun getUserStationsWithLocation(): List<StationWithLocation>

    /**
     * Retrieves available parameters for a specific weather station.
     *
     * @param stationId The identifier (complex ID) of the weather station
     * @return List of parameter information including IDs and names
     * @throws Exception if the request fails or authentication is invalid
     */
    suspend fun getStationParameters(stationId: String): List<ParameterInfo>

    /**
     * Retrieves detailed information about a specific weather station.
     *
     * @param stationId The identifier of the weather station
     * @return Station information or null if not found
     * @throws Exception if the request fails or authentication is invalid
     */
    suspend fun getStationInfo(stationId: String): StationInfo?

    /**
     * Checks if a weather station is accessible to the authenticated user.
     *
     * @param stationId The identifier of the weather station
     * @return true if the station is accessible, false otherwise
     */
    suspend fun isStationAccessible(stationId: String): Boolean

    /**
     * Retrieves stations within a geographic bounding box.
     * This is useful for map-based applications that need to show only visible stations.
     *
     * @param northLatitude Northern boundary of the bounding box
     * @param southLatitude Southern boundary of the bounding box
     * @param eastLongitude Eastern boundary of the bounding box
     * @param westLongitude Western boundary of the bounding box
     * @return List of stations within the specified geographic area
     * @throws Exception if the request fails or authentication is invalid
     */
    suspend fun getStationsInBounds(
        northLatitude: Double,
        southLatitude: Double,
        eastLongitude: Double,
        westLongitude: Double
    ): List<StationWithLocation>

    /**
     * Searches for stations by name or station number.
     *
     * @param query Search query (can match station name or number)
     * @return List of stations matching the search criteria
     * @throws Exception if the request fails or authentication is invalid
     */
    suspend fun searchStations(query: String): List<StationInfo>

    /**
     * Retrieves the nearest weather station to a given geographic location.
     *
     * @param latitude Target latitude
     * @param longitude Target longitude
     * @param maxDistanceKm Maximum distance in kilometers to search within
     * @return The nearest station within the specified distance, or null if none found
     * @throws Exception if the request fails or authentication is invalid
     */
    suspend fun getNearestStation(
        latitude: Double,
        longitude: Double,
        maxDistanceKm: Double = 50.0
    ): StationWithLocation?

    /**
     * Refreshes the station cache.
     * This method can be used to force a refresh of cached station data.
     */
    suspend fun refreshStations()
}