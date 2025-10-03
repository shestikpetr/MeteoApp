package com.shestikpetr.meteo.localization.repository

import com.shestikpetr.meteo.localization.interfaces.LocalizationRepository
import com.shestikpetr.meteo.localization.interfaces.LocalizationCache
import com.shestikpetr.meteo.localization.interfaces.EmbeddedStringProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository with caching and fallback support
 * Follows Single Responsibility Principle - coordinates between cache, network, and embedded strings
 * Implements the strategy pattern for different data sources
 */
@Singleton
class CachedStringRepository @Inject constructor(
    private val networkRepository: LocalizationRepository,
    private val cache: LocalizationCache,
    private val embeddedProvider: EmbeddedStringProvider
) : LocalizationRepository {

    private companion object {
        const val CACHE_VALIDITY_HOURS = 24L
    }

    override suspend fun loadStrings(locale: String): Result<Map<String, String>> {
        return try {
            // 1. Try cache first
            val cachedStrings = getCachedStringsIfValid(locale)
            if (cachedStrings != null) {
                return Result.success(cachedStrings)
            }

            // 2. Try network
            val networkResult = networkRepository.loadStrings(locale)
            if (networkResult.isSuccess) {
                val strings = networkResult.getOrNull()!!
                cache.cacheStrings(locale, strings)
                return Result.success(strings)
            }

            // 3. Fallback to embedded strings
            val embeddedStrings = embeddedProvider.getEmbeddedStrings(locale)
            if (embeddedStrings.isNotEmpty()) {
                return Result.success(embeddedStrings)
            }

            // 4. If no embedded strings for locale, try default locale
            val defaultStrings = embeddedProvider.getEmbeddedStrings(embeddedProvider.getDefaultLocale())
            Result.success(defaultStrings)

        } catch (e: Exception) {
            // Final fallback to embedded strings
            try {
                val fallbackStrings = embeddedProvider.getEmbeddedStrings(
                    embeddedProvider.getDefaultLocale()
                )
                Result.success(fallbackStrings)
            } catch (fallbackError: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getSupportedLocales(): Result<List<String>> {
        return try {
            // Try network first
            val networkResult = networkRepository.getSupportedLocales()
            if (networkResult.isSuccess) {
                return networkResult
            }

            // Fallback to embedded locales
            val embeddedLocales = embeddedProvider.getSupportedLocales()
            Result.success(embeddedLocales)
        } catch (e: Exception) {
            // Final fallback
            val embeddedLocales = embeddedProvider.getSupportedLocales()
            Result.success(embeddedLocales)
        }
    }

    override suspend fun isLocaleSupported(locale: String): Boolean {
        return getSupportedLocales().getOrNull()?.contains(locale) ?: false
    }

    private suspend fun getCachedStringsIfValid(locale: String): Map<String, String>? {
        if (!cache.isCacheValid(locale)) {
            return null
        }

        val cacheAge = cache.getCacheAge(locale) ?: return null
        val currentTime = System.currentTimeMillis()
        val maxAge = CACHE_VALIDITY_HOURS * 60 * 60 * 1000L

        return if (currentTime - cacheAge < maxAge) {
            cache.getCachedStrings(locale)
        } else {
            null
        }
    }
}