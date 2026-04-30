package com.shestikpetr.meteoapp.presentation.main.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.shestikpetr.meteoapp.domain.model.ParameterMeta
import com.shestikpetr.meteoapp.domain.model.Station
import com.shestikpetr.meteoapp.domain.model.StationLatest
import com.shestikpetr.meteoapp.ui.map.MapMarkerRenderer
import com.shestikpetr.meteoapp.ui.theme.appColors
import com.shestikpetr.meteoapp.ui.util.formatParameterValue
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

/**
 * OSMdroid-карта. Получает уже отфильтрованный список станций с привязанным
 * StationLatest (через [latestByStation]) и текущим выбранным параметром.
 */
@Composable
fun OsmMapView(
    stations: List<Station>,
    latestByStation: Map<String, StationLatest>,
    selectedParameter: ParameterMeta?,
    activeStationNumber: String?,
    onStationClick: (Station) -> Unit,
    initialCameraSet: Boolean,
    onInitialCameraSet: () -> Unit,
    userLocation: GeoPoint?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val palette = MaterialTheme.appColors

    val mapView = remember {
        MapView(context).apply {
            setMultiTouchControls(true)
            @Suppress("DEPRECATION")
            setBuiltInZoomControls(false)
        }
    }
    val markerRenderer = remember { MapMarkerRenderer(context) }

    // Initial camera
    LaunchedEffect(userLocation, stations) {
        if (!initialCameraSet && (userLocation != null || stations.isNotEmpty())) {
            val target = userLocation ?: stations.firstOrNull()
                ?.takeIf { it.latitude != null && it.longitude != null }
                ?.let { GeoPoint(it.latitude!!, it.longitude!!) }
            target?.let {
                mapView.controller.setZoom(10.0)
                mapView.controller.setCenter(it)
                onInitialCameraSet()
            }
        }
    }

    LaunchedEffect(activeStationNumber, stations) {
        val active = stations.firstOrNull { it.stationNumber == activeStationNumber }
        if (active?.latitude != null && active.longitude != null) {
            mapView.controller.animateTo(GeoPoint(active.latitude, active.longitude), 13.0, 800L)
        }
    }

    LaunchedEffect(stations, latestByStation, selectedParameter, activeStationNumber, palette.isDark) {
        mapView.overlays.clear()
        stations.forEach { station ->
            val lat = station.latitude ?: return@forEach
            val lon = station.longitude ?: return@forEach
            val point = GeoPoint(lat, lon)
            val latest = latestByStation[station.stationNumber]

            val displayText = if (selectedParameter != null) {
                val value = latest?.valueOf(selectedParameter.code)
                val unit = latest?.unitOf(selectedParameter.code) ?: selectedParameter.unit
                if (value != null) "${value.formatParameterValue()} ${unit ?: ""}".trim()
                else "—"
            } else {
                station.name
            }

            val active = station.stationNumber == activeStationNumber
            val marker = Marker(mapView).apply {
                position = point
                icon = markerRenderer.render(displayText, active, palette.isDark)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                setOnMarkerClickListener { _, _ ->
                    onStationClick(station)
                    true
                }
            }
            mapView.overlays.add(marker)
        }
        mapView.invalidate()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}
