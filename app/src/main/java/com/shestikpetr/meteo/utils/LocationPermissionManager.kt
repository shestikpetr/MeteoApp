package com.shestikpetr.meteo.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.yandex.mapkit.MapKitFactory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages location permissions for the application.
 *
 * This class handles requesting location permissions, checking permission status,
 * and coordinating with MapKit lifecycle based on permission results.
 * Extracted from the original MainActivity permission logic.
 */
@Singleton
class LocationPermissionManager @Inject constructor() {

    /**
     * Callback interface for permission results.
     */
    interface PermissionCallback {
        fun onPermissionGranted()
        fun onPermissionDenied()
        fun onPermissionRationale()
    }

    private var permissionCallback: PermissionCallback? = null
    private var activityResultLauncher: ActivityResultLauncher<Array<String>>? = null

    /**
     * Initializes the permission manager with an activity.
     * Should be called in the activity's onCreate method.
     *
     * @param activity The ComponentActivity that will handle permission requests
     * @param callback Optional callback for permission results
     */
    fun initialize(activity: ComponentActivity, callback: PermissionCallback? = null) {
        this.permissionCallback = callback

        // Register the permission request launcher
        activityResultLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            handlePermissionResult(permissions)
        }

        Log.d("LocationPermissionManager", "Initialized with activity: ${activity.javaClass.simpleName}")
    }

    /**
     * Checks if location permissions are granted.
     *
     * @param context The application context
     * @return true if either fine or coarse location permission is granted
     */
    fun hasLocationPermission(context: Context): Boolean {
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocationGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineLocationGranted || coarseLocationGranted
    }

    /**
     * Checks if fine location permission is specifically granted.
     *
     * @param context The application context
     * @return true if fine location permission is granted
     */
    fun hasFineLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Checks if coarse location permission is specifically granted.
     *
     * @param context The application context
     * @return true if coarse location permission is granted
     */
    fun hasCoarseLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Requests location permissions if not already granted.
     * Based on the original requestLocationPermissions logic from MainActivity.
     *
     * @param context The application context for permission checking
     */
    fun requestLocationPermissions(context: Context) {
        if (hasLocationPermission(context)) {
            Log.d("LocationPermissionManager", "Location permissions already granted")
            permissionCallback?.onPermissionGranted()
            startMapKitIfPermissionGranted()
            return
        }

        val launcher = activityResultLauncher
        if (launcher == null) {
            Log.e("LocationPermissionManager", "Permission launcher not initialized. Call initialize() first.")
            permissionCallback?.onPermissionDenied()
            return
        }

        Log.d("LocationPermissionManager", "Requesting location permissions")
        launcher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    /**
     * Handles the result of permission requests.
     *
     * @param permissions Map of requested permissions and their grant status
     */
    private fun handlePermissionResult(permissions: Map<String, Boolean>) {
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        val locationGranted = fineLocationGranted || coarseLocationGranted

        Log.d("LocationPermissionManager", "Permission result - Fine: $fineLocationGranted, Coarse: $coarseLocationGranted")

        if (locationGranted) {
            Log.d("LocationPermissionManager", "Location permission granted")
            permissionCallback?.onPermissionGranted()
            startMapKitIfPermissionGranted()
        } else {
            Log.d("LocationPermissionManager", "Location permission denied")
            permissionCallback?.onPermissionDenied()
        }
    }

    /**
     * Starts MapKit if location permission is granted.
     * Based on the original logic from MainActivity.
     */
    private fun startMapKitIfPermissionGranted() {
        try {
            MapKitFactory.getInstance().onStart()
            Log.d("LocationPermissionManager", "MapKit started after permission grant")
        } catch (e: Exception) {
            Log.e("LocationPermissionManager", "Error starting MapKit: ${e.message}", e)
        }
    }

    /**
     * Gets a detailed permission status for logging and debugging.
     *
     * @param context The application context
     * @return Map containing detailed permission status
     */
    fun getPermissionStatus(context: Context): Map<String, Any> {
        return mapOf(
            "hasFineLocation" to hasFineLocationPermission(context),
            "hasCoarseLocation" to hasCoarseLocationPermission(context),
            "hasAnyLocation" to hasLocationPermission(context),
            "isInitialized" to (activityResultLauncher != null)
        )
    }

    /**
     * Sets the permission callback.
     *
     * @param callback The callback to receive permission results
     */
    fun setPermissionCallback(callback: PermissionCallback?) {
        this.permissionCallback = callback
    }

    /**
     * Clears the permission callback and launcher references.
     * Should be called when the activity is destroyed.
     */
    fun cleanup() {
        Log.d("LocationPermissionManager", "Cleaning up permission manager")
        permissionCallback = null
        activityResultLauncher = null
    }

    /**
     * Checks if the permission manager is properly initialized.
     *
     * @return true if initialized and ready to handle permission requests
     */
    fun isInitialized(): Boolean {
        return activityResultLauncher != null
    }

    /**
     * Gets the list of location permissions that the app requires.
     *
     * @return Array of required location permissions
     */
    fun getRequiredPermissions(): Array<String> {
        return arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    /**
     * Checks if we should show permission rationale to the user.
     *
     * @param activity The activity to check rationale for
     * @return true if rationale should be shown
     */
    fun shouldShowPermissionRationale(activity: ComponentActivity): Boolean {
        return activity.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) ||
               activity.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    /**
     * Forces MapKit restart if permissions are available.
     * Useful for manual permission refresh scenarios.
     *
     * @param context The application context
     */
    fun refreshMapKitWithPermissions(context: Context) {
        if (hasLocationPermission(context)) {
            Log.d("LocationPermissionManager", "Refreshing MapKit with existing permissions")
            startMapKitIfPermissionGranted()
        } else {
            Log.d("LocationPermissionManager", "Cannot refresh MapKit - no location permissions")
        }
    }
}