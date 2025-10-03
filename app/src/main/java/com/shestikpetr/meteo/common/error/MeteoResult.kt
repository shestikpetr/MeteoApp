package com.shestikpetr.meteo.common.error

/**
 * Unified result wrapper for all operations in the Meteo application.
 * Provides consistent error handling and success states across all layers.
 *
 * Based on Kotlin's Result class but with domain-specific error types
 * and better integration with the application's error handling strategy.
 */
sealed class MeteoResult<T> {
    /**
     * Success state containing the operation result.
     */
    data class Success<T>(val data: T) : MeteoResult<T>()

    /**
     * Error state containing the error information.
     */
    data class Error<T>(val error: MeteoError) : MeteoResult<T>()

    /**
     * Loading state for operations that require UI feedback.
     */
    class Loading<T> : MeteoResult<T>()

    /**
     * Returns true if this result represents a success.
     */
    val isSuccess: Boolean get() = this is Success

    /**
     * Returns true if this result represents an error.
     */
    val isError: Boolean get() = this is Error

    /**
     * Returns true if this result represents a loading state.
     */
    val isLoading: Boolean get() = this is Loading

    /**
     * Returns the data if this is a success, null otherwise.
     */
    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }

    /**
     * Returns the error if this is an error, null otherwise.
     */
    fun getErrorOrNull(): MeteoError? = when (this) {
        is Error -> error
        else -> null
    }

    /**
     * Returns the data if this is a success, or the result of [defaultValue] function if not.
     */
    inline fun getOrElse(defaultValue: (error: MeteoError?) -> T): T = when (this) {
        is Success -> data
        is Error -> defaultValue(error)
        is Loading -> defaultValue(null)
    }

    /**
     * Transforms the success data using the provided function.
     */
    inline fun <R> map(transform: (T) -> R): MeteoResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> Error(error)
        is Loading -> Loading()
    }

    /**
     * Transforms the success data using the provided function that returns a MeteoResult.
     */
    inline fun <R> flatMap(transform: (T) -> MeteoResult<R>): MeteoResult<R> = when (this) {
        is Success -> transform(data)
        is Error -> Error(error)
        is Loading -> Loading()
    }

    /**
     * Transforms the error using the provided function.
     */
    inline fun mapError(transform: (MeteoError) -> MeteoError): MeteoResult<T> = when (this) {
        is Success -> this
        is Error -> Error(transform(error))
        is Loading -> this
    }

    /**
     * Executes the provided function on success.
     */
    inline fun onSuccess(action: (T) -> Unit): MeteoResult<T> {
        if (this is Success) action(data)
        return this
    }

    /**
     * Executes the provided function on error.
     */
    inline fun onError(action: (MeteoError) -> Unit): MeteoResult<T> {
        if (this is Error) action(error)
        return this
    }

    /**
     * Executes the provided function on loading.
     */
    inline fun onLoading(action: () -> Unit): MeteoResult<T> {
        if (this is Loading) action()
        return this
    }

    companion object {
        /**
         * Creates a success result.
         */
        fun <T> success(data: T): MeteoResult<T> = Success(data)

        /**
         * Creates an error result from a MeteoError.
         */
        fun <T> error(error: MeteoError): MeteoResult<T> = Error(error)

        /**
         * Creates an error result from an exception.
         */
        fun <T> error(exception: Throwable, code: String = "UNKNOWN_ERROR"): MeteoResult<T> =
            Error(MeteoError.fromException(exception, code))

        /**
         * Creates an error result with a message.
         */
        fun <T> error(message: String, code: String = "UNKNOWN_ERROR"): MeteoResult<T> =
            Error(MeteoError.Generic(code, message))

        /**
         * Creates a loading result.
         */
        fun <T> loading(): MeteoResult<T> = Loading()

        /**
         * Wraps a suspending function that can throw exceptions.
         */
        suspend inline fun <T> catching(action: suspend () -> T): MeteoResult<T> {
            return try {
                success(action())
            } catch (e: Exception) {
                error(e)
            }
        }

        /**
         * Wraps a regular function that can throw exceptions.
         */
        inline fun <T> catchingSync(action: () -> T): MeteoResult<T> {
            return try {
                success(action())
            } catch (e: Exception) {
                error(e)
            }
        }
    }
}

/**
 * Extension function to convert Kotlin Result to MeteoResult.
 */
fun <T> Result<T>.toMeteoResult(): MeteoResult<T> {
    return fold(
        onSuccess = { MeteoResult.success(it) },
        onFailure = { MeteoResult.error(it) }
    )
}

/**
 * Extension function to convert MeteoResult to Kotlin Result.
 */
fun <T> MeteoResult<T>.toResult(): Result<T> {
    return when (this) {
        is MeteoResult.Success -> Result.success(data)
        is MeteoResult.Error -> Result.failure(error.toException())
        is MeteoResult.Loading -> Result.failure(IllegalStateException("Result is in loading state"))
    }
}