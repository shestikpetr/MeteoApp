package com.shestikpetr.meteoapp.ui.screens.main.components

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
import com.shestikpetr.meteoapp.data.model.ParameterMetadata
import com.shestikpetr.meteoapp.data.model.StationWithData
import com.shestikpetr.meteoapp.ui.map.MapMarkerRenderer
import com.shestikpetr.meteoapp.ui.util.formatParameterValue
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun OsmMapView(
    modifier: Modifier = Modifier,
    stations: List<StationWithData>,
    selectedParameter: ParameterMetadata?,
    userLocation: GeoPoint?,
    initialCameraSet: Boolean,
    onInitialCameraSet: () -> Unit,
    focusedStation: StationWithData?,
    onFocusHandled: () -> Unit,
    onStationClick: (StationWithData) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val mapView = remember {
        MapView(context).apply {
            setMultiTouchControls(true)
            @Suppress("DEPRECATION")
            setBuiltInZoomControls(false)
        }
    }
    val markerRenderer = remember { MapMarkerRenderer(context) }

    LaunchedEffect(userLocation, stations) {
        if (!initialCameraSet && (userLocation != null || stations.isNotEmpty())) {
            val targetPoint = userLocation ?: stations.firstOrNull()?.let {
                GeoPoint(it.latitude, it.longitude)
            }
            targetPoint?.let {
                mapView.controller.setZoom(10.0)
                mapView.controller.setCenter(it)
                onInitialCameraSet()
            }
        }
    }

    LaunchedEffect(focusedStation) {
        focusedStation?.let { station ->
            mapView.controller.animateTo(
                GeoPoint(station.latitude, station.longitude),
                14.0,
                1000L
            )
            onFocusHandled()
        }
    }

    LaunchedEffect(stations, selectedParameter) {
        mapView.overlays.clear()

        stations.forEach { station ->
            val point = GeoPoint(station.latitude, station.longitude)
            val hasData = station.parameterValue != null || selectedParameter == null

            val displayText = if (selectedParameter != null) {
                val value = station.parameterValue?.formatParameterValue() ?: "—"
                if (station.unit != null && station.parameterValue != null) {
                    "$value ${station.unit}"
                } else {
                    value
                }
            } else {
                station.name
            }

            val marker = Marker(mapView).apply {
                position = point
                icon = markerRenderer.render(displayText, hasData)
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
                else -> {}
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
