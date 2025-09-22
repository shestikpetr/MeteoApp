package com.shestikpetr.meteo.utils

import android.content.Context
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.yandex.mapkit.MapKitFactory
import ru.sulgik.mapkit.MapKit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Yandex MapKit lifecycle and initialization.
 *
 * This class handles MapKit API key setup, initialization, and lifecycle management
 * based on the original logic from MainActivity. It provides automatic lifecycle
 * handling when attached to a LifecycleOwner.
 */
@Singleton
class MapKitLifecycleManager @Inject constructor() : DefaultLifecycleObserver {

    companion object {
        private const val API_KEY = "e6cb4f2f-1295-4ffe-bfca-8ab2b9533d6a"
        private var isApiKeySet = false
        private var isInitialized = false
    }

    private var isStarted = false
    private var context: Context? = null

    /**
     * Sets the MapKit API key if not already set.
     * This is a static operation that only needs to be done once per app lifecycle.
     */
    private fun setApiKeyIfNeeded() {
        if (!isApiKeySet) {
            Log.d("MapKitLifecycleManager", "Setting MapKit API key")
            MapKit.setApiKey(API_KEY)
            isApiKeySet = true
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
            Log.d("MapKitLifecycleManager", "MapKit already initialized")
            return
        }

        try {
            setApiKeyIfNeeded()
            Log.d("MapKitLifecycleManager", "Initializing MapKit")
            MapKitFactory.initialize(context)
            isInitialized = true
            Log.d("MapKitLifecycleManager", "MapKit initialized successfully")
        } catch (e: Exception) {
            Log.e("MapKitLifecycleManager", "Error initializing MapKit: ${e.message}", e)
            throw e
        }
    }

    /**
     * Starts MapKit instance.
     * Based on the original logic from MainActivity.onStart().
     */
    fun start() {
        if (!isInitialized) {
            Log.w("MapKitLifecycleManager", "MapKit not initialized, cannot start")
            return
        }

        if (isStarted) {
            Log.d("MapKitLifecycleManager", "MapKit already started")
            return
        }

        try {
            Log.d("MapKitLifecycleManager", "Starting MapKit")
            MapKitFactory.getInstance().onStart()
            isStarted = true
            Log.d("MapKitLifecycleManager", "MapKit started successfully")
        } catch (e: Exception) {
            Log.e("MapKitLifecycleManager", "Error starting MapKit: ${e.message}", e)
        }
    }

    /**
     * Stops MapKit instance.
     * Based on the original logic from MainActivity.onStop().
     */
    fun stop() {
        if (!isStarted) {
            Log.d("MapKitLifecycleManager", "MapKit not started, nothing to stop")
            return
        }

        try {
            Log.d("MapKitLifecycleManager", "Stopping MapKit")
            MapKitFactory.getInstance().onStop()
            isStarted = false
            Log.d("MapKitLifecycleManager", "MapKit stopped successfully")
        } catch (e: Exception) {
            Log.e("MapKitLifecycleManager", "Error stopping MapKit: ${e.message}", e)
        }
    }

    /**
     * Forces a restart of MapKit if it's currently initialized.
     * Useful for permission changes or configuration updates.
     */
    fun restart() {
        if (!isInitialized) {
            Log.w("MapKitLifecycleManager", "MapKit not initialized, cannot restart")
            return
        }

        Log.d("MapKitLifecycleManager", "Restarting MapKit")
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
        Log.d("MapKitLifecycleManager", "Lifecycle onStart")
        start()
    }

    /**
     * Lifecycle observer method - called when the lifecycle owner stops.
     */
    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        Log.d("MapKitLifecycleManager", "Lifecycle onStop")
        stop()
    }

    /**
     * Lifecycle observer method - called when the lifecycle owner is destroyed.
     */
    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        Log.d("MapKitLifecycleManager", "Lifecycle onDestroy")
        cleanup()
    }

    /**
     * Attaches this manager to a lifecycle owner for automatic lifecycle management.
     *
     * @param lifecycleOwner The lifecycle owner (typically an Activity or Fragment)
     */
    fun attachToLifecycle(lifecycleOwner: LifecycleOwner) {
        Log.d("MapKitLifecycleManager", "Attaching to lifecycle: ${lifecycleOwner.javaClass.simpleName}")
        lifecycleOwner.lifecycle.addObserver(this)
    }

    /**
     * Detaches this manager from a lifecycle owner.
     *
     * @param lifecycleOwner The lifecycle owner to detach from
     */
    fun detachFromLifecycle(lifecycleOwner: LifecycleOwner) {
        Log.d("MapKitLifecycleManager", "Detaching from lifecycle: ${lifecycleOwner.javaClass.simpleName}")
        lifecycleOwner.lifecycle.removeObserver(this)
    }

    /**
     * Cleans up resources and references.
     */
    private fun cleanup() {
        Log.d("MapKitLifecycleManager", "Cleaning up MapKit lifecycle manager")
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
            Log.e("MapKitLifecycleManager", "Safe initialization failed: ${e.message}", e)
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
            Log.e("MapKitLifecycleManager", "Safe start failed: ${e.message}", e)
            false
        }
    }

    /**
     * Gets the API key used for MapKit (for debugging purposes).
     * Note: In production, this should be handled more securely.
     *
     * @return The MapKit API key
     */
    fun getApiKey(): String = API_KEY

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
            Log.w("MapKitLifecycleManager", "MapKit factory not available: ${e.message}")
            false
        }
    }
}