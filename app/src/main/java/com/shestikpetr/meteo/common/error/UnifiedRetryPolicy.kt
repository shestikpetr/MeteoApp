package com.shestikpetr.meteo.common.error

import com.shestikpetr.meteo.common.logging.MeteoLogger
import com.shestikpetr.meteo.config.interfaces.RetryConfigRepository
import kotlinx.coroutines.delay
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow

/**
 * Unified retry policy that integrates with MeteoResult and MeteoError.
 * Replaces the legacy RetryPolicy with consistent error handling.
 *
 * Features:
 * - Integration with MeteoResult for consistent return types
 * - Structured error classification using MeteoError
 * - Configurable retry strategies
 * - Comprehensive logging using MeteoLogger
 * - Fallback mechanisms for critical operations
 */
@Singleton
class UnifiedRetryPolicy @Inject constructor(
    private val retryConfigRepository: RetryConfigRepository
) {
    private val logger = MeteoLogger.forClass(UnifiedRetryPolicy::class)

    /**
     * Configuration for retry behavior.
     */
    data class RetryConfig(
        val maxAttempts: Int = 3,
        val baseDelayMs: Long = 1000L,
        val maxDelayMs: Long = 5000L,
        val useExponentialBackoff: Boolean = false,
        val retryOnHttpErrors: Set<Int> = setOf(500, 502, 503, 504), // Server errors
        val retryOnNetworkErrors: Boolean = true,
        val retryOnParseErrors: Boolean = false,
        val backoffMultiplier: Double = 2.0
    )

    /**
     * Executes an operation with retry logic.
     *
     * @param config The retry configuration
     * @param operation The suspending operation to execute
     * @return MeteoResult containing either success data or error information
     */
    suspend fun <T> executeWithRetry(
        config: RetryConfig = RetryConfig(),
        operation: suspend (attempt: Int) -> T
    ): MeteoResult<T> {
        var lastError: MeteoError? = null

        repeat(config.maxAttempts) { attempt ->
            logger.startOperation("retry_operation", "attempt" to "${attempt + 1}/${config.maxAttempts}")

            try {
                val result = operation(attempt)
                logger.completeOperation("retry_operation", "success_on_attempt_${attempt + 1}")
                return MeteoResult.success(result)

            } catch (exception: Exception) {
                val error = MeteoError.fromException(exception)
                lastError = error

                logger.warn(error, "retry_attempt_${attempt + 1}")

                // Check if we should retry this type of error
                if (!shouldRetry(error, config)) {
                    logger.failOperation("retry_operation", error)
                    return MeteoResult.error(error)
                }

                // Don't wait after the last attempt
                if (attempt < config.maxAttempts - 1) {
                    val delayMs = calculateDelay(attempt, config)
                    logger.d { "Waiting ${delayMs}ms before retry attempt ${attempt + 2}" }
                    delay(delayMs)
                }
            }
        }

        val finalError = lastError ?: MeteoError.generic("RETRY_EXHAUSTED", "All retry attempts failed")
        logger.failOperation("retry_operation", finalError)
        return MeteoResult.error(finalError)
    }

    /**
     * Convenience method for executing with default retry configuration.
     */
    suspend fun <T> executeWithDefaultRetry(operation: suspend (attempt: Int) -> T): MeteoResult<T> {
        return executeWithRetry(RetryConfig(), operation)
    }

    /**
     * Executes an operation that should return a fallback value on failure.
     *
     * @param fallbackValue The value to return if all retries fail
     * @param config The retry configuration
     * @param operation The operation to execute
     * @return The operation result or fallback value
     */
    suspend fun <T> executeWithFallback(
        fallbackValue: T,
        config: RetryConfig = RetryConfig(),
        operation: suspend (attempt: Int) -> T
    ): T {
        return when (val result = executeWithRetry(config, operation)) {
            is MeteoResult.Success -> result.data
            is MeteoResult.Error -> {
                logger.warn(result.error, "using_fallback_value")
                fallbackValue
            }
            is MeteoResult.Loading -> fallbackValue // Should not happen but handle gracefully
        }
    }

    /**
     * Executes an operation and returns a MeteoResult, converting exceptions to errors.
     */
    suspend fun <T> executeWithErrorHandling(operation: suspend () -> T): MeteoResult<T> {
        return try {
            MeteoResult.success(operation())
        } catch (exception: Exception) {
            val error = MeteoError.fromException(exception)
            logger.error(error, "operation_failed")
            MeteoResult.error(error)
        }
    }

    /**
     * Determines if an error should trigger a retry.
     */
    private fun shouldRetry(error: MeteoError, config: RetryConfig): Boolean {
        return when (error) {
            is MeteoError.Network.HttpError -> {
                config.retryOnHttpErrors.contains(error.httpCode)
            }
            is MeteoError.Network.NoConnection,
            is MeteoError.Network.Timeout,
            is MeteoError.Network.ProtocolError -> {
                config.retryOnNetworkErrors
            }
            is MeteoError.Data.ParseError -> {
                config.retryOnParseErrors
            }
            is MeteoError.Auth.TokenExpired -> {
                false // Token refresh should be handled separately
            }
            is MeteoError.Auth -> {
                false // Auth errors should not be retried
            }
            else -> false // Conservative approach for unknown errors
        }
    }

    /**
     * Calculates the delay before the next retry attempt.
     */
    private fun calculateDelay(attempt: Int, config: RetryConfig): Long {
        return if (config.useExponentialBackoff) {
            val exponentialDelay = config.baseDelayMs * config.backoffMultiplier.pow(attempt.toDouble()).toLong()
            minOf(exponentialDelay, config.maxDelayMs)
        } else {
            config.baseDelayMs
        }
    }

    /**
     * Creates a retry configuration optimized for sensor data operations.
     */
    suspend fun createSensorDataRetryConfig(): RetryConfig {
        return retryConfigRepository.getSensorDataRetryConfig().fold(
            onSuccess = { config ->
                RetryConfig(
                    maxAttempts = config.maxAttempts,
                    baseDelayMs = config.baseDelayMs,
                    maxDelayMs = config.maxDelayMs,
                    useExponentialBackoff = config.useExponentialBackoff,
                    retryOnHttpErrors = if (config.retryOnHttpErrors) setOf(500, 502, 503, 504) else emptySet(),
                    retryOnNetworkErrors = config.retryOnNetworkErrors,
                    retryOnParseErrors = config.retryOnJsonErrors,
                    backoffMultiplier = config.backoffMultiplier
                )
            },
            onFailure = {
                logger.warn(MeteoError.fromException(it), "failed_to_load_sensor_retry_config")
                getDefaultSensorDataRetryConfig()
            }
        )
    }

    /**
     * Creates a retry configuration optimized for station data operations.
     */
    suspend fun createStationDataRetryConfig(): RetryConfig {
        return retryConfigRepository.getStationDataRetryConfig().fold(
            onSuccess = { config ->
                RetryConfig(
                    maxAttempts = config.maxAttempts,
                    baseDelayMs = config.baseDelayMs,
                    maxDelayMs = config.maxDelayMs,
                    useExponentialBackoff = config.useExponentialBackoff,
                    retryOnHttpErrors = if (config.retryOnHttpErrors) setOf(500, 502, 503, 504) else emptySet(),
                    retryOnNetworkErrors = config.retryOnNetworkErrors,
                    retryOnParseErrors = false, // Station data should be more strict
                    backoffMultiplier = config.backoffMultiplier
                )
            },
            onFailure = {
                logger.warn(MeteoError.fromException(it), "failed_to_load_station_retry_config")
                getDefaultStationDataRetryConfig()
            }
        )
    }

    /**
     * Creates a retry configuration optimized for authentication operations.
     */
    fun createAuthRetryConfig(): RetryConfig {
        return RetryConfig(
            maxAttempts = 2,
            baseDelayMs = 500L,
            maxDelayMs = 1000L,
            useExponentialBackoff = false,
            retryOnHttpErrors = setOf(500, 502, 503, 504), // Only server errors
            retryOnNetworkErrors = true,
            retryOnParseErrors = false,
            backoffMultiplier = 1.5
        )
    }

    /**
     * Default configuration for sensor data operations.
     */
    private fun getDefaultSensorDataRetryConfig(): RetryConfig {
        return RetryConfig(
            maxAttempts = 3,
            baseDelayMs = 1000L,
            maxDelayMs = 3000L,
            useExponentialBackoff = false,
            retryOnHttpErrors = setOf(500, 502, 503, 504),
            retryOnNetworkErrors = true,
            retryOnParseErrors = true,
            backoffMultiplier = 2.0
        )
    }

    /**
     * Default configuration for station data operations.
     */
    private fun getDefaultStationDataRetryConfig(): RetryConfig {
        return RetryConfig(
            maxAttempts = 2,
            baseDelayMs = 500L,
            maxDelayMs = 2000L,
            useExponentialBackoff = true,
            retryOnHttpErrors = setOf(500, 502, 503, 504),
            retryOnNetworkErrors = true,
            retryOnParseErrors = false,
            backoffMultiplier = 2.0
        )
    }
}

/**
 * Extension function to wrap HTTP calls with unified error handling.
 */
suspend fun <T> retrofitCall(call: suspend () -> T): MeteoResult<T> {
    return try {
        MeteoResult.success(call())
    } catch (exception: HttpException) {
        val error = when (exception.code()) {
            401 -> MeteoError.Auth.Unauthorized(exception)
            403 -> MeteoError.Auth.Forbidden(exception)
            404 -> MeteoError.Data.NotFound("resource", cause = exception)
            422 -> MeteoError.Data.ValidationError("request", "Invalid request data", exception)
            in 500..599 -> MeteoError.Network.HttpError(exception.code(), exception.message(), exception)
            else -> MeteoError.Network.HttpError(exception.code(), exception.message(), exception)
        }
        MeteoResult.error(error)
    } catch (exception: Exception) {
        MeteoResult.error(MeteoError.fromException(exception))
    }
}