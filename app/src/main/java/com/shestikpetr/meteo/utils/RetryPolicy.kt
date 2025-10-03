package com.shestikpetr.meteo.utils

import com.shestikpetr.meteo.common.constants.MeteoConstants
import com.shestikpetr.meteo.common.logging.MeteoLogger
import com.shestikpetr.meteo.config.interfaces.RetryConfigRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow

/**
 * Configurable retry policy for network operations with different strategies and error handling.
 *
 * This class provides retry mechanisms with exponential backoff, specific error handling,
 * and customizable retry conditions based on the original retry logic from NetworkMeteoRepository.
 */
@Singleton
class RetryPolicy @Inject constructor(
    private val retryConfigRepository: RetryConfigRepository
) {

    private val logger = MeteoLogger.forClass(RetryPolicy::class)

    /**
     * Configuration for retry behavior.
     */
    data class RetryConfig(
        val maxAttempts: Int = 3,
        val baseDelayMs: Long = 1000L,
        val maxDelayMs: Long = 5000L,
        val useExponentialBackoff: Boolean = false,
        val retryOnHttpErrors: Boolean = true,
        val retryOnNetworkErrors: Boolean = true,
        val retryOnJsonErrors: Boolean = true,
        val backoffMultiplier: Double = 2.0
    )

    /**
     * Result wrapper for retry operations.
     */
    sealed class RetryResult<T> {
        data class Success<T>(val data: T) : RetryResult<T>()
        data class Failure<T>(val exception: Exception, val attemptsMade: Int) : RetryResult<T>()
    }

    /**
     * Executes an operation with retry logic based on the provided configuration.
     *
     * @param config The retry configuration
     * @param operation The suspending operation to execute
     * @return RetryResult containing either success data or failure information
     */
    suspend fun <T> executeWithRetry(
        config: RetryConfig = RetryConfig(),
        operation: suspend (attempt: Int) -> T
    ): RetryResult<T> {
        var lastException: Exception? = null

        repeat(config.maxAttempts) { attempt ->
            try {
                logger.d("Attempt ${attempt + 1}/${config.maxAttempts}")
                val result = operation(attempt)
                logger.d("Operation succeeded on attempt ${attempt + 1}")
                return RetryResult.Success(result)
            } catch (e: HttpException) {
                lastException = e
                logger.w("HTTP error ${e.code()} on attempt ${attempt + 1}: ${e.message()}")

                if (!config.retryOnHttpErrors) {
                    logger.d("HTTP errors disabled for retry, failing immediately")
                    return RetryResult.Failure(e, attempt + 1)
                }

                logHttpErrorDetails(e)
            } catch (e: com.google.gson.JsonSyntaxException) {
                lastException = e
                logger.w("JSON parsing error on attempt ${attempt + 1}: ${e.message}")

                if (!config.retryOnJsonErrors) {
                    logger.d("JSON errors disabled for retry, failing immediately")
                    return RetryResult.Failure(e, attempt + 1)
                }
            } catch (e: java.net.ProtocolException) {
                lastException = e
                logger.w("Protocol error on attempt ${attempt + 1}: ${e.message}")

                if (!config.retryOnNetworkErrors) {
                    logger.d("Network errors disabled for retry, failing immediately")
                    return RetryResult.Failure(e, attempt + 1)
                }
            } catch (e: Exception) {
                lastException = e
                logger.w("General error on attempt ${attempt + 1}: ${e.message}")

                // For other exceptions, only retry if network errors are enabled
                if (!config.retryOnNetworkErrors) {
                    logger.d("General errors disabled for retry, failing immediately")
                    return RetryResult.Failure(e, attempt + 1)
                }
            }

            // Don't wait after the last attempt
            if (attempt < config.maxAttempts - 1) {
                val delayMs = calculateDelay(attempt, config)
                logger.d("Waiting ${delayMs}ms before next attempt")
                delay(delayMs)
            }
        }

        val finalException = lastException ?: Exception("All retry attempts failed")
        logger.e("All ${config.maxAttempts} attempts failed", finalException)
        return RetryResult.Failure(finalException, config.maxAttempts)
    }

    /**
     * Convenience method for executing with default retry configuration.
     */
    suspend fun <T> executeWithDefaultRetry(operation: suspend (attempt: Int) -> T): RetryResult<T> {
        return executeWithRetry(RetryConfig(), operation)
    }

    /**
     * Executes an operation that should return a fallback value on failure.
     * Based on the original getLatestSensorData logic that returns -1000.0 on failure.
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
            is RetryResult.Success -> result.data
            is RetryResult.Failure -> {
                logger.w("Using fallback value after ${result.attemptsMade} failed attempts")
                fallbackValue
            }
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
     * Logs detailed information about HTTP errors for debugging.
     */
    private fun logHttpErrorDetails(e: HttpException) {
        try {
            val errorBody = e.response()?.errorBody()?.string()
            logger.w("HTTP error body: $errorBody")
        } catch (bodyException: Exception) {
            logger.w("Could not read HTTP error body: ${bodyException.message}")
        }
    }

    /**
     * Checks if an exception should trigger a retry based on its type.
     */
    fun shouldRetry(exception: Exception, config: RetryConfig): Boolean {
        return when (exception) {
            is HttpException -> config.retryOnHttpErrors
            is com.google.gson.JsonSyntaxException -> config.retryOnJsonErrors
            is java.net.ProtocolException -> config.retryOnNetworkErrors
            else -> config.retryOnNetworkErrors
        }
    }

    /**
     * Creates a retry configuration optimized for sensor data operations.
     * Now uses dynamic configuration instead of hardcoded values.
     */
    suspend fun createSensorDataRetryConfig(): RetryConfig {
        return retryConfigRepository.getSensorDataRetryConfig().fold(
            onSuccess = { config ->
                RetryConfig(
                    maxAttempts = config.maxAttempts,
                    baseDelayMs = config.baseDelayMs,
                    maxDelayMs = config.maxDelayMs,
                    useExponentialBackoff = config.useExponentialBackoff,
                    retryOnHttpErrors = config.retryOnHttpErrors,
                    retryOnNetworkErrors = config.retryOnNetworkErrors,
                    retryOnJsonErrors = config.retryOnJsonErrors,
                    backoffMultiplier = config.backoffMultiplier
                )
            },
            onFailure = {
                logger.w("Failed to get sensor data retry config, using defaults", it)
                getDefaultSensorDataRetryConfig()
            }
        )
    }

    /**
     * Creates a retry configuration optimized for station data operations.
     * Now uses dynamic configuration instead of hardcoded values.
     */
    suspend fun createStationDataRetryConfig(): RetryConfig {
        return retryConfigRepository.getStationDataRetryConfig().fold(
            onSuccess = { config ->
                RetryConfig(
                    maxAttempts = config.maxAttempts,
                    baseDelayMs = config.baseDelayMs,
                    maxDelayMs = config.maxDelayMs,
                    useExponentialBackoff = config.useExponentialBackoff,
                    retryOnHttpErrors = config.retryOnHttpErrors,
                    retryOnNetworkErrors = config.retryOnNetworkErrors,
                    retryOnJsonErrors = config.retryOnJsonErrors,
                    backoffMultiplier = config.backoffMultiplier
                )
            },
            onFailure = {
                logger.w("Failed to get station data retry config, using defaults", it)
                getDefaultStationDataRetryConfig()
            }
        )
    }

    /**
     * Synchronous versions of config creation for backward compatibility.
     * Uses runBlocking internally - prefer the suspend versions when possible.
     */
    fun createSensorDataRetryConfigSync(): RetryConfig {
        return runBlocking { createSensorDataRetryConfig() }
    }

    fun createStationDataRetryConfigSync(): RetryConfig {
        return runBlocking { createStationDataRetryConfig() }
    }

    /**
     * Default fallback configurations
     */
    private fun getDefaultSensorDataRetryConfig(): RetryConfig {
        return RetryConfig(
            maxAttempts = 3, // Original hardcoded value
            baseDelayMs = 1000L, // Original hardcoded value
            maxDelayMs = 3000L,
            useExponentialBackoff = false,
            retryOnHttpErrors = true,
            retryOnNetworkErrors = true,
            retryOnJsonErrors = true,
            backoffMultiplier = 2.0
        )
    }

    private fun getDefaultStationDataRetryConfig(): RetryConfig {
        return RetryConfig(
            maxAttempts = 2,
            baseDelayMs = 500L,
            maxDelayMs = 2000L,
            useExponentialBackoff = true,
            retryOnHttpErrors = true,
            retryOnNetworkErrors = true,
            retryOnJsonErrors = false, // Station data parsing should be more strict
            backoffMultiplier = 2.0
        )
    }
}