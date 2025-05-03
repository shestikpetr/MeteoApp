package com.shestikpetr.meteo.data

import ru.sulgik.mapkit.geometry.Point
import ru.sulgik.mapkit.map.CameraPosition


object Datasource {

    val startPosition = CameraPosition(
        target = Point(56.460337, 84.961591),
        zoom = 15.0f,
        azimuth = 0.0f,
        tilt = 0.0f
    )

    // Айди и координаты комлексов
    val placemarks = listOf(
        "50000022" to Point(56.460337, 84.961591),
        "60000104" to Point(56.460039, 84.962282),
        "60000105" to Point(56.460850, 84.962327)
    )
}