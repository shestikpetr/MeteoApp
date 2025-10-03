package com.shestikpetr.meteo.localization.network

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Retrofit service for localization API endpoints
 * Follows API v1 structure as defined in the project
 */
interface LocalizationApiService {

    /**
     * Get localized strings for a specific locale
     * Endpoint: GET /api/v1/localization/{locale}
     */
    @GET("localization/{locale}")
    suspend fun getLocalizationStrings(
        @Path("locale") locale: String
    ): Response<LocalizationResponse>

    /**
     * Get list of supported locales
     * Endpoint: GET /api/v1/localization/supported
     */
    @GET("localization/supported")
    suspend fun getSupportedLocales(): Response<SupportedLocalesResponse>
}

/**
 * Response wrapper following API v1 format
 */
data class LocalizationResponse(
    val success: Boolean,
    val data: Map<String, String>,
    val message: String? = null
)

/**
 * Response for supported locales
 */
data class SupportedLocalesResponse(
    val success: Boolean,
    val data: List<String>,
    val message: String? = null
)