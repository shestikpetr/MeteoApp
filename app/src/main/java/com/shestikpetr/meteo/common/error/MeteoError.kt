package com.shestikpetr.meteo.common.error

import retrofit2.HttpException
import java.net.ProtocolException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * Unified error types for the Meteo application.
 * Provides consistent error classification and handling across all layers.
 *
 * All errors include:
 * - code: A stable identifier for programmatic handling
 * - message: Human-readable error description
 * - cause: Optional underlying exception for debugging
 */
sealed class MeteoError(
    open val code: String,
    open val message: String,
    open val cause: Throwable? = null
) {

    /**
     * Network-related errors.
     */
    sealed class Network(
        code: String,
        message: String,
        cause: Throwable? = null
    ) : MeteoError(code, message, cause) {

        data class NoConnection(
            override val cause: Throwable? = null
        ) : Network("NETWORK_NO_CONNECTION", "No internet connection available", cause)

        data class Timeout(
            override val cause: Throwable? = null
        ) : Network("NETWORK_TIMEOUT", "Network request timed out", cause)

        data class HttpError(
            val httpCode: Int,
            override val message: String,
            override val cause: Throwable? = null
        ) : Network("NETWORK_HTTP_$httpCode", message, cause)

        data class SslError(
            override val cause: Throwable? = null
        ) : Network("NETWORK_SSL_ERROR", "SSL/TLS connection error", cause)

        data class ProtocolError(
            override val cause: Throwable? = null
        ) : Network("NETWORK_PROTOCOL_ERROR", "Network protocol error", cause)
    }

    /**
     * Authentication and authorization errors.
     */
    sealed class Auth(
        code: String,
        message: String,
        cause: Throwable? = null
    ) : MeteoError(code, message, cause) {

        data class InvalidCredentials(
            override val cause: Throwable? = null
        ) : Auth("AUTH_INVALID_CREDENTIALS", "Invalid username or password", cause)

        data class TokenExpired(
            override val cause: Throwable? = null
        ) : Auth("AUTH_TOKEN_EXPIRED", "Authentication token has expired", cause)

        data class TokenInvalid(
            override val cause: Throwable? = null
        ) : Auth("AUTH_TOKEN_INVALID", "Authentication token is invalid", cause)

        data class RefreshTokenExpired(
            override val cause: Throwable? = null
        ) : Auth("AUTH_REFRESH_TOKEN_EXPIRED", "Refresh token has expired", cause)

        data class Unauthorized(
            override val cause: Throwable? = null
        ) : Auth("AUTH_UNAUTHORIZED", "Access denied - authentication required", cause)

        data class Forbidden(
            override val cause: Throwable? = null
        ) : Auth("AUTH_FORBIDDEN", "Access denied - insufficient permissions", cause)

        data class UserNotFound(
            override val cause: Throwable? = null
        ) : Auth("AUTH_USER_NOT_FOUND", "User account not found", cause)

        data class UserAlreadyExists(
            override val cause: Throwable? = null
        ) : Auth("AUTH_USER_EXISTS", "User account already exists", cause)
    }

    /**
     * Data parsing and validation errors.
     */
    sealed class Data(
        code: String,
        message: String,
        cause: Throwable? = null
    ) : MeteoError(code, message, cause) {

        data class ParseError(
            override val cause: Throwable? = null
        ) : Data("DATA_PARSE_ERROR", "Failed to parse server response", cause)

        data class ValidationError(
            val field: String,
            override val message: String,
            override val cause: Throwable? = null
        ) : Data("DATA_VALIDATION_ERROR", message, cause)

        data class NotFound(
            val resourceType: String,
            val resourceId: String? = null,
            override val cause: Throwable? = null
        ) : Data(
            "DATA_NOT_FOUND",
            "Resource not found: $resourceType${resourceId?.let { " (ID: $it)" } ?: ""}",
            cause
        )

        data class Corrupted(
            override val cause: Throwable? = null
        ) : Data("DATA_CORRUPTED", "Data is corrupted or inconsistent", cause)
    }

    /**
     * Configuration and system errors.
     */
    sealed class System(
        code: String,
        message: String,
        cause: Throwable? = null
    ) : MeteoError(code, message, cause) {

        data class ConfigurationError(
            override val message: String,
            override val cause: Throwable? = null
        ) : System("SYSTEM_CONFIG_ERROR", message, cause)

        data class StorageError(
            override val cause: Throwable? = null
        ) : System("SYSTEM_STORAGE_ERROR", "Local storage operation failed", cause)

        data class LocationPermissionDenied(
            override val cause: Throwable? = null
        ) : System("SYSTEM_LOCATION_PERMISSION", "Location permission is required", cause)

        data class LocationUnavailable(
            override val cause: Throwable? = null
        ) : System("SYSTEM_LOCATION_UNAVAILABLE", "Current location is not available", cause)

        data class ServiceUnavailable(
            val serviceName: String,
            override val cause: Throwable? = null
        ) : System("SYSTEM_SERVICE_UNAVAILABLE", "Service unavailable: $serviceName", cause)
    }

    /**
     * Generic errors for cases not covered by specific types.
     */
    data class Generic(
        override val code: String,
        override val message: String,
        override val cause: Throwable? = null
    ) : MeteoError(code, message, cause)

    /**
     * Converts this error to an exception for compatibility with throw statements.
     */
    fun toException(): Exception {
        return cause as? Exception ?: MeteoException(this)
    }

    /**
     * Returns a user-friendly message suitable for displaying in UI.
     * Can be localized based on the error code.
     */
    fun getUserMessage(): String {
        return when (this) {
            is Network.NoConnection -> "Проверьте подключение к интернету"
            is Network.Timeout -> "Запрос слишком долго выполняется"
            is Network.HttpError -> when (httpCode) {
                500 -> "Ошибка сервера. Попробуйте позже"
                503 -> "Сервис временно недоступен"
                else -> "Ошибка сети ($httpCode)"
            }
            is Auth.InvalidCredentials -> "Неверное имя пользователя или пароль"
            is Auth.TokenExpired -> "Сессия истекла. Войдите в систему заново"
            is Auth.Unauthorized -> "Необходима авторизация"
            is Data.NotFound -> "Данные не найдены"
            is Data.ParseError -> "Ошибка обработки данных"
            is System.LocationPermissionDenied -> "Требуется разрешение на использование геолокации"
            else -> message
        }
    }

    companion object {
        /**
         * Creates a MeteoError from a generic exception.
         */
        fun fromException(exception: Throwable, defaultCode: String = "UNKNOWN_ERROR"): MeteoError {
            return when (exception) {
                is HttpException -> Network.HttpError(
                    httpCode = exception.code(),
                    message = "HTTP ${exception.code()}: ${exception.message()}",
                    cause = exception
                )
                is UnknownHostException -> Network.NoConnection(exception)
                is SocketTimeoutException -> Network.Timeout(exception)
                is SSLException -> Network.SslError(exception)
                is ProtocolException -> Network.ProtocolError(exception)
                is com.google.gson.JsonSyntaxException -> Data.ParseError(exception)
                is SecurityException -> when {
                    exception.message?.contains("authentication", ignoreCase = true) == true ||
                    exception.message?.contains("token", ignoreCase = true) == true ->
                        Auth.TokenInvalid(exception)
                    else -> System.ConfigurationError(
                        message = exception.message ?: "Security error",
                        cause = exception
                    )
                }
                is IllegalArgumentException -> Data.ValidationError(
                    field = "unknown",
                    message = exception.message ?: "Validation failed",
                    cause = exception
                )
                is MeteoException -> exception.error
                else -> Generic(
                    code = defaultCode,
                    message = exception.message ?: "Unknown error occurred",
                    cause = exception
                )
            }
        }

        /**
         * Creates a network error based on HTTP status code.
         */
        fun httpError(code: Int, message: String, cause: Throwable? = null): Network.HttpError {
            return Network.HttpError(code, message, cause)
        }

        /**
         * Creates an authentication error for invalid credentials.
         */
        fun invalidCredentials(cause: Throwable? = null): Auth.InvalidCredentials {
            return Auth.InvalidCredentials(cause)
        }

        /**
         * Creates a data not found error.
         */
        fun notFound(resourceType: String, resourceId: String? = null, cause: Throwable? = null): Data.NotFound {
            return Data.NotFound(resourceType, resourceId, cause)
        }

        /**
         * Creates a validation error.
         */
        fun validation(field: String, message: String, cause: Throwable? = null): Data.ValidationError {
            return Data.ValidationError(field, message, cause)
        }

        /**
         * Creates a generic error.
         */
        fun generic(code: String, message: String, cause: Throwable? = null): Generic {
            return Generic(code, message, cause)
        }
    }
}

/**
 * Exception wrapper for MeteoError to maintain compatibility with throw statements.
 */
class MeteoException(val error: MeteoError) : Exception(error.message, error.cause)