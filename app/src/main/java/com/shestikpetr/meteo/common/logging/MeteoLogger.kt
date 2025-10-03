package com.shestikpetr.meteo.common.logging

import android.util.Log
import com.shestikpetr.meteo.common.error.MeteoError
import kotlin.reflect.KClass

/**
 * Unified logging system for the Meteo application.
 * Provides consistent logging across all components with proper tagging,
 * error handling integration, and configurable log levels.
 *
 * Features:
 * - Automatic tag generation from class names
 * - Integration with MeteoError for structured error logging
 * - Configurable log levels
 * - Performance-aware logging (lazy message evaluation)
 * - Consistent formatting across the application
 */
object MeteoLogger {

    /**
     * Log levels for controlling verbosity.
     */
    enum class LogLevel(val priority: Int) {
        VERBOSE(Log.VERBOSE),
        DEBUG(Log.DEBUG),
        INFO(Log.INFO),
        WARN(Log.WARN),
        ERROR(Log.ERROR)
    }

    /**
     * Current minimum log level. Logs below this level will be ignored.
     * This can be controlled by build configuration or runtime settings.
     */
    private var minLogLevel: LogLevel = LogLevel.DEBUG

    /**
     * Maximum tag length to prevent Android log truncation.
     */
    private const val MAX_TAG_LENGTH = 23

    /**
     * Prefix for all Meteo application logs.
     */
    private const val TAG_PREFIX = "Meteo"

    /**
     * Sets the minimum log level.
     */
    fun setMinLogLevel(level: LogLevel) {
        minLogLevel = level
    }

    /**
     * Creates a logger instance for a specific class.
     */
    fun forClass(clazz: KClass<*>): Logger {
        return Logger(createTag(clazz.simpleName ?: "Unknown"))
    }

    /**
     * Creates a logger instance for a specific class using Java Class.
     */
    fun forClass(clazz: Class<*>): Logger {
        return Logger(createTag(clazz.simpleName))
    }

    /**
     * Creates a logger instance with a custom tag.
     */
    fun forTag(tag: String): Logger {
        return Logger(createTag(tag))
    }

    /**
     * Creates a properly formatted and length-limited tag.
     */
    private fun createTag(name: String): String {
        val fullTag = "$TAG_PREFIX.$name"
        return if (fullTag.length <= MAX_TAG_LENGTH) {
            fullTag
        } else {
            // Truncate but keep the prefix
            val availableLength = MAX_TAG_LENGTH - TAG_PREFIX.length - 1 // -1 for the dot
            "$TAG_PREFIX.${name.take(availableLength)}"
        }
    }

    /**
     * Checks if a log level should be output.
     */
    private fun shouldLog(level: LogLevel): Boolean {
        return level.priority >= minLogLevel.priority
    }

    /**
     * Logger instance for a specific component.
     */
    class Logger internal constructor(private val tag: String) {

        /**
         * Logs a verbose message.
         */
        fun v(message: () -> String) {
            if (shouldLog(LogLevel.VERBOSE)) {
                Log.v(tag, message())
            }
        }

        fun v(message: String, throwable: Throwable? = null) {
            if (shouldLog(LogLevel.VERBOSE)) {
                if (throwable != null) {
                    Log.v(tag, message, throwable)
                } else {
                    Log.v(tag, message)
                }
            }
        }

        /**
         * Logs a debug message.
         */
        fun d(message: () -> String) {
            if (shouldLog(LogLevel.DEBUG)) {
                Log.d(tag, message())
            }
        }

        fun d(message: String, throwable: Throwable? = null) {
            if (shouldLog(LogLevel.DEBUG)) {
                if (throwable != null) {
                    Log.d(tag, message, throwable)
                } else {
                    Log.d(tag, message)
                }
            }
        }

        /**
         * Logs an info message.
         */
        fun i(message: () -> String) {
            if (shouldLog(LogLevel.INFO)) {
                Log.i(tag, message())
            }
        }

        fun i(message: String, throwable: Throwable? = null) {
            if (shouldLog(LogLevel.INFO)) {
                if (throwable != null) {
                    Log.i(tag, message, throwable)
                } else {
                    Log.i(tag, message)
                }
            }
        }

        /**
         * Logs a warning message.
         */
        fun w(message: () -> String) {
            if (shouldLog(LogLevel.WARN)) {
                Log.w(tag, message())
            }
        }

        fun w(message: String, throwable: Throwable? = null) {
            if (shouldLog(LogLevel.WARN)) {
                if (throwable != null) {
                    Log.w(tag, message, throwable)
                } else {
                    Log.w(tag, message)
                }
            }
        }

