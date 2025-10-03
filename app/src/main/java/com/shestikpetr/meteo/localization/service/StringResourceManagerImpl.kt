package com.shestikpetr.meteo.localization.service

import com.shestikpetr.meteo.localization.interfaces.StringResourceManager
import com.shestikpetr.meteo.localization.interfaces.LocalizationService
import com.shestikpetr.meteo.localization.interfaces.StringKey
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.runBlocking

/**
 * Main implementation of StringResourceManager
 * Provides unified API for accessing localized strings
 * Delegates to LocalizationService for actual string management
 */
@Singleton
class StringResourceManagerImpl @Inject constructor(
    private val localizationService: LocalizationService
) : StringResourceManager {

    override suspend fun getString(key: StringKey): String {
        return localizationService.getString(key)
    }

    override suspend fun getString(key: StringKey, vararg args: Any): String {
        return localizationService.getString(key, *args)
    }

    override suspend fun getCurrentLocale(): String {
        return localizationService.currentLocale.value
    }

    override suspend fun setLocale(locale: String) {
        localizationService.changeLocale(locale)
    }

    override suspend fun isLocaleSupported(locale: String): Boolean {
        return localizationService.getSupportedLocales().contains(locale)
    }

    override fun getStringSync(key: StringKey): String {
        return localizationService.getString(key)
    }

    override fun getStringSync(key: StringKey, vararg args: Any): String {
        return localizationService.getString(key, *args)
    }
}