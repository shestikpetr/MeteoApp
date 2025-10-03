package com.shestikpetr.meteo.localization.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.shestikpetr.meteo.localization.interfaces.LocalePreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

private val Context.localeDataStore: DataStore<Preferences> by preferencesDataStore(name = "locale_preferences")

/**
 * DataStore-based implementation of LocalePreferences
 * Manages user language preferences persistently
 */
@Singleton
class LocalePreferencesImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : LocalePreferences {

    private companion object {
        val PREFERRED_LOCALE_KEY = stringPreferencesKey("preferred_locale")
    }

    override suspend fun getPreferredLocale(): String? {
        return try {
            context.localeDataStore.data
                .map { preferences -> preferences[PREFERRED_LOCALE_KEY] }
                .first()
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun setPreferredLocale(locale: String) {
        try {
            context.localeDataStore.edit { preferences ->
                preferences[PREFERRED_LOCALE_KEY] = locale
            }
        } catch (e: Exception) {
            // Log error but don't fail
        }
    }

    override suspend fun getSystemLocale(): String {
        return try {
            val systemLocale = Locale.getDefault().language
            // Map system locales to supported locales
            when (systemLocale) {
                "ru" -> "ru"
                "en" -> "en"
                else -> "ru" // Default fallback
            }
        } catch (e: Exception) {
            "ru" // Safe fallback
        }
    }
}