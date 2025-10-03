package com.shestikpetr.meteo.repository.impl

import android.util.Log
import com.shestikpetr.meteo.data.StationWithLocation
import com.shestikpetr.meteo.network.AuthManager
import com.shestikpetr.meteo.network.MeteoApiService
import com.shestikpetr.meteo.network.ParameterInfo
import com.shestikpetr.meteo.network.StationInfo
import com.shestikpetr.meteo.repository.interfaces.StationRepository
import com.yandex.maps.mobile.BuildConfig
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sqrt

/**
 * Network implementation of StationRepository interface.
 * This implementation extracts station-related functionality from the original
 * NetworkMeteoRepository and focuses solely on station operations.
 */
@Singleton
class NetworkStationRepository @Inject constructor(
    private val meteoApiService: MeteoApiService,
    private val authManager: AuthManager
) : StationRepository {

    // Cache for stations to avoid repeated network calls
    private var cachedStations: List<StationInfo>? = null
    private var cachedStationsWithLocation: List<StationWithLocation>? = null
    private val cacheMutex = Mutex()

    override suspend fun getUserStations(): List<StationInfo> {
        return cacheMutex.withLock {
            if (cachedStations == null) {
                try {
                    val authHeader = authManager.getAuthorizationHeader()
                        ?: throw SecurityException("No valid authentication token")
                    val response = meteoApiService.getUserStations(authHeader)
                    if (!response.isSuccessful || response.body()?.success != true) {
                        throw RuntimeException("Failed to get user stations")
                    }
                    cachedStations = response.body()?.data ?: emptyList()
                    Log.d("StationRepository", "Loaded ${cachedStations?.size} stations from API")
                } catch (e: Exception) {
                    Log.e("StationRepository", "Error loading stations: ${e.message}", e)
                    throw e
                }
            }
            cachedStations ?: emptyList()
        }
    }

    override suspend fun getUserStationsWithLocation(): List<StationWithLocation> {
        return cacheMutex.withLock {
            if (cachedStationsWithLocation == null) {
                try {
                    val authHeader = authManager.getAuthorizationHeader()
                        ?: throw SecurityException("No valid authentication token")
                    Log.d("StationRepository", "Requesting stations with auth header")

                    val response = meteoApiService.getUserStations(authHeader)
                    if (!response.isSuccessful || response.body()?.success != true) {
                        throw RuntimeException("Failed to get user stations")
                    }
                    val stations = response.body()?.data ?: emptyList()
                    Log.d("StationRepository", "Got stations: ${stations.size}")

                    // Filter stations with null fields before mapping
                    cachedStationsWithLocation = stations
                        .filter { station ->
                            val isValid = station.station_number.isNotEmpty() && station.location.isNotEmpty()
                            if (!isValid) {
                                Log.w("StationRepository", "Skipped station with incomplete data: $station")
                            }
                            isValid
                        }
                        .map { station ->
                            StationWithLocation(
                                stationNumber = station.station_number,
                                name = station.name ?: station.station_number,
                                latitude = station.getLatitudeDouble(),
                                longitude = station.getLongitudeDouble(),
                                customName = station.custom_name,
                                location = station.location,
                                isFavorite = station.is_favorite
                            )
                        }
                } catch (e: Exception) {
                    Log.e("StationRepository", "Error loading stations with location: ${e.message}", e)

                    // In debug mode return demo stations
                    if (BuildConfig.DEBUG) {
                        Log.d("StationRepository", "Returning test stations for debug")
                        cachedStationsWithLocation = createDemoStations()
                    } else {
                        throw e
                    }
                }
            }
            cachedStationsWithLocation ?: emptyList()
        }
    }

    override suspend fun getStationParameters(stationId: String): List<ParameterInfo> {
        // First try to get parameters from already loaded stations data
        try {
            val stationInfo = getStationInfo(stationId)
            if (stationInfo != null) {
                val activeParameters = stationInfo.getParameterInfoList()
                if (activeParameters.isNotEmpty()) {
                    Log.d("StationRepository", "Using ${activeParameters.size} active parameters from stations cache for $stationId")
                    return activeParameters
                }
            }
        } catch (e: Exception) {
            Log.w("StationRepository", "Could not get parameters from station cache: ${e.message}")
        }

        // Fallback to API call if needed
        val authHeader = authManager.getAuthorizationHeader()
            ?: throw SecurityException("No valid authentication token")
        val response = meteoApiService.getStationParameters(stationId, authHeader)
        if (!response.isSuccessful || response.body()?.success != true) {
            throw RuntimeException("Failed to get station parameters")
        }

        // Convert ParameterVisibilityInfo to ParameterInfo
        val visibilityParams = response.body()?.data ?: emptyList()
        return visibilityParams.map { visParam ->
            ParameterInfo(
                code = visParam.code,
                name = visParam.name,
                unit = visParam.unit ?: "",
                description = visParam.description ?: "",
                category = visParam.category ?: ""
            )
        }
    }

    override suspend fun getStationInfo(stationId: String): StationInfo? {
        return try {
            val stations = getUserStations()
            stations.find { it.station_number == stationId }
        } catch (e: Exception) {
            Log.w("StationRepository", "Error getting station info for $stationId: ${e.message}")
            null
        }
    }

    override suspend fun isStationAccessible(stationId: String): Boolean {
        return try {
            val stations = getUserStations()
            stations.any { it.station_number == stationId }
        } catch (e: Exception) {
            Log.w("StationRepository", "Error checking station accessibility for $stationId: ${e.message}")
            false
        }
    }

    override suspend fun getStationsInBounds(
        northLatitude: Double,
        southLatitude: Double,
        eastLongitude: Double,
        westLongitude: Double
    ): List<StationWithLocation> {
        val allStations = getUserStationsWithLocation()
        return allStations.filter { station ->
            station.latitude in southLatitude..northLatitude &&
                    station.longitude in westLongitude..eastLongitude
        }
    }

    override suspend fun searchStations(query: String): List<StationInfo> {
        val allStations = getUserStations()
        val lowercaseQuery = query.lowercase()

        return allStations.filter { station ->
            station.station_number.lowercase().contains(lowercaseQuery) ||
                    station.name?.lowercase()?.contains(lowercaseQuery) == true
        }
    }

    override suspend fun getNearestStation(
        latitude: Double,
        longitude: Double,
        maxDistanceKm: Double
    ): StationWithLocation? {
        val allStations = getUserStationsWithLocation()

        var nearestStation: StationWithLocation? = null
        var minDistance = Double.MAX_VALUE

        allStations.forEach { station ->
            val distance = calculateDistance(latitude, longitude, station.latitude, station.longitude)
            if (distance <= maxDistanceKm && distance < minDistance) {
                minDistance = distance
                nearestStation = station
            }
        }

        return nearestStation
    }

    override suspend fun refreshStations() {
        cacheMutex.withLock {
            cachedStations = null
            cachedStationsWithLocation = null
        }
        // Preload fresh data
        getUserStations()
        getUserStationsWithLocation()
    }

    // Function to create test data
    private fun createDemoStations(): List<StationWithLocation> {
        return listOf(
            StationWithLocation(
                stationNumber = "60000105",
                name = "60000105",
                latitude = 56.460850,
                longitude = 84.962327,
                customName = "Demo 105",
                location = "Томск",
                isFavorite = false
            ),
            StationWithLocation(
                stationNumber = "60000104",
                name = "60000104",
                latitude = 56.460039,
                longitude = 84.962282,
                customName = "Demo 104",
                location = "Новосибирск",
                isFavorite = false
            ),
            StationWithLocation(
                stationNumber = "50000022",
                name = "50000022",
                latitude = 56.460337,
                longitude = 84.961591,
                customName = "Demo 022",
                location = "Красноярск",
                isFavorite = false
            )
        )
    }

    private fun parseLocation(location: String): Pair<Double, Double> {
        try {
            if (location.isBlank()) {
                Log.w("StationRepository", "Got empty location string")
                return Pair(56.460337, 84.961591) // Default values
            }

            val parts = location.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .map {
                    try {
                        it.toDouble()
                    } catch (e: NumberFormatException) {
                        Log.e("StationRepository", "Error converting coordinate: $it", e)
                        0.0 // Default value on conversion error
                    }
                }

            if (parts.size >= 2) {
                return Pair(parts[0], parts[1])
            } else {
                Log.w("StationRepository", "Invalid coordinate format: $location")
            }
        } catch (e: Exception) {
            Log.e("StationRepository", "Error parsing coordinates: $location", e)
        }

        // Default values for Tomsk center
        return Pair(56.460337, 84.961591)
    }

    /**
     * Calculate distance between two geographic points using simplified formula.
     * This is an approximation suitable for short distances.
     */
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val deltaLatKm = (lat2 - lat1) * 111.0 // 1 degree latitude ≈ 111 km
        val deltaLonKm = (lon2 - lon1) * 111.0 * cos(Math.toRadians((lat1 + lat2) / 2))

        return sqrt(deltaLatKm * deltaLatKm + deltaLonKm * deltaLonKm)
    }
}