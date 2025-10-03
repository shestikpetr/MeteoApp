package com.shestikpetr.meteo.utils

import android.content.Context
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.shestikpetr.meteo.config.MapConfig
import com.yandex.mapkit.MapKitFactory
import ru.sulgik.mapkit.MapKit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Yandex MapKit lifecycle and initialization.
 *
 * This class handles MapKit API key setup, initialization, and lifecycle management
 * based on configuration system. It provides automatic lifecycle handling when
 * attached to a LifecycleOwner. Now follows SOLID principles by depending on
 * MapConfig abstraction instead of hardcoded values.
 */
@Singleton
class MapKitLifecycleManager @Inject constructor(
    private val mapConfig: MapConfig
) : DefaultLifecycleObserver {

    companion object {
        private const val TAG = "MapKitLifecycleManager"
        private var isApiKeySet = false
        private var isInitialized = false
    }

    private var isStarted = false
    private var context: Context? = null

    /**
     * Sets the MapKit API key if not already set.
     * This is a static operation that only needs to be done once per app lifecycle.
     * Now uses configuration system instead of hardcoded value.
     */
    private fun setApiKeyIfNeeded() {
        if (!isApiKeySet) {
            Log.d(TAG, "Setting MapKit API key from configuration")
            val apiKey = mapConfig.apiKey
            if (apiKey.isNotBlank()) {
                MapKit.setApiKey(apiKey)
                isApiKeySet = true
                Log.d(TAG, "MapKit API key set successfully")
            } else {
                Log.e(TAG, "MapKit API key is empty in configuration!")
                throw IllegalStateException("MapKit API key is not configured")
            }
        }
    }

    /**
     * Initializes MapKit with the provided context.
     * Based on the original logic from MainActivity.onCreate().
     *
     * @param context The application or activity context
     */
    fun initialize(context: Context) {
        this.context = context.applicationContext

        if (isInitialized) {
            Log.d(TAG, "MapKit already initialized")
            return
        }

        try {
            setApiKeyIfNeeded()
            Log.d(TAG, "Initializing MapKit")
            MapKitFactory.initialize(context)
            isInitialized = true
            Log.d(TAG, "MapKit initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing MapKit: ${e.message}", e)
            throw e
        }
    }

    /**
     * Starts MapKit instance.
     * Based on the original logic from MainActivity.onStart().
     */
    fun start() {
        if (!isInitialized) {
            Log.w(TAG, "MapKit not initialized, cannot start")
            return
        }

        if (isStarted) {
            Log.d(TAG, "MapKit already started")
            return
        }

        try {
            Log.d(TAG, "Starting MapKit")
            MapKitFactory.getInstance().onStart()
            isStarted = true
            Log.d(TAG, "MapKit started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting MapKit: ${e.message}", e)
        }
    }

    /**
     * Stops MapKit instance.
     * Based on the original logic from MainActivity.onStop().
     */
    fun stop() {
        if (!isStarted) {
            Log.d(TAG, "MapKit not started, nothing to stop")
            return
        }

        try {
            Log.d(TAG, "Stopping MapKit")
            MapKitFactory.getInstance().onStop()
            isStarted = false
            Log.d(TAG, "MapKit stopped successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping MapKit: ${e.message}", e)
        }
    }

    /**
     * Forces a restart of MapKit if it's currently initialized.
     * Useful for permission changes or configuration updates.
     */
    fun restart() {
        if (!isInitialized) {
            Log.w(TAG, "MapKit not initialized, cannot restart")
            return
        }

        Log.d(TAG, "Restarting MapKit")
        stop()
        start()
    }

    /**
     * Initializes and starts MapKit in one call.
     * Convenience method for simple use cases.
     *
     * @param context The application or activity context
     */
    fun initializeAndStart(context: Context) {
        initialize(context)
        start()
    }

    /**
     * Gets the current state of MapKit.
     *
     * @return Map containing MapKit state information
     */
    fun getState(): Map<String, Any> {
        return mapOf(
            "isApiKeySet" to isApiKeySet,
            "isInitialized" to isInitialized,
            "isStarted" to isStarted,
            "hasContext" to (context != null)
        )
    }

    /**
     * Checks if MapKit is properly initialized and ready to use.
     *
     * @return true if MapKit is initialized and started
     */
    fun isReady(): Boolean {
        return isInitialized && isStarted
    }

    /**
     * Lifecycle observer method - called when the lifecycle owner starts.
     */
    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        Log.d(TAG, "Lifecycle onStart")
        start()
    }

    /**
     * Lifecycle observer method - called when the lifecycle owner stops.
     */
    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        Log.d(TAG, "Lifecycle onStop")
        stop()
    }

    /**
     * Lifecycle observer method - called when the lifecycle owner is destroyed.
     */
    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        Log.d(TAG, "Lifecycle onDestroy")
        cleanup()
    }

    /**
     * Attaches this manager to a lifecycle owner for automatic lifecycle management.
     *
     * @param lifecycleOwner The lifecycle owner (typically an Activity or Fragment)
     */
    fun attachToLifecycle(lifecycleOwner: LifecycleOwner) {
        Log.d(TAG, "Attaching to lifecycle: ${lifecycleOwner.javaClass.simpleName}")
        lifecycleOwner.lifecycle.addObserver(this)
    }

    /**
     * Detaches this manager from a lifecycle owner.
     *
     * @param lifecycleOwner The lifecycle owner to detach from
     */
    fun detachFromLifecycle(lifecycleOwner: LifecycleOwner) {
        Log.d(TAG, "Detaching from lifecycle: ${lifecycleOwner.javaClass.simpleName}")
        lifecycleOwner.lifecycle.removeObserver(this)
    }

    /**
     * Cleans up resources and references.
     */
    private fun cleanup() {
        Log.d(TAG, "Cleaning up MapKit lifecycle manager")
        context = null
        // Note: We don't reset static flags as MapKit state persists across activity recreations
    }

    /**
     * Attempts to safely initialize MapKit with error handling.
     * Returns true if successful, false otherwise.
     *
     * @param context The application or activity context
     * @return true if initialization was successful
     */
    fun safeInitialize(context: Context): Boolean {
        return try {
            initialize(context)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Safe initialization failed: ${e.message}", e)
            false
        }
    }

    /**
     * Attempts to safely start MapKit with error handling.
     * Returns true if successful, false otherwise.
     *
     * @return true if start was successful
     */
    fun safeStart(): Boolean {
        return try {
            start()
            isStarted
        } catch (e: Exception) {
            Log.e(TAG, "Safe start failed: ${e.message}", e)
            false
        }
    }

    /**
     * Gets the API key used for MapKit (for debugging purposes).
     * Now retrieves from configuration instead of hardcoded value.
     *
     * @return The MapKit API key from configuration
     */
    fun getApiKey(): String = mapConfig.apiKey

    /**
     * Gets the default zoom level from configuration.
     *
     * @return The default zoom level
     */
    fun getDefaultZoomLevel(): Float = mapConfig.defaultZoomLevel

    /**
     * Checks if clustering is enabled in configuration.
     *
     * @return true if clustering is enabled
     */
    fun isClusteringEnabled(): Boolean = mapConfig.enableClustering

    /**
     * Checks if the MapKit factory instance is available.
     *
     * @return true if MapKit factory is available
     */
    fun isFactoryAvailable(): Boolean {
        return try {
            MapKitFactory.getInstance()
            true
        } catch (e: Exception) {
            Log.w(TAG, "MapKit factory not available: ${e.message}")
            false
        }
    }
}