package com.shestikpetr.meteo.network.interfaces

import okhttp3.Request
import okhttp3.Response

/**
 * Abstract HTTP client interface to abstract away direct OkHttp dependency.
 * This interface follows the Dependency Inversion Principle by providing
 * an abstraction over HTTP operations, making the code testable and allowing
 * for different HTTP client implementations.
 */
interface HttpClient {

    /**
     * Executes an HTTP request synchronously.
     *
     * @param request The HTTP request to execute
     * @return The HTTP response
     * @throws Exception if the request fails
     */
    @Throws(Exception::class)
    fun executeRequest(request: Request): Response

    /**
     * Executes an HTTP request asynchronously.
     *
     * @param request The HTTP request to execute
     * @param callback Callback to handle the response or failure
     */
    fun executeRequestAsync(request: Request, callback: HttpCallback)

    /**
     * Creates a new HTTP request builder.
     *
     * @return A new request builder instance
     */
    fun newRequestBuilder(): Request.Builder

    /**
     * Sets the connection timeout for requests.
     *
     * @param timeoutSeconds Timeout in seconds
     */
    fun setConnectionTimeout(timeoutSeconds: Long)

    /**
     * Sets the read timeout for requests.
     *
     * @param timeoutSeconds Timeout in seconds
     */
    fun setReadTimeout(timeoutSeconds: Long)
}

/**
 * Callback interface for asynchronous HTTP operations.
 */
interface HttpCallback {
    /**
     * Called when the HTTP request completes successfully.
     *
     * @param response The HTTP response
     */
    fun onSuccess(response: Response)

    /**
     * Called when the HTTP request fails.
     *
     * @param exception The exception that caused the failure
     */
    fun onFailure(exception: Exception)
}