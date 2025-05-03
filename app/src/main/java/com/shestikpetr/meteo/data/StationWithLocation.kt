package com.shestikpetr.meteo.data

data class StationWithLocation(
    val stationNumber: String,
    val name: String,
    val latitude: Double,
    val longitude: Double
)