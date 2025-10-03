package com.shestikpetr.meteo.utils

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.shestikpetr.meteo.data.StationWithLocation
import com.shestikpetr.meteo.network.StationInfo
import com.yandex.maps.mobile.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class for transforming and validating station data.
 *
 * This class handles parsing coordinates, transforming station data between different formats,
 * and validation of station information based on the original logic from NetworkMeteoRepository.
 */
@Singleton
class StationDataTransformer @Inject constructor() {

    companion object {
        // Default coordinates for center of Tomsk, Russia
        private const val DEFAULT_LATITUDE = 56.460337
        private const val DEFAULT_LONGITUDE = 84.961591

        // Validation constants
        private const val MIN_LATITUDE = -90.0
        private const val MAX_LATITUDE = 90.0
        private const val MIN_LONGITUDE = -180.0
        private const val MAX_LONGITUDE = 180.0
    }

    /**
     * Result wrapper for transformation operations.
     */
    sealed class TransformResult<T> {
        data class Success<T>(val data: T) : TransformResult<T>()
        data class Error<T>(val message: String, val exception: Exception? = null) : TransformResult<T>()
    }

    /**
     * Transforms a list of StationInfo objects to StationWithLocation objects.
     * Filters out invalid stations and handles coordinate parsing.
     *
     * @param stations The list of station info from the API
     * @return TransformResult containing the transformed stations or error information
     */
    fun transformStationsWithLocation(stations: List<StationInfo>): TransformResult<List<StationWithLocation>> {
        try {
            Log.d("StationDataTransformer", "Transforming ${stations.size} stations")

            val validStations = stations
                .filter { station ->
                    val isValid = isValidStation(station)
                    if (!isValid) {
                        Log.w("StationDataTransformer", "Skipping invalid station: $station")
                    }
                    isValid
                }
                .mapNotNull { station ->
                    transformSingleStation(station)
                }

            Log.d("StationDataTransformer", "Successfully transformed ${validStations.size} stations")
            return TransformResult.Success(validStations)

        } catch (e: Exception) {
            Log.e("StationDataTransformer", "Error transforming stations: ${e.message}", e)
            return TransformResult.Error("Failed to transform stations", e)
        }
    }

    /**
     * Transforms a single StationInfo to StationWithLocation.
     *
     * @param station The station info to transform
     * @return The transformed station or null if transformation fails
     */
    private fun transformSingleStation(station: StationInfo): StationWithLocation? {
        return try {
            StationWithLocation(
                stationNumber = station.station_number,
                name = station.name ?: station.station_number,
                latitude = station.getLatitudeDouble(),
                longitude = station.getLongitudeDouble(),
                customName = station.custom_name,
                location = station.location,
                isFavorite = station.is_favorite
            )
        } catch (e: Exception) {
            Log.e("StationDataTransformer", "Failed to transform station ${station.station_number}: ${e.message}")
            null
        }
    }

    /**
     * Validates if a station has required non-null fields.
     *
     * @param station The station to validate
     * @return true if station is valid, false otherwise
     */
    fun isValidStation(station: StationInfo): Boolean {
        return station.station_number.isNotEmpty() && station.location.isNotEmpty()
    }

    /**
     * Parses location string into latitude and longitude coordinates.
     * Based on the original parseLocation logic from NetworkMeteoRepository.
     *
     * @param location The location string (e.g., "56.460337, 84.961591")
     * @return Pair of latitude and longitude
     */
    fun parseLocation(location: String): Pair<Double, Double> {
        try {
            if (location.isBlank()) {
                Log.w("StationDataTransformer", "Empty location string provided")
                return getDefaultCoordinates()
            }

            val parts = location.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .map { coordinateString ->
                    try {
                        coordinateString.toDouble()
                    } catch (e: NumberFormatException) {
                        Log.e("StationDataTransformer", "Error parsing coordinate: $coordinateString", e)
                        0.0 // Default value for parsing errors
                    }
                }

            if (parts.size >= 2) {
                val latitude = parts[0]
                val longitude = parts[1]

                // Validate coordinates are within valid ranges
                if (isValidCoordinate(latitude, longitude)) {
                    return Pair(latitude, longitude)
                } else {
                    Log.w("StationDataTransformer", "Invalid coordinates: lat=$latitude, lon=$longitude")
                }
            } else {
                Log.w("StationDataTransformer", "Invalid location format: $location")
            }
        } catch (e: Exception) {
            Log.e("StationDataTransformer", "Error parsing location: $location", e)
        }

        // Return default coordinates for any error case
        return getDefaultCoordinates()
    }

