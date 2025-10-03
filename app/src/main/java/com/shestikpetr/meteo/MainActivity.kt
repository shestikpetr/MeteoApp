package com.shestikpetr.meteo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.shestikpetr.meteo.ui.navigation.MeteoApp
import com.shestikpetr.meteo.ui.theme.MeteoTheme
import com.shestikpetr.meteo.utils.LocationPermissionManager
import com.shestikpetr.meteo.utils.MapKitLifecycleManager
import com.shestikpetr.meteo.localization.interfaces.StringResourceManager
import com.shestikpetr.meteo.localization.compose.LocalizationProvider
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var locationPermissionManager: LocationPermissionManager

    @Inject
    lateinit var mapKitLifecycleManager: MapKitLifecycleManager

    @Inject
    lateinit var stringResourceManager: StringResourceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize MapKit
        mapKitLifecycleManager.initialize(this)

        // Initialize location permission manager
        locationPermissionManager.initialize(this)

        // Attach lifecycle management
        mapKitLifecycleManager.attachToLifecycle(this)

        // Request location permissions
        locationPermissionManager.requestLocationPermissions(this)

        setContent {
            MeteoTheme {
                LocalizationProvider(stringResourceManager) {
                    MeteoApp()
                }
            }
        }
    }

    // Permission handling is now managed by LocationPermissionManager

    // MapKit lifecycle is now managed automatically by MapKitLifecycleManager
    // through lifecycle observers

    override fun onDestroy() {
        // Clean up managers
        locationPermissionManager.cleanup()
        mapKitLifecycleManager.detachFromLifecycle(this)
        super.onDestroy()
    }
}