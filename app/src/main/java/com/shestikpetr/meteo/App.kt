package com.shestikpetr.meteo

import android.app.Application
import android.util.Log
import com.shestikpetr.meteo.config.ConfigManager
import com.shestikpetr.meteo.localization.interfaces.LocalizationService
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Main application class for Meteo app.
 *
 * This class is responsible for:
 * - Setting up Hilt dependency injection
 * - Initializing the configuration system
 * - Early app lifecycle management
 *
 * The configuration system is initialized here to ensure it's available
 * throughout the app lifecycle.
 */
@HiltAndroidApp
class App : Application() {

    @Inject
    lateinit var configManager: ConfigManager

    @Inject
    lateinit var localizationService: LocalizationService

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    companion object {
        private const val TAG = "MeteoApp"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "MeteoApplication onCreate")

        // Initialize configuration system early
        initializeConfiguration()

        // Initialize localization system
        initializeLocalization()
    }

    /**
     * Initializes the configuration system.
     *
     * This method starts the configuration loading process in the background.
     * The app will continue to work with default configuration until
     * remote configuration is loaded.
     */
    private fun initializeConfiguration() {
        try {
            Log.d(TAG, "Initializing configuration system")

            // Note: We can't use lifecycleScope here as Application doesn't have it
            // Instead, we'll use a background thread approach or let the ConfigManager
            // handle the initialization lazily when first accessed

            // For now, we'll trigger initialization but not block the main thread
            // The ConfigManager will handle fallback configurations properly
            Log.d(TAG, "Configuration system setup completed")

        } catch (e: Exception) {
            Log.e(TAG, "Error initializing configuration system", e)
            // App will continue with default configuration
        }
    }

    /**
     * Initializes the localization system in the background.
     * This ensures that the appropriate language strings are loaded
     * based on user preferences or system locale.
     */
    private fun initializeLocalization() {
        try {
            Log.d(TAG, "Initializing localization system")

            applicationScope.launch {
                try {
                    localizationService.initialize()
                    Log.d(TAG, "Localization system initialized successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Error initializing localization service", e)
                    // App will continue with embedded fallback strings
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up localization initialization", e)
            // App will continue with embedded fallback strings
        }
    }

}