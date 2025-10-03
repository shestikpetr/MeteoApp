package com.shestikpetr.meteo.common.constants

/**
 * Centralized constants for the Meteo application.
 * All magic numbers and configuration values should be defined here
 * to ensure consistency and maintainability.
 */
object MeteoConstants {

    /**
     * Data-related constants.
     */
    object Data {
        /** Value returned when sensor data is unavailable */
        const val UNAVAILABLE_VALUE = -99.0

        /** Maximum age of cached data in milliseconds (5 minutes) */
        const val CACHE_MAX_AGE_MS = 5 * 60 * 1000L

        /** Default polling interval for real-time data in milliseconds (30 seconds) */
        const val DEFAULT_POLLING_INTERVAL_MS = 30 * 1000L

        /** Maximum number of data points to display on charts */
        const val MAX_CHART_DATA_POINTS = 1000

        /** Minimum interval between data points in milliseconds */
        const val MIN_DATA_INTERVAL_MS = 1000L
    }

    /**
     * Network-related constants.
     */
    object Network {
        /** Default connection timeout in seconds */
        const val DEFAULT_CONNECT_TIMEOUT_SECONDS = 30L

        /** Default read timeout in seconds */
        const val DEFAULT_READ_TIMEOUT_SECONDS = 30L

        /** Default write timeout in seconds */
        const val DEFAULT_WRITE_TIMEOUT_SECONDS = 30L

        /** Maximum retry attempts for failed requests */
        const val DEFAULT_MAX_RETRY_ATTEMPTS = 3

        /** Base delay between retry attempts in milliseconds */
        const val DEFAULT_RETRY_DELAY_MS = 1000L

        /** Maximum delay between retry attempts in milliseconds */
        const val DEFAULT_MAX_RETRY_DELAY_MS = 5000L

        /** Emergency timeout in seconds (shorter for emergency fallback) */
        const val EMERGENCY_TIMEOUT_SECONDS = 15L
    }

    /**
     * UI-related constants.
     */
    object UI {
        /** Default animation duration in milliseconds */
        const val DEFAULT_ANIMATION_DURATION_MS = 300

        /** Default fade animation duration in milliseconds */
        const val DEFAULT_FADE_DURATION_MS = 200

        /** Default scale animation duration in milliseconds */
        const val DEFAULT_SCALE_DURATION_MS = 150

        /** Default map zoom level */
        const val DEFAULT_MAP_ZOOM = 15.0f

        /** Minimum map zoom level */
        const val MIN_MAP_ZOOM = 1.0f

        /** Maximum map zoom level */
        const val MAX_MAP_ZOOM = 20.0f

        /** Minimum size for touch targets in dp */
        const val MIN_TOUCH_TARGET_SIZE_DP = 48

        /** Default padding in dp */
        const val DEFAULT_PADDING_DP = 16

        /** Small padding in dp */
        const val SMALL_PADDING_DP = 8

        /** Large padding in dp */
        const val LARGE_PADDING_DP = 24
    }

    /**
     * Map-related constants.
     */
    object Map {
        /** Default latitude for Tomsk, Russia */
        const val DEFAULT_LATITUDE = 56.460337

        /** Default longitude for Tomsk, Russia */
        const val DEFAULT_LONGITUDE = 84.961591

        /** Clustering distance threshold in pixels */
        const val CLUSTER_DISTANCE_THRESHOLD_PX = 100

        /** Minimum zoom level for clustering */
        const val CLUSTER_MIN_ZOOM_LEVEL = 10.0f

        /** Maximum number of stations to display without clustering */
        const val MAX_STATIONS_WITHOUT_CLUSTERING = 50

        /** Emergency map zoom level (lower for better performance) */
        const val EMERGENCY_MAP_ZOOM = 5.0f
    }

    /**
     * Authentication-related constants.
     */
    object Auth {
        /** JWT token expiration buffer in minutes */
        const val TOKEN_EXPIRATION_BUFFER_MINUTES = 5L

        /** Maximum login attempts before lockout */
        const val MAX_LOGIN_ATTEMPTS = 5

        /** Lockout duration in minutes */
        const val LOGIN_LOCKOUT_DURATION_MINUTES = 15L

        /** Session timeout in hours */
        const val SESSION_TIMEOUT_HOURS = 24L

        /** Emergency token expiration buffer in minutes (minimal for emergency) */
        const val EMERGENCY_TOKEN_BUFFER_MINUTES = 1L
    }

    /**
     * Storage-related constants.
     */
    object Storage {
        /** SharedPreferences file name */
        const val PREFS_NAME = "meteo_prefs"

        /** Maximum number of items to keep in local cache */
        const val MAX_CACHE_ITEMS = 1000

        /** Database version */
        const val DATABASE_VERSION = 1

        /** Database name */
        const val DATABASE_NAME = "meteo_database"
    }

    /**
     * Validation-related constants.
     */
    object Validation {
        /** Minimum username length */
        const val MIN_USERNAME_LENGTH = 3

        /** Maximum username length */
        const val MAX_USERNAME_LENGTH = 50

        /** Minimum password length */
        const val MIN_PASSWORD_LENGTH = 6

        /** Maximum password length */
        const val MAX_PASSWORD_LENGTH = 128

        /** Maximum station name length */
        const val MAX_STATION_NAME_LENGTH = 100

        /** Valid latitude range */
        const val MIN_LATITUDE = -90.0
        const val MAX_LATITUDE = 90.0

        /** Valid longitude range */
        const val MIN_LONGITUDE = -180.0
        const val MAX_LONGITUDE = 180.0
    }

    /**
     * Debug and logging constants.
     */
    object Debug {
        /** Enable debug logging */
        const val ENABLE_DEBUG_LOGGING = true

        /** Enable performance monitoring */
        const val ENABLE_PERFORMANCE_MONITORING = false

        /** Enable network request logging */
        const val ENABLE_NETWORK_LOGGING = true

        /** Debug tag prefix */
        const val DEBUG_TAG_PREFIX = "Meteo"
    }

    /**
     * Format-related constants.
     */
    object Format {
        /** Date format for API requests */
        const val API_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'"

        /** Display date format */
        const val DISPLAY_DATE_FORMAT = "dd.MM.yyyy HH:mm"

        /** Short date format */
        const val SHORT_DATE_FORMAT = "dd.MM.yyyy"

        /** Time format */
        const val TIME_FORMAT = "HH:mm"

        /** Number format for sensor values (decimal places) */
        const val SENSOR_VALUE_DECIMAL_PLACES = 2
    }

    /**
     * Error codes and messages.
     */
    object ErrorCodes {
        /** Generic unknown error */
        const val UNKNOWN_ERROR = "UNKNOWN_ERROR"

        /** Network connection error */
        const val NETWORK_ERROR = "NETWORK_ERROR"

        /** Authentication error */
        const val AUTH_ERROR = "AUTH_ERROR"

        /** Data validation error */
        const val VALIDATION_ERROR = "VALIDATION_ERROR"

        /** Permission denied error */
        const val PERMISSION_ERROR = "PERMISSION_ERROR"
    }
}