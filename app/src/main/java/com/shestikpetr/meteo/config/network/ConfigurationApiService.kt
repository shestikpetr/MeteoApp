package com.shestikpetr.meteo.config.network

import com.shestikpetr.meteo.config.data.*
import retrofit2.http.GET
import retrofit2.http.Headers

/**
 * Retrofit API service for configuration endpoints.
 * Provides access to dynamic configuration from the remote server.
 */
interface ConfigurationApiService {

    /**
     * Gets validation configuration from the server
     */
    @GET("config/validation")
    @Headers("Accept: application/json")
    suspend fun getValidationConfig(): ConfigurationApiResponse<ValidationConfigResponse>

    /**
     * Gets retry policy configuration from the server
     */
    @GET("config/retry-policy")
    @Headers("Accept: application/json")
    suspend fun getRetryConfig(): ConfigurationApiResponse<RetryConfigResponse>

    /**
     * Gets theme configuration from the server
     */
    @GET("config/themes")
    @Headers("Accept: application/json")
    suspend fun getThemeConfig(): ConfigurationApiResponse<ThemeConfigResponse>

    /**
     * Gets demo/development configuration from the server
     */
    @GET("config/demo")
    @Headers("Accept: application/json")
    suspend fun getDemoConfig(): ConfigurationApiResponse<DemoConfigResponse>

    /**
     * Gets all configuration in a single request (for efficiency)
     */
    @GET("config/all")
    @Headers("Accept: application/json")
    suspend fun getAllConfig(): ConfigurationApiResponse<AllConfigResponse>
}

/**
 * Response model for the all-config endpoint
 */
data class AllConfigResponse(
    val validation: ValidationConfigResponse,
    val retry: RetryConfigResponse,
    val theme: ThemeConfigResponse,
    val demo: DemoConfigResponse
)