package com.shestikpetr.meteo.localization.network

import com.shestikpetr.meteo.localization.interfaces.NetworkStringLoader
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Network implementation for loading localization strings
 * Uses Retrofit to communicate with the localization API
 */
@Singleton
class NetworkStringLoaderImpl @Inject constructor(
    private val apiService: LocalizationApiService
) : NetworkStringLoader {

    override suspend fun loadStringsFromNetwork(locale: String): Result<Map<String, String>> {
        return try {
            val response = apiService.getLocalizationStrings(locale)

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.success) {
                    Result.success(body.data)
                } else {
                    Result.failure(
                        Exception("API returned unsuccessful response: ${body?.message}")
                    )
                }
            } else {
                Result.failure(
                    HttpException(response)
                )
            }
        } catch (e: IOException) {
            Result.failure(
                Exception("Network error: ${e.message}", e)
            )
        } catch (e: HttpException) {
            Result.failure(
                Exception("HTTP error ${e.code()}: ${e.message()}", e)
            )
        } catch (e: Exception) {
            Result.failure(
                Exception("Unexpected error: ${e.message}", e)
            )
        }
    }

    override suspend fun loadSupportedLocales(): Result<List<String>> {
        return try {
            val response = apiService.getSupportedLocales()

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.success) {
                    Result.success(body.data)
                } else {
                    Result.failure(
                        Exception("API returned unsuccessful response: ${body?.message}")
                    )
                }
            } else {
                Result.failure(
                    HttpException(response)
                )
            }
        } catch (e: IOException) {
            Result.failure(
                Exception("Network error: ${e.message}", e)
            )
        } catch (e: HttpException) {
            Result.failure(
                Exception("HTTP error ${e.code()}: ${e.message()}", e)
            )
        } catch (e: Exception) {
            Result.failure(
                Exception("Unexpected error: ${e.message}", e)
            )
        }
    }
}