package com.shestikpetr.meteoapp.util

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.tasks.await
import org.osmdroid.util.GeoPoint

class LocationProvider(private val context: Context) {

    @SuppressLint("MissingPermission")
    suspend fun getLastKnownLocation(): GeoPoint? {
        return try {
            val client = LocationServices.getFusedLocationProviderClient(context)
            val location = client.lastLocation.await()
            location?.let { GeoPoint(it.latitude, it.longitude) }
        } catch (_: Exception) {
            null
        }
    }
}