    /**
     * Validates that latitude and longitude are within valid ranges.
     *
     * @param latitude The latitude to validate
     * @param longitude The longitude to validate
     * @return true if coordinates are valid, false otherwise
     */
    fun isValidCoordinate(latitude: Double, longitude: Double): Boolean {
        return latitude in MIN_LATITUDE..MAX_LATITUDE &&
               longitude in MIN_LONGITUDE..MAX_LONGITUDE
    }

    /**
     * Gets default coordinates (center of Tomsk).
     *
     * @return Pair of default latitude and longitude
     */
    fun getDefaultCoordinates(): Pair<Double, Double> {
        return Pair(DEFAULT_LATITUDE, DEFAULT_LONGITUDE)
    }

    /**
     * Extracts a numeric value from a raw API response string.
     * Based on the original extractValueFromResponse logic.
     *
     * @param response The raw response string
     * @return The extracted numeric value or null if extraction fails
     */
    fun extractValueFromResponse(response: String): Double? {
        return try {
            val trimmedResponse = response.trim()

            if (trimmedResponse.startsWith("{")) {
                // Try to parse as JSON
                extractValueFromJson(trimmedResponse)
            } else {
                // Try to extract number from plain text
                extractNumberFromText(trimmedResponse)
            }
        } catch (e: Exception) {
            Log.e("StationDataTransformer", "Error extracting value from '$response': ${e.message}")
            null
        }
    }

    /**
     * Extracts value from JSON response.
     *
     * @param jsonString The JSON string to parse
     * @return The extracted value or null
     */
    private fun extractValueFromJson(jsonString: String): Double? {
        return try {
            val gson = Gson()
            val jsonObject = gson.fromJson(jsonString, JsonObject::class.java)
            jsonObject.get("value")?.asDouble
        } catch (e: Exception) {
            Log.e("StationDataTransformer", "Error parsing JSON: $jsonString", e)
            null
        }
    }

    /**
     * Extracts a number from plain text using regex.
     *
     * @param text The text to extract number from
     * @return The extracted number or null
     */
    private fun extractNumberFromText(text: String): Double? {
        return try {
            val numberPattern = "-?\\d+(?:\\.\\d+)?".toRegex()
            val match = numberPattern.find(text)
            match?.value?.toDouble()
        } catch (e: Exception) {
            Log.e("StationDataTransformer", "Error extracting number from text: $text", e)
            null
        }
    }

    /**
     * Creates demo stations for testing purposes.
     * Based on the original createDemoStations logic.
     *
     * @return List of demo stations
     */
    fun createDemoStations(): List<StationWithLocation> {
        Log.d("StationDataTransformer", "Creating demo stations for testing")
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

    /**
     * Creates emergency stations when all other data loading fails.
     * Based on the original createEmergencyStations logic.
     *
     * @return List of emergency stations
     */
    fun createEmergencyStations(): List<StationWithLocation> {
        Log.d("StationDataTransformer", "Creating emergency stations")
        return listOf(
            StationWithLocation(
                stationNumber = "60000105",
                name = "60000105",
                latitude = 56.460850,
                longitude = 84.962327,
                customName = "Томск (тестовая станция)",
                location = "Томск",
                isFavorite = false
            )
        )
    }

    /**
     * Determines if demo stations should be used based on debug configuration.
     *
     * @return true if demo stations should be used, false otherwise
     */
    fun shouldUseDemoStations(): Boolean {
        return BuildConfig.DEBUG
    }

    /**
     * Validates a complete StationWithLocation object.
     *
     * @param station The station to validate
     * @return true if station is valid, false otherwise
     */
    fun isValidStationWithLocation(station: StationWithLocation): Boolean {
        return station.stationNumber.isNotEmpty() &&
               station.name.isNotEmpty() &&
               isValidCoordinate(station.latitude, station.longitude)
    }

    /**
     * Calculates the distance between two coordinate points (rough estimation).
     *
     * @param lat1 First latitude
     * @param lon1 First longitude
     * @param lat2 Second latitude
     * @param lon2 Second longitude
     * @return Approximate distance in kilometers
     */
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371.0 // Earth's radius in kilometers

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)

        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

        return earthRadius * c
    }
}