        /**
         * Logs an error message.
         */
        fun e(message: () -> String) {
            if (shouldLog(LogLevel.ERROR)) {
                Log.e(tag, message())
            }
        }

        fun e(message: String, throwable: Throwable? = null) {
            if (shouldLog(LogLevel.ERROR)) {
                if (throwable != null) {
                    Log.e(tag, message, throwable)
                } else {
                    Log.e(tag, message)
                }
            }
        }

        /**
         * Logs a MeteoError with appropriate level and formatting.
         */
        fun error(error: MeteoError, context: String? = null) {
            if (shouldLog(LogLevel.ERROR)) {
                val contextPrefix = context?.let { "[$it] " } ?: ""
                val message = "${contextPrefix}${error.code}: ${error.message}"
                Log.e(tag, message, error.cause)
            }
        }

        /**
         * Logs a MeteoError as a warning (for recoverable errors).
         */
        fun warn(error: MeteoError, context: String? = null) {
            if (shouldLog(LogLevel.WARN)) {
                val contextPrefix = context?.let { "[$it] " } ?: ""
                val message = "${contextPrefix}${error.code}: ${error.message}"
                Log.w(tag, message, error.cause)
            }
        }

        /**
         * Logs the start of an operation (for debugging).
         */
        fun startOperation(operationName: String, vararg params: Pair<String, Any?>) {
            if (shouldLog(LogLevel.DEBUG)) {
                val paramString = if (params.isNotEmpty()) {
                    params.joinToString(", ") { "${it.first}=${it.second}" }
                } else {
                    "no params"
                }
                Log.d(tag, "Starting $operationName ($paramString)")
            }
        }

        /**
         * Logs the successful completion of an operation.
         */
        fun completeOperation(operationName: String, result: Any? = null) {
            if (shouldLog(LogLevel.DEBUG)) {
                val resultString = result?.let { " -> $it" } ?: ""
                Log.d(tag, "Completed $operationName$resultString")
            }
        }

        /**
         * Logs the failure of an operation with error details.
         */
        fun failOperation(operationName: String, error: MeteoError) {
            if (shouldLog(LogLevel.ERROR)) {
                Log.e(tag, "Failed $operationName: ${error.code} - ${error.message}", error.cause)
            }
        }

        /**
         * Logs network request details.
         */
        fun logNetworkRequest(method: String, url: String, params: Map<String, Any?> = emptyMap()) {
            if (shouldLog(LogLevel.DEBUG)) {
                val paramString = if (params.isNotEmpty()) {
                    " with params: ${params.entries.joinToString(", ") { "${it.key}=${it.value}" }}"
                } else {
                    ""
                }
                Log.d(tag, "Network $method request to $url$paramString")
            }
        }

        /**
         * Logs network response details.
         */
        fun logNetworkResponse(method: String, url: String, statusCode: Int, responseTimeMs: Long? = null) {
            if (shouldLog(LogLevel.DEBUG)) {
                val timeString = responseTimeMs?.let { " (${it}ms)" } ?: ""
                Log.d(tag, "Network $method response from $url: $statusCode$timeString")
            }
        }

        /**
         * Logs performance metrics.
         */
        fun logPerformance(operationName: String, durationMs: Long, additionalMetrics: Map<String, Any> = emptyMap()) {
            if (shouldLog(LogLevel.INFO)) {
                val metricsString = if (additionalMetrics.isNotEmpty()) {
                    ", " + additionalMetrics.entries.joinToString(", ") { "${it.key}=${it.value}" }
                } else {
                    ""
                }
                Log.i(tag, "Performance [$operationName]: ${durationMs}ms$metricsString")
            }
        }

        /**
         * Logs cache operations.
         */
        fun logCache(operation: String, key: String, hit: Boolean? = null) {
            if (shouldLog(LogLevel.DEBUG)) {
                val hitString = when (hit) {
                    true -> " (HIT)"
                    false -> " (MISS)"
                    null -> ""
                }
                Log.d(tag, "Cache $operation: $key$hitString")
            }
        }
    }
}

/**
 * Extension function to create a logger for any class.
 */
inline fun <reified T : Any> T.logger(): MeteoLogger.Logger {
    return MeteoLogger.forClass(T::class)
}

/**
 * Extension function to create a logger for any class using Java reflection.
 */
fun Any.loggerJava(): MeteoLogger.Logger {
    return MeteoLogger.forClass(this::class.java)
}