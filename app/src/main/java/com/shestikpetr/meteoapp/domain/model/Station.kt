package com.shestikpetr.meteoapp.domain.model

/** Станция, привязанная к аккаунту пользователя. */
data class Station(
    val stationNumber: String,
    val name: String,
    val location: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val altitude: Double? = null
)
