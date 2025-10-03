package com.shestikpetr.meteo.data

data class StationWithLocation(
    val stationNumber: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val customName: String? = null,
    val location: String? = null,
    val isFavorite: Boolean = false
) {
    /**
     * Get display name - prioritizes custom name over system name
     */
    fun getDisplayName(): String = customName ?: name
}