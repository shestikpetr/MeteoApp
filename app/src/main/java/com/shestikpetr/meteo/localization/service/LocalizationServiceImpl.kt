package com.shestikpetr.meteo.localization.service

import com.shestikpetr.meteo.localization.interfaces.LocalizationService
import com.shestikpetr.meteo.localization.interfaces.LocalizationRepository
import com.shestikpetr.meteo.localization.interfaces.LocalePreferences
import com.shestikpetr.meteo.localization.interfaces.StringFormatter
import com.shestikpetr.meteo.localization.interfaces.StringKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service implementation for localization management
 * Coordinates between repository, preferences, and UI state
 * Thread-safe and reactive using StateFlow
 */
@Singleton
class LocalizationServiceImpl @Inject constructor(
    private val repository: LocalizationRepository,
    private val preferences: LocalePreferences,
    private val formatter: StringFormatter
) : LocalizationService {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val stringsMutex = Mutex()

    private val _currentLocale = MutableStateFlow("ru")
    override val currentLocale: StateFlow<String> = _currentLocale.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    override val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    override val error: StateFlow<String?> = _error.asStateFlow()

    private var currentStrings: Map<String, String> = emptyMap()
    private var supportedLocales: List<String> = listOf("ru", "en")

    override suspend fun initialize() {
        _isLoading.value = true
        _error.value = null

        try {
            // Load preferred locale or use system default
            val preferredLocale = preferences.getPreferredLocale()
                ?: preferences.getSystemLocale()

            // Load supported locales
            val localesResult = repository.getSupportedLocales()
            if (localesResult.isSuccess) {
                supportedLocales = localesResult.getOrNull() ?: supportedLocales
            }

            // Validate and set locale
            val validLocale = if (supportedLocales.contains(preferredLocale)) {
                preferredLocale
            } else {
                supportedLocales.firstOrNull() ?: "ru"
            }

            changeLocale(validLocale)
        } catch (e: Exception) {
            _error.value = "Failed to initialize localization: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }

    override suspend fun changeLocale(locale: String): Result<Unit> {
        if (!supportedLocales.contains(locale)) {
            return Result.failure(IllegalArgumentException("Locale $locale is not supported"))
        }

        _isLoading.value = true
        _error.value = null

        return try {
            stringsMutex.withLock {
                val stringsResult = repository.loadStrings(locale)
                if (stringsResult.isSuccess) {
                    currentStrings = stringsResult.getOrNull() ?: emptyMap()
                    _currentLocale.value = locale
                    preferences.setPreferredLocale(locale)
                    Result.success(Unit)
                } else {
                    val error = stringsResult.exceptionOrNull()
                    _error.value = "Failed to load strings for locale $locale: ${error?.message}"
                    Result.failure(error ?: Exception("Unknown error"))
                }
            }
        } catch (e: Exception) {
            _error.value = "Error changing locale: ${e.message}"
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }

    override suspend fun refreshStrings(): Result<Unit> {
        return changeLocale(_currentLocale.value)
    }

    override suspend fun getSupportedLocales(): List<String> {
        return supportedLocales
    }

    override fun getString(key: StringKey): String {
        return currentStrings[key.key] ?: key.key
    }

    override fun getString(key: StringKey, vararg args: Any): String {
        val template = currentStrings[key.key] ?: key.key
        return try {
            formatter.format(template, *args)
        } catch (e: Exception) {
            // Fallback to template if formatting fails
            template
        }
    }

    /**
     * Preload strings for better performance
     */
    fun preloadStrings(locale: String) {
        serviceScope.launch {
            try {
                repository.loadStrings(locale)
            } catch (e: Exception) {
                // Silent preloading - don't update error state
            }
        }
    }
}