package com.shestikpetr.meteo.localization.repository

import com.shestikpetr.meteo.localization.interfaces.LocalizationRepository
import com.shestikpetr.meteo.localization.interfaces.NetworkStringLoader
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository implementation for network-based string loading
 * Follows Dependency Inversion Principle - depends on NetworkStringLoader abstraction
 */
@Singleton
class NetworkStringRepository @Inject constructor(
    private val networkLoader: NetworkStringLoader
) : LocalizationRepository {

    override suspend fun loadStrings(locale: String): Result<Map<String, String>> {
        return try {
            networkLoader.loadStringsFromNetwork(locale)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getSupportedLocales(): Result<List<String>> {
        return try {
            networkLoader.loadSupportedLocales()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun isLocaleSupported(locale: String): Boolean {
        return getSupportedLocales().getOrNull()?.contains(locale) ?: false
    }
